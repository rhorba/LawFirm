# Financial Management Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: use executing-plans skill to implement this plan task-by-task.

**Goal:** Implement the full financial management feature — transaction ledger with filters/export, invoice lifecycle, and a financial tab embedded in case detail.

**Architecture:** Four Flyway migrations evolve the DB schema; new Spring Boot services/controllers expose a REST API; Angular 18 standalone components split the UI between a global `/financial/ledger` page and a case-scoped financial tab. The existing `FinancialSummary` DTO field `totalPayments` is renamed to `totalRevenue` to match the new direction model.

**Tech Stack:** Spring Boot 3.4 (Java 21), Angular 18, Flyway, MapStruct, Tailwind.

---

## Batch 1 — Database Migrations ✅ COMPLETED

### Task 1: V46 — Evolve financial_transactions

**Files:**
- Create: `backend/src/main/resources/db/migration/V46__evolve_financial_transactions.sql`

**Step 1: Write the migration (LF line endings — enforced by .gitattributes)**

```sql
-- V46__evolve_financial_transactions.sql
ALTER TABLE financial_transactions
  ADD COLUMN direction VARCHAR(10) NOT NULL DEFAULT 'EXPENSE'
    CHECK (direction IN ('REVENUE', 'EXPENSE')),
  ADD COLUMN operation_type VARCHAR(20) NOT NULL DEFAULT 'OTHER'
    CHECK (operation_type IN (
      'OPENING_FEE', 'PROCEDURE_FEE', 'INTERVENTION_FEE', 'EXPERT_FEE',
      'DOCUMENT_FEE', 'NOTIFICATION_FEE', 'JUDICIAL_TAX', 'OTHER')),
  ADD COLUMN payment_mode VARCHAR(15)
    CHECK (payment_mode IN ('CHECK', 'TRANSFER', 'CASH', 'CREDIT_CARD', 'MONEY_ORDER')),
  ADD COLUMN account_number VARCHAR(50),
  ADD COLUMN deleted_at TIMESTAMP;

-- Migrate existing data
UPDATE financial_transactions
  SET direction =
    CASE WHEN transaction_type = 'PAYMENT' THEN 'REVENUE' ELSE 'EXPENSE' END;

-- Drop old column (also removes its CHECK constraint)
ALTER TABLE financial_transactions DROP COLUMN transaction_type;

CREATE INDEX idx_transactions_direction  ON financial_transactions(direction);
CREATE INDEX idx_transactions_deleted_at ON financial_transactions(deleted_at);
```

**Step 2: Verify migration applies**
```bash
cd backend && mvn flyway:migrate
```
Expected: `Successfully applied 1 migration to schema "public" (V46)`.

---

### Task 2: V47 — Create invoices table

**Files:**
- Create: `backend/src/main/resources/db/migration/V47__create_invoices_table.sql`

**Step 1: Write the migration**

```sql
-- V47__create_invoices_table.sql
CREATE SEQUENCE invoice_number_seq START 1;

CREATE TABLE invoices (
  id             BIGSERIAL PRIMARY KEY,
  case_id        BIGINT NOT NULL REFERENCES cases(id),
  invoice_number VARCHAR(50) NOT NULL UNIQUE,
  issue_date     DATE NOT NULL,
  due_date       DATE,
  status         VARCHAR(15) NOT NULL DEFAULT 'DRAFT'
                   CHECK (status IN ('DRAFT', 'SENT', 'PAID', 'CANCELLED')),
  subtotal       DECIMAL(15,2) NOT NULL DEFAULT 0,
  tax_amount     DECIMAL(15,2) NOT NULL DEFAULT 0,
  total_amount   DECIMAL(15,2) NOT NULL DEFAULT 0,
  notes          TEXT,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version        BIGINT NOT NULL DEFAULT 0,
  deleted_at     TIMESTAMP
);

CREATE INDEX idx_invoices_case   ON invoices(case_id);
CREATE INDEX idx_invoices_status ON invoices(status);
```

**Step 2: Verify**
```bash
cd backend && mvn flyway:migrate
```

---

### Task 3: V48 — Create invoice_items table

**Files:**
- Create: `backend/src/main/resources/db/migration/V48__create_invoice_items_table.sql`

**Step 1: Write the migration**

```sql
-- V48__create_invoice_items_table.sql
CREATE TABLE invoice_items (
  id             BIGSERIAL PRIMARY KEY,
  invoice_id     BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
  description    VARCHAR(255) NOT NULL,
  operation_type VARCHAR(20) NOT NULL,
  quantity       INT NOT NULL DEFAULT 1,
  unit_price     DECIMAL(15,2) NOT NULL,
  line_total     DECIMAL(15,2) NOT NULL,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version        BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_invoice_items_invoice ON invoice_items(invoice_id);
```

**Step 2: Verify**
```bash
cd backend && mvn flyway:migrate
```

---

### Task 4: V49 — Seed financial permissions

**Files:**
- Create: `backend/src/main/resources/db/migration/V49__seed_financial_permissions.sql`

**Step 1: Write the migration**

```sql
-- V49__seed_financial_permissions.sql
INSERT INTO permissions (name, description) VALUES
  ('FINANCIAL_READ',   'View financial transactions and summaries'),
  ('FINANCIAL_CREATE', 'Create financial transactions'),
  ('FINANCIAL_UPDATE', 'Soft-delete financial transactions'),
  ('INVOICE_READ',     'View invoices'),
  ('INVOICE_CREATE',   'Create invoices'),
  ('INVOICE_MANAGE',   'Update invoice status and delete invoices');

-- ADMIN: all 6
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN (
    'FINANCIAL_READ','FINANCIAL_CREATE','FINANCIAL_UPDATE',
    'INVOICE_READ','INVOICE_CREATE','INVOICE_MANAGE'
  );

-- MODERATOR: read-only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MODERATOR'
  AND p.name IN ('FINANCIAL_READ', 'INVOICE_READ');
```

**Step 2: Verify**
```bash
cd backend && mvn flyway:migrate
```

---

## Batch 2 — Backend Domain Layer ✅ COMPLETED

### Task 5: Update FinancialTransaction entity

**Files:**
- Modify: `backend/src/main/java/com/lawfirm/domain/model/FinancialTransaction.java`

Replace the single `TransactionType` enum with three separate enums and add new fields. **Replace the entire file.**

```java
package com.lawfirm.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FinancialTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private OperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 15)
    private PaymentMode paymentMode;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "lawyer_payment_year")
    private Integer lawyerPaymentYear;

    @Column(name = "fiscal_year_from")
    private LocalDate fiscalYearFrom;

    @Column(name = "fiscal_year_to")
    private LocalDate fiscalYearTo;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public enum Direction {
        REVENUE, EXPENSE
    }

    public enum OperationType {
        OPENING_FEE, PROCEDURE_FEE, INTERVENTION_FEE, EXPERT_FEE,
        DOCUMENT_FEE, NOTIFICATION_FEE, JUDICIAL_TAX, OTHER
    }

    public enum PaymentMode {
        CHECK, TRANSFER, CASH, CREDIT_CARD, MONEY_ORDER
    }
}
```

**Step 2: Compile**
```bash
cd backend && mvn clean compile
```
Expected: **compilation errors** in `CaseMapper` and `FinancialTransactionRepository` — those are fixed in Tasks 8, 13, 14.

---

### Task 6: Create Invoice entity

**Files:**
- Create: `backend/src/main/java/com/lawfirm/domain/model/Invoice.java`

```java
package com.lawfirm.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Invoice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    public enum InvoiceStatus {
        DRAFT, SENT, PAID, CANCELLED
    }
}
```

---

### Task 7: Create InvoiceItem entity

**Files:**
- Create: `backend/src/main/java/com/lawfirm/domain/model/InvoiceItem.java`

```java
package com.lawfirm.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class InvoiceItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private FinancialTransaction.OperationType operationType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal;
}
```

---

### Task 8: Update FinancialTransactionRepository

**Files:**
- Modify: `backend/src/main/java/com/lawfirm/domain/repository/FinancialTransactionRepository.java`

Add `JpaSpecificationExecutor`, replace old JPQL queries with soft-delete-aware ones. **Replace the entire file.**

```java
package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.FinancialTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransaction, Long>,
                JpaSpecificationExecutor<FinancialTransaction> {

    List<FinancialTransaction> findByCaseEntityIdAndDeletedAtIsNull(Long caseId);

    Page<FinancialTransaction> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<FinancialTransaction> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t " +
           "WHERE t.caseEntity.id = :caseId " +
           "AND t.direction = com.lawfirm.domain.model.FinancialTransaction.Direction.REVENUE " +
           "AND t.deletedAt IS NULL")
    BigDecimal sumRevenueByCaseId(@Param("caseId") Long caseId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t " +
           "WHERE t.caseEntity.id = :caseId " +
           "AND t.direction = com.lawfirm.domain.model.FinancialTransaction.Direction.EXPENSE " +
           "AND t.deletedAt IS NULL")
    BigDecimal sumExpensesByCaseId(@Param("caseId") Long caseId);

    @Query("SELECT COUNT(t) FROM FinancialTransaction t " +
           "WHERE t.caseEntity.id = :caseId AND t.deletedAt IS NULL")
    int countByCaseId(@Param("caseId") Long caseId);
}
```

**Step 2: Compile**
```bash
cd backend && mvn clean compile
```

---

### Task 9: Create InvoiceRepository

**Files:**
- Create: `backend/src/main/java/com/lawfirm/domain/repository/InvoiceRepository.java`

```java
package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Invoice> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByInvoiceNumber(String invoiceNumber);
}
```

---

## Batch 3 — Domain Layer (Entities & Repositories) ✅ COMPLETED

### Task 7 ✅ — InvoiceItem entity created
### Task 8 ✅ — FinancialTransactionRepository updated (JpaSpecificationExecutor + soft-delete queries)
### Task 9 ✅ — InvoiceRepository created

---

## Batch 4 — Application Layer: DTOs & Mappers ✅ COMPLETED

### Task 10: Update FinancialSummary DTO

**Files:**
- Modify: `backend/src/main/java/com/lawfirm/application/dto/response/FinancialSummary.java`

Rename `totalPayments` → `totalRevenue` to match the new direction model. **Replace the entire file.**

```java
package com.lawfirm.application.dto.response;

import java.math.BigDecimal;

public record FinancialSummary(
    BigDecimal totalRevenue,
    BigDecimal totalExpenses,
    BigDecimal balance,
    Integer transactionCount
) {}
```

---

### Task 11: Create Request DTOs

**Files:**
- Create: `backend/src/main/java/com/lawfirm/application/dto/request/FinancialTransactionRequest.java`
- Create: `backend/src/main/java/com/lawfirm/application/dto/request/FinancialFilterRequest.java`
- Create: `backend/src/main/java/com/lawfirm/application/dto/request/InvoiceRequest.java`
- Create: `backend/src/main/java/com/lawfirm/application/dto/request/InvoiceItemRequest.java`
- Create: `backend/src/main/java/com/lawfirm/application/dto/request/InvoiceStatusRequest.java`

**Step 1: FinancialTransactionRequest.java**

```java
package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.FinancialTransaction.Direction;
import com.lawfirm.domain.model.FinancialTransaction.OperationType;
import com.lawfirm.domain.model.FinancialTransaction.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialTransactionRequest(
    @NotNull Long caseId,
    @NotNull Direction direction,
    @NotNull OperationType operationType,
    PaymentMode paymentMode,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    LocalDate paymentDate,
    String paymentReference,
    String accountNumber,
    String description
) {}
```

**Step 2: FinancialFilterRequest.java**

```java
package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.FinancialTransaction.Direction;
import com.lawfirm.domain.model.FinancialTransaction.OperationType;

import java.time.LocalDate;

public record FinancialFilterRequest(
    Long caseId,
    Long clientId,
    Direction direction,
    OperationType operationType,
    LocalDate dateFrom,
    LocalDate dateTo
) {}
```

**Step 3: InvoiceItemRequest.java**

```java
package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.FinancialTransaction.OperationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InvoiceItemRequest(
    @NotBlank @Size(max = 255) String description,
    @NotNull OperationType operationType,
    @Min(1) int quantity,
    @NotNull @DecimalMin("0.00") BigDecimal unitPrice
) {}
```

**Step 4: InvoiceRequest.java**

```java
package com.lawfirm.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceRequest(
    @NotNull Long caseId,
    @NotNull LocalDate issueDate,
    LocalDate dueDate,
    @DecimalMin("0.00") BigDecimal taxAmount,
    String notes,
    @NotEmpty @Valid List<InvoiceItemRequest> items
) {}
```

**Step 5: InvoiceStatusRequest.java**

```java
package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.Invoice.InvoiceStatus;
import jakarta.validation.constraints.NotNull;

public record InvoiceStatusRequest(
    @NotNull InvoiceStatus status
) {}
```

**Step 6: Compile**
```bash
cd backend && mvn clean compile
```

---

### Task 12: Create Response DTOs

**Files:**
- Create: `backend/src/main/java/com/lawfirm/application/dto/response/FinancialTransactionResponse.java`
- Create: `backend/src/main/java/com/lawfirm/application/dto/response/InvoiceItemResponse.java`
- Create: `backend/src/main/java/com/lawfirm/application/dto/response/InvoiceResponse.java`

**Step 1: FinancialTransactionResponse.java**

```java
package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.FinancialTransaction.Direction;
import com.lawfirm.domain.model.FinancialTransaction.OperationType;
import com.lawfirm.domain.model.FinancialTransaction.PaymentMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FinancialTransactionResponse(
    Long id,
    Long caseId,
    String caseNumber,
    Direction direction,
    OperationType operationType,
    PaymentMode paymentMode,
    BigDecimal amount,
    LocalDate paymentDate,
    String paymentReference,
    String accountNumber,
    String description,
    LocalDateTime createdAt,
    String createdBy
) {}
```

**Step 2: InvoiceItemResponse.java**

```java
package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.FinancialTransaction.OperationType;

import java.math.BigDecimal;

public record InvoiceItemResponse(
    Long id,
    String description,
    OperationType operationType,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal
) {}
```

**Step 3: InvoiceResponse.java**

```java
package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.Invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
    Long id,
    Long caseId,
    String caseNumber,
    String invoiceNumber,
    LocalDate issueDate,
    LocalDate dueDate,
    InvoiceStatus status,
    BigDecimal subtotal,
    BigDecimal taxAmount,
    BigDecimal totalAmount,
    String notes,
    List<InvoiceItemResponse> items,
    LocalDateTime createdAt
) {}
```

**Step 4: Compile**
```bash
cd backend && mvn clean compile
```

---

### Task 13: Create MapStruct Mappers

**Files:**
- Create: `backend/src/main/java/com/lawfirm/application/mapper/FinancialTransactionMapper.java`
- Create: `backend/src/main/java/com/lawfirm/application/mapper/InvoiceMapper.java`

**Step 1: FinancialTransactionMapper.java**

```java
package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.FinancialTransactionResponse;
import com.lawfirm.domain.model.FinancialTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FinancialTransactionMapper {

    @Mapping(target = "caseId",     source = "caseEntity.id")
    @Mapping(target = "caseNumber", source = "caseEntity.fullCaseNumber")
    @Mapping(target = "createdBy",  expression = "java(t.getCreatedBy())")
    FinancialTransactionResponse toResponse(FinancialTransaction t);
}
```

> **Note on `createdBy`:** `BaseEntity` has a `@CreatedBy` JPA Auditing field populated by Spring Security. Verify the field name in your `BaseEntity`; if it's named differently, adjust the expression.

**Step 2: InvoiceMapper.java**

```java
package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.InvoiceItemResponse;
import com.lawfirm.application.dto.response.InvoiceResponse;
import com.lawfirm.domain.model.Invoice;
import com.lawfirm.domain.model.InvoiceItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "caseId",     source = "caseEntity.id")
    @Mapping(target = "caseNumber", source = "caseEntity.fullCaseNumber")
    InvoiceResponse toResponse(Invoice invoice);

    InvoiceItemResponse toItemResponse(InvoiceItem item);
}
```

**Step 3: Compile**
```bash
cd backend && mvn clean compile
```

---

### Task 14: Update CaseMapper — fix calculateFinancialSummary

**Files:**
- Modify: `backend/src/main/java/com/lawfirm/application/mapper/CaseMapper.java`

The `calculateFinancialSummary` method uses the old `TransactionType.PAYMENT` enum value. Update it to use `Direction.REVENUE` and also update the `FinancialSummary` constructor call to use `totalRevenue` (positional record — just rename the local variable for clarity).

**Step 1: Update the `calculateFinancialSummary` default method**

Replace these lines in `CaseMapper.java`:
```java
// OLD — delete these lines:
BigDecimal totalPayments = caseEntity.getTransactions().stream()
    .filter(t -> t.getTransactionType() == FinancialTransaction.TransactionType.PAYMENT)
    .map(FinancialTransaction::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal totalExpenses = caseEntity.getTransactions().stream()
    .filter(t -> t.getTransactionType() == FinancialTransaction.TransactionType.EXPENSE)
    .map(FinancialTransaction::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal balance = totalPayments.subtract(totalExpenses);
int count = caseEntity.getTransactions().size();

return new FinancialSummary(totalPayments, totalExpenses, balance, count);
```

With:
```java
// NEW
BigDecimal totalRevenue = caseEntity.getTransactions().stream()
    .filter(t -> t.getDeletedAt() == null
              && t.getDirection() == FinancialTransaction.Direction.REVENUE)
    .map(FinancialTransaction::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal totalExpenses = caseEntity.getTransactions().stream()
    .filter(t -> t.getDeletedAt() == null
              && t.getDirection() == FinancialTransaction.Direction.EXPENSE)
    .map(FinancialTransaction::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal balance = totalRevenue.subtract(totalExpenses);
int count = (int) caseEntity.getTransactions().stream()
    .filter(t -> t.getDeletedAt() == null).count();

return new FinancialSummary(totalRevenue, totalExpenses, balance, count);
```

Also update the guard at the top of `calculateFinancialSummary`:
```java
// OLD:
return new FinancialSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
// Already correct — record field order matches new definition.
```

**Step 2: Compile — must be zero errors**
```bash
cd backend && mvn clean compile
```

---

## Batch 5 — Backend Service & Controller Layer ✅ COMPLETED

### Task 15: Add iText7 dependency for PDF export

**Files:**
- Modify: `backend/pom.xml`

**Step 1: Add inside `<dependencies>`**

```xml
<!-- iText7 PDF generation -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>7.2.5</version>
    <type>pom</type>
</dependency>
```

> **Note:** Apache POI should already be present (used by `ClientExportService`). Verify with `grep -r "poi" backend/pom.xml`. If missing, also add:
> ```xml
> <dependency>
>     <groupId>org.apache.poi</groupId>
>     <artifactId>poi-ooxml</artifactId>
>     <version>5.2.5</version>
> </dependency>
> ```

**Step 2: Resolve dependencies**
```bash
cd backend && mvn dependency:resolve
```

---

### Task 16: Create FinancialTransactionService

**Files:**
- Create: `backend/src/main/java/com/lawfirm/application/service/FinancialTransactionService.java`

```java
package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.FinancialFilterRequest;
import com.lawfirm.application.dto.request.FinancialTransactionRequest;
import com.lawfirm.application.dto.response.FinancialSummary;
import com.lawfirm.application.dto.response.FinancialTransactionResponse;
import com.lawfirm.application.mapper.FinancialTransactionMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.FinancialTransaction;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.FinancialTransactionRepository;
import com.lawfirm.infrastructure.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialTransactionService {

    private final FinancialTransactionRepository transactionRepository;
    private final CaseRepository caseRepository;
    private final FinancialTransactionMapper mapper;

    // ── Read ─────────────────────────────────────────────────────────────────

    public Page<FinancialTransactionResponse> search(FinancialFilterRequest filter, Pageable pageable) {
        Specification<FinancialTransaction> spec = buildSpec(filter);
        return transactionRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    public List<FinancialTransactionResponse> findByCaseId(Long caseId) {
        return transactionRepository.findByCaseEntityIdAndDeletedAtIsNull(caseId)
            .stream().map(mapper::toResponse).toList();
    }

    public FinancialSummary getSummaryByCaseId(Long caseId) {
        BigDecimal revenue  = transactionRepository.sumRevenueByCaseId(caseId);
        BigDecimal expenses = transactionRepository.sumExpensesByCaseId(caseId);
        int count           = transactionRepository.countByCaseId(caseId);
        return new FinancialSummary(revenue, expenses, revenue.subtract(expenses), count);
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Transactional
    public FinancialTransactionResponse create(FinancialTransactionRequest request) {
        Case caseEntity = caseRepository.findById(request.caseId())
            .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + request.caseId()));

        FinancialTransaction tx = FinancialTransaction.builder()
            .caseEntity(caseEntity)
            .direction(request.direction())
            .operationType(request.operationType())
            .paymentMode(request.paymentMode())
            .amount(request.amount())
            .paymentDate(request.paymentDate())
            .paymentReference(request.paymentReference())
            .accountNumber(request.accountNumber())
            .description(request.description())
            .build();

        return mapper.toResponse(transactionRepository.save(tx));
    }

    @Transactional
    public void softDelete(Long id) {
        FinancialTransaction tx = transactionRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        tx.setDeletedAt(LocalDateTime.now());
        transactionRepository.save(tx);
    }

    // ── Export ────────────────────────────────────────────────────────────────

    public byte[] exportExcel(FinancialFilterRequest filter) {
        List<FinancialTransaction> transactions = transactionRepository
            .findAll(buildSpec(filter));

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Transactions");
            Row header = sheet.createRow(0);
            String[] cols = {"ID", "Case", "Direction", "Type", "Mode", "Amount", "Date", "Reference", "Description"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

            int rowNum = 1;
            for (FinancialTransaction t : transactions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(t.getId());
                row.createCell(1).setCellValue(t.getCaseEntity().getFullCaseNumber());
                row.createCell(2).setCellValue(t.getDirection().name());
                row.createCell(3).setCellValue(t.getOperationType().name());
                row.createCell(4).setCellValue(t.getPaymentMode() != null ? t.getPaymentMode().name() : "");
                row.createCell(5).setCellValue(t.getAmount().doubleValue());
                row.createCell(6).setCellValue(t.getPaymentDate() != null ? t.getPaymentDate().toString() : "");
                row.createCell(7).setCellValue(t.getPaymentReference() != null ? t.getPaymentReference() : "");
                row.createCell(8).setCellValue(t.getDescription() != null ? t.getDescription() : "");
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Excel export failed", e);
        }
    }

    // ── Specification builder ─────────────────────────────────────────────────

    private Specification<FinancialTransaction> buildSpec(FinancialFilterRequest f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (f.caseId() != null)      predicates.add(cb.equal(root.get("caseEntity").get("id"), f.caseId()));
            if (f.direction() != null)   predicates.add(cb.equal(root.get("direction"), f.direction()));
            if (f.operationType() != null) predicates.add(cb.equal(root.get("operationType"), f.operationType()));
            if (f.dateFrom() != null)    predicates.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), f.dateFrom()));
            if (f.dateTo() != null)      predicates.add(cb.lessThanOrEqualTo(root.get("paymentDate"), f.dateTo()));
            if (f.clientId() != null) {
                predicates.add(cb.equal(root.get("caseEntity").get("client").get("id"), f.clientId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

---

### Task 17: Create InvoiceService

**Files:**
- Create: `backend/src/main/java/com/lawfirm/application/service/InvoiceService.java`

```java
package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.InvoiceItemRequest;
import com.lawfirm.application.dto.request.InvoiceRequest;
import com.lawfirm.application.dto.response.InvoiceResponse;
import com.lawfirm.application.mapper.InvoiceMapper;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.FinancialTransaction.OperationType;
import com.lawfirm.domain.model.Invoice;
import com.lawfirm.domain.model.Invoice.InvoiceStatus;
import com.lawfirm.domain.model.InvoiceItem;
import com.lawfirm.domain.repository.CaseRepository;
import com.lawfirm.domain.repository.InvoiceRepository;
import com.lawfirm.infrastructure.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CaseRepository caseRepository;
    private final InvoiceMapper mapper;
    private final EntityManager entityManager;

    // Valid status transitions
    private static final Map<InvoiceStatus, Set<InvoiceStatus>> VALID_TRANSITIONS = Map.of(
        InvoiceStatus.DRAFT,     Set.of(InvoiceStatus.SENT, InvoiceStatus.CANCELLED),
        InvoiceStatus.SENT,      Set.of(InvoiceStatus.PAID, InvoiceStatus.CANCELLED),
        InvoiceStatus.PAID,      Set.of(),
        InvoiceStatus.CANCELLED, Set.of()
    );

    // ── Read ─────────────────────────────────────────────────────────────────

    public Page<InvoiceResponse> findAll(Pageable pageable) {
        return invoiceRepository.findAllByDeletedAtIsNull(pageable).map(mapper::toResponse);
    }

    public InvoiceResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        Case caseEntity = caseRepository.findById(request.caseId())
            .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + request.caseId()));

        String invoiceNumber = generateInvoiceNumber(request.issueDate().getYear());

        // Calculate totals from items
        List<InvoiceItem> items = request.items().stream()
            .map(this::buildItem)
            .toList();

        BigDecimal subtotal = items.stream()
            .map(InvoiceItem::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal taxAmount    = request.taxAmount() != null ? request.taxAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount  = subtotal.add(taxAmount);

        Invoice invoice = Invoice.builder()
            .caseEntity(caseEntity)
            .invoiceNumber(invoiceNumber)
            .issueDate(request.issueDate())
            .dueDate(request.dueDate())
            .notes(request.notes())
            .subtotal(subtotal)
            .taxAmount(taxAmount)
            .totalAmount(totalAmount)
            .build();

        // Link items to invoice
        items.forEach(item -> item.setInvoice(invoice));
        invoice.getItems().addAll(items);

        return mapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse updateStatus(Long id, InvoiceStatus newStatus) {
        Invoice invoice = getOrThrow(id);
        Set<InvoiceStatus> allowed = VALID_TRANSITIONS.get(invoice.getStatus());
        if (!allowed.contains(newStatus)) {
            throw new IllegalArgumentException(
                "Cannot transition from " + invoice.getStatus() + " to " + newStatus);
        }
        invoice.setStatus(newStatus);
        return mapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public void softDelete(Long id) {
        Invoice invoice = getOrThrow(id);
        invoice.setDeletedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Invoice getOrThrow(Long id) {
        return invoiceRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }

    private InvoiceItem buildItem(InvoiceItemRequest req) {
        BigDecimal lineTotal = req.unitPrice().multiply(BigDecimal.valueOf(req.quantity()));
        return InvoiceItem.builder()
            .description(req.description())
            .operationType(req.operationType())
            .quantity(req.quantity())
            .unitPrice(req.unitPrice())
            .lineTotal(lineTotal)
            .build();
    }

    private synchronized String generateInvoiceNumber(int year) {
        // Uses the DB sequence created in V47
        Long seq = ((Number) entityManager
            .createNativeQuery("SELECT nextval('invoice_number_seq')")
            .getSingleResult()).longValue();
        return String.format("FAC-%d-%04d", year, seq);
    }
}
```

---

### Task 18: Create FinancialTransactionController

**Files:**
- Create: `backend/src/main/java/com/lawfirm/presentation/financial/FinancialTransactionController.java`

```java
package com.lawfirm.presentation.financial;

import com.lawfirm.application.dto.request.FinancialFilterRequest;
import com.lawfirm.application.dto.request.FinancialTransactionRequest;
import com.lawfirm.application.dto.response.FinancialTransactionResponse;
import com.lawfirm.application.service.FinancialTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/financial")
@RequiredArgsConstructor
@Tag(name = "Financial Transactions", description = "Financial ledger management")
public class FinancialTransactionController {

    private final FinancialTransactionService service;

    @GetMapping("/transactions")
    @PreAuthorize("hasPermission(null, 'FINANCIAL_READ')")
    @Operation(summary = "Paginated transaction list with optional filters")
    public ResponseEntity<Page<FinancialTransactionResponse>> list(
        @RequestParam(required = false) Long caseId,
        @RequestParam(required = false) Long clientId,
        @RequestParam(required = false) String direction,
        @RequestParam(required = false) String operationType,
        @RequestParam(required = false) String dateFrom,
        @RequestParam(required = false) String dateTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sort
    ) {
        FinancialFilterRequest filter = buildFilter(caseId, clientId, direction, operationType, dateFrom, dateTo);
        return ResponseEntity.ok(service.search(filter, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sort))));
    }

    @GetMapping("/cases/{caseId}/transactions")
    @PreAuthorize("hasPermission(null, 'FINANCIAL_READ')")
    @Operation(summary = "All non-deleted transactions for a case")
    public ResponseEntity<List<FinancialTransactionResponse>> listByCase(@PathVariable Long caseId) {
        return ResponseEntity.ok(service.findByCaseId(caseId));
    }

    @PostMapping("/transactions")
    @PreAuthorize("hasPermission(null, 'FINANCIAL_CREATE')")
    @Operation(summary = "Create a new financial transaction")
    public ResponseEntity<FinancialTransactionResponse> create(
        @Valid @RequestBody FinancialTransactionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @DeleteMapping("/transactions/{id}")
    @PreAuthorize("hasPermission(null, 'FINANCIAL_UPDATE')")
    @Operation(summary = "Soft-delete a transaction")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transactions/export/excel")
    @PreAuthorize("hasPermission(null, 'FINANCIAL_READ')")
    @Operation(summary = "Export transactions as Excel")
    public void exportExcel(
        @RequestParam(required = false) Long caseId,
        @RequestParam(required = false) Long clientId,
        @RequestParam(required = false) String direction,
        @RequestParam(required = false) String operationType,
        @RequestParam(required = false) String dateFrom,
        @RequestParam(required = false) String dateTo,
        HttpServletResponse response
    ) throws IOException {
        FinancialFilterRequest filter = buildFilter(caseId, clientId, direction, operationType, dateFrom, dateTo);
        byte[] xlsx = service.exportExcel(filter);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=transactions.xlsx");
        response.getOutputStream().write(xlsx);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FinancialFilterRequest buildFilter(Long caseId, Long clientId, String direction,
                                               String operationType, String dateFrom, String dateTo) {
        return new FinancialFilterRequest(
            caseId,
            clientId,
            direction != null ? com.lawfirm.domain.model.FinancialTransaction.Direction.valueOf(direction) : null,
            operationType != null ? com.lawfirm.domain.model.FinancialTransaction.OperationType.valueOf(operationType) : null,
            dateFrom != null ? LocalDate.parse(dateFrom) : null,
            dateTo != null ? LocalDate.parse(dateTo) : null
        );
    }
}
```

---

### Task 19: Create InvoiceController

**Files:**
- Create: `backend/src/main/java/com/lawfirm/presentation/financial/InvoiceController.java`

```java
package com.lawfirm.presentation.financial;

import com.lawfirm.application.dto.request.InvoiceRequest;
import com.lawfirm.application.dto.request.InvoiceStatusRequest;
import com.lawfirm.application.dto.response.InvoiceResponse;
import com.lawfirm.application.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financial/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice lifecycle management")
public class InvoiceController {

    private final InvoiceService service;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'INVOICE_READ')")
    @Operation(summary = "Paginated invoice list")
    public ResponseEntity<Page<InvoiceResponse>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'INVOICE_CREATE')")
    @Operation(summary = "Create an invoice with line items")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'INVOICE_READ')")
    @Operation(summary = "Get invoice detail")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasPermission(null, 'INVOICE_MANAGE')")
    @Operation(summary = "Transition invoice status")
    public ResponseEntity<InvoiceResponse> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody InvoiceStatusRequest request
    ) {
        return ResponseEntity.ok(service.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'INVOICE_MANAGE')")
    @Operation(summary = "Soft-delete an invoice")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Step 2: Full backend compile + test**
```bash
cd backend && mvn clean verify
```
Expected: BUILD SUCCESS, all migrations applied, no test failures.

---

## Batch 6 — Frontend: Models, Service & Routing ✅ COMPLETED

### Task 20: Create financial.model.ts + Update case.model.ts

**Files:**
- Create: `frontend/src/app/core/models/financial.model.ts`
- Modify: `frontend/src/app/core/models/case.model.ts`

**Step 1: Create financial.model.ts**

```typescript
// frontend/src/app/core/models/financial.model.ts

export type TransactionDirection = 'REVENUE' | 'EXPENSE';

export type OperationType =
  | 'OPENING_FEE'
  | 'PROCEDURE_FEE'
  | 'INTERVENTION_FEE'
  | 'EXPERT_FEE'
  | 'DOCUMENT_FEE'
  | 'NOTIFICATION_FEE'
  | 'JUDICIAL_TAX'
  | 'OTHER';

export type PaymentMode = 'CHECK' | 'TRANSFER' | 'CASH' | 'CREDIT_CARD' | 'MONEY_ORDER';

export type InvoiceStatus = 'DRAFT' | 'SENT' | 'PAID' | 'CANCELLED';

export interface TransactionResponse {
  id: number;
  caseId: number;
  caseNumber: string;
  direction: TransactionDirection;
  operationType: OperationType;
  paymentMode?: PaymentMode;
  amount: number;
  paymentDate?: string;
  paymentReference?: string;
  accountNumber?: string;
  description?: string;
  createdAt: string;
  createdBy: string;
}

export interface FinancialSummary {
  totalRevenue: number;
  totalExpenses: number;
  balance: number;
  transactionCount: number;
}

export interface TransactionRequest {
  caseId: number;
  direction: TransactionDirection;
  operationType: OperationType;
  paymentMode?: PaymentMode;
  amount: number;
  paymentDate?: string;
  paymentReference?: string;
  accountNumber?: string;
  description?: string;
}

export interface FinancialFilter {
  caseId?: number;
  clientId?: number;
  direction?: TransactionDirection;
  operationType?: OperationType;
  dateFrom?: string;
  dateTo?: string;
}

export interface InvoiceItemResponse {
  id: number;
  description: string;
  operationType: OperationType;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface InvoiceResponse {
  id: number;
  caseId: number;
  caseNumber: string;
  invoiceNumber: string;
  issueDate: string;
  dueDate?: string;
  status: InvoiceStatus;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  notes?: string;
  items: InvoiceItemResponse[];
  createdAt: string;
}

export interface InvoiceItemRequest {
  description: string;
  operationType: OperationType;
  quantity: number;
  unitPrice: number;
}

export interface InvoiceRequest {
  caseId: number;
  issueDate: string;
  dueDate?: string;
  taxAmount?: number;
  notes?: string;
  items: InvoiceItemRequest[];
}

export interface InvoiceStatusRequest {
  status: InvoiceStatus;
}
```

**Step 2: Update case.model.ts — rename `totalPayments` → `totalRevenue` in FinancialSummary**

In `frontend/src/app/core/models/case.model.ts`, find the `FinancialSummary` interface:
```typescript
// OLD:
export interface FinancialSummary {
  totalPayments: number;
  totalExpenses: number;
  balance: number;
  transactionCount: number;
}
```
Replace with an import from `financial.model.ts` (to keep a single source of truth):
```typescript
// NEW — replace the FinancialSummary interface definition with:
export type { FinancialSummary } from './financial.model';
```
Or simply update in place:
```typescript
export interface FinancialSummary {
  totalRevenue: number;   // was: totalPayments
  totalExpenses: number;
  balance: number;
  transactionCount: number;
}
```

**Step 3: Type-check**
```bash
cd frontend && pnpm tsc --noEmit
```

---

### Task 21: Create financial.service.ts

**Files:**
- Create: `frontend/src/app/services/financial.service.ts`

```typescript
// frontend/src/app/services/financial.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TransactionResponse,
  TransactionRequest,
  FinancialFilter,
  InvoiceResponse,
  InvoiceRequest,
  InvoiceStatusRequest,
} from '../core/models/financial.model';
import { PageResponse } from '../core/models/case.model';

export interface PageParams {
  page: number;
  size: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class FinancialService {
  private http = inject(HttpClient);
  private readonly base = '/api/financial';

  // ── Transactions ───────────────────────────────────────────────────────────

  getTransactions(
    filter: FinancialFilter,
    paging: PageParams
  ): Observable<PageResponse<TransactionResponse>> {
    let params = new HttpParams()
      .set('page', paging.page.toString())
      .set('size', paging.size.toString())
      .set('sort', paging.sort ?? 'createdAt');
    if (filter.caseId)       params = params.set('caseId', filter.caseId.toString());
    if (filter.clientId)     params = params.set('clientId', filter.clientId.toString());
    if (filter.direction)    params = params.set('direction', filter.direction);
    if (filter.operationType) params = params.set('operationType', filter.operationType);
    if (filter.dateFrom)     params = params.set('dateFrom', filter.dateFrom);
    if (filter.dateTo)       params = params.set('dateTo', filter.dateTo);
    return this.http.get<PageResponse<TransactionResponse>>(`${this.base}/transactions`, { params });
  }

  getTransactionsByCase(caseId: number): Observable<TransactionResponse[]> {
    return this.http.get<TransactionResponse[]>(`${this.base}/cases/${caseId}/transactions`);
  }

  createTransaction(request: TransactionRequest): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.base}/transactions`, request);
  }

  softDeleteTransaction(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/transactions/${id}`);
  }

  exportExcel(filter: FinancialFilter): Observable<Blob> {
    let params = new HttpParams();
    if (filter.caseId)       params = params.set('caseId', filter.caseId.toString());
    if (filter.direction)    params = params.set('direction', filter.direction);
    if (filter.operationType) params = params.set('operationType', filter.operationType);
    if (filter.dateFrom)     params = params.set('dateFrom', filter.dateFrom);
    if (filter.dateTo)       params = params.set('dateTo', filter.dateTo);
    return this.http.get(`${this.base}/transactions/export/excel`, { params, responseType: 'blob' });
  }

  // ── Invoices ───────────────────────────────────────────────────────────────

  getInvoices(paging: PageParams): Observable<PageResponse<InvoiceResponse>> {
    const params = new HttpParams()
      .set('page', paging.page.toString())
      .set('size', paging.size.toString());
    return this.http.get<PageResponse<InvoiceResponse>>(`${this.base}/invoices`, { params });
  }

  getInvoice(id: number): Observable<InvoiceResponse> {
    return this.http.get<InvoiceResponse>(`${this.base}/invoices/${id}`);
  }

  createInvoice(request: InvoiceRequest): Observable<InvoiceResponse> {
    return this.http.post<InvoiceResponse>(`${this.base}/invoices`, request);
  }

  updateInvoiceStatus(id: number, request: InvoiceStatusRequest): Observable<InvoiceResponse> {
    return this.http.patch<InvoiceResponse>(`${this.base}/invoices/${id}/status`, request);
  }

  softDeleteInvoice(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/invoices/${id}`);
  }
}
```

**Step 2: Type-check**
```bash
cd frontend && pnpm tsc --noEmit
```

---

## Batch 6 — Frontend: Routing & Navigation

### Task 22: Update app.routes.ts + sidebar.component.ts

**Files:**
- Modify: `frontend/src/app/app.routes.ts`
- Modify: `frontend/src/app/features/layout/sidebar/sidebar.component.ts`

**Step 1: Add financial routes to app.routes.ts**

Inside the `children` array (after the `cases/:id/edit` route block), add:

```typescript
{
  path: 'financial',
  children: [
    {
      path: '',
      redirectTo: 'ledger',
      pathMatch: 'full',
    },
    {
      path: 'ledger',
      loadComponent: () =>
        import('./features/financial/ledger/financial-ledger.component').then(
          (m) => m.FinancialLedgerComponent
        ),
    },
    {
      path: 'invoices',
      loadComponent: () =>
        import('./features/financial/invoices/invoice-list/invoice-list.component').then(
          (m) => m.InvoiceListComponent
        ),
    },
    {
      path: 'invoices/new',
      loadComponent: () =>
        import('./features/financial/invoices/invoice-form/invoice-form.component').then(
          (m) => m.InvoiceFormComponent
        ),
    },
    {
      path: 'invoices/:id',
      loadComponent: () =>
        import('./features/financial/invoices/invoice-detail/invoice-detail.component').then(
          (m) => m.InvoiceDetailComponent
        ),
    },
  ],
},
```

**Step 2: Add Financial nav item to sidebar.component.ts**

In `navItems` array, add after the Clients entry:

```typescript
{ label: 'Financial', icon: 'account_balance_wallet', route: '/financial', permission: 'FINANCIAL_READ' },
```

**Step 3: Type-check**
```bash
cd frontend && pnpm tsc --noEmit
```

---

## Batch 7 — Frontend: Shared Components

### Task 23: Create financial-summary-card component

**Files:**
- Create: `frontend/src/app/features/financial/shared/financial-summary-card/financial-summary-card.component.ts`
- Create: `frontend/src/app/features/financial/shared/financial-summary-card/financial-summary-card.component.html`

**Step 1: Component class**

```typescript
// financial-summary-card.component.ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FinancialSummary } from '../../../../core/models/financial.model';

@Component({
  selector: 'app-financial-summary-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './financial-summary-card.component.html',
})
export class FinancialSummaryCardComponent {
  @Input({ required: true }) summary!: FinancialSummary;
}
```

**Step 2: Template**

```html
<!-- financial-summary-card.component.html -->
<div class="grid grid-cols-1 md:grid-cols-3 gap-4">
  <!-- Revenue -->
  <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-5 border-l-4 border-green-500">
    <p class="text-sm font-medium text-gray-500 dark:text-gray-400">Revenus</p>
    <p class="mt-1 text-2xl font-bold text-green-600 dark:text-green-400">
      {{ summary.totalRevenue | number:'1.2-2' }} MAD
    </p>
  </div>
  <!-- Expenses -->
  <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-5 border-l-4 border-red-500">
    <p class="text-sm font-medium text-gray-500 dark:text-gray-400">Dépenses</p>
    <p class="mt-1 text-2xl font-bold text-red-600 dark:text-red-400">
      {{ summary.totalExpenses | number:'1.2-2' }} MAD
    </p>
  </div>
  <!-- Balance -->
  <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-5 border-l-4"
       [class.border-green-500]="summary.balance >= 0"
       [class.border-red-500]="summary.balance < 0">
    <p class="text-sm font-medium text-gray-500 dark:text-gray-400">Solde</p>
    <p class="mt-1 text-2xl font-bold"
       [class.text-green-600]="summary.balance >= 0"
       [class.text-red-600]="summary.balance < 0">
      {{ summary.balance | number:'1.2-2' }} MAD
    </p>
    <p class="text-xs text-gray-400 mt-1">{{ summary.transactionCount }} transaction(s)</p>
  </div>
</div>
```

---

## Batch 8 — Frontend: Transaction Feature

### Task 24: Create transaction-form component (modal)

**Files:**
- Create: `frontend/src/app/features/financial/ledger/transaction-form/transaction-form.component.ts`
- Create: `frontend/src/app/features/financial/ledger/transaction-form/transaction-form.component.html`

**Step 1: Component class**

```typescript
// transaction-form.component.ts
import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FinancialService } from '../../../../services/financial.service';
import { CaseService } from '../../../../services/case.service';
import {
  TransactionDirection,
  OperationType,
  PaymentMode,
  TransactionRequest,
} from '../../../../core/models/financial.model';

@Component({
  selector: 'app-transaction-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transaction-form.component.html',
})
export class TransactionFormComponent implements OnInit {
  @Input() caseId?: number;  // Pre-filled when opened from case tab
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  private financialService = inject(FinancialService);
  private caseService = inject(CaseService);

  loading = signal(false);
  error = signal<string | null>(null);

  // Form state
  form = signal<Partial<TransactionRequest>>({
    direction: 'EXPENSE',
    operationType: 'OTHER',
  });

  readonly directionOptions: { value: TransactionDirection; label: string }[] = [
    { value: 'REVENUE', label: 'Revenu' },
    { value: 'EXPENSE', label: 'Dépense' },
  ];

  readonly operationTypeOptions: { value: OperationType; label: string }[] = [
    { value: 'OPENING_FEE',       label: "Frais d'ouverture" },
    { value: 'PROCEDURE_FEE',     label: 'Frais de procédure' },
    { value: 'INTERVENTION_FEE',  label: "Frais d'intervention" },
    { value: 'EXPERT_FEE',        label: "Frais d'expert" },
    { value: 'DOCUMENT_FEE',      label: 'Frais de document' },
    { value: 'NOTIFICATION_FEE',  label: 'Frais de notification' },
    { value: 'JUDICIAL_TAX',      label: 'Taxe judiciaire' },
    { value: 'OTHER',             label: 'Autre' },
  ];

  readonly paymentModeOptions: { value: PaymentMode; label: string }[] = [
    { value: 'CHECK',       label: 'Chèque' },
    { value: 'TRANSFER',    label: 'Virement' },
    { value: 'CASH',        label: 'Espèces' },
    { value: 'CREDIT_CARD', label: 'Carte bancaire' },
    { value: 'MONEY_ORDER', label: 'Mandat' },
  ];

  isExpense = () => this.form().direction === 'EXPENSE';

  ngOnInit(): void {
    if (this.caseId) {
      this.form.update((f) => ({ ...f, caseId: this.caseId }));
    }
  }

  updateField(field: keyof TransactionRequest, value: unknown): void {
    this.form.update((f) => ({ ...f, [field]: value || undefined }));
  }

  onDirectionChange(value: string): void {
    this.form.update((f) => ({
      ...f,
      direction: value as TransactionDirection,
      operationType: value === 'REVENUE' ? 'OTHER' : f.operationType,
    }));
  }

  submit(): void {
    const f = this.form();
    if (!f.caseId || !f.direction || !f.operationType || !f.amount) {
      this.error.set('Veuillez remplir tous les champs obligatoires.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.financialService.createTransaction(f as TransactionRequest).subscribe({
      next: () => {
        this.loading.set(false);
        this.saved.emit();
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Échec de la création');
        this.loading.set(false);
      },
    });
  }
}
```

**Step 2: Template** (modal dialog — follow the same pattern as lawyer/client modals)

```html
<!-- transaction-form.component.html -->
<div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
  <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-lg mx-4 p-6">
    <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">Nouvelle transaction</h2>

    @if (error()) {
      <div class="mb-4 p-3 bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 rounded text-sm">
        {{ error() }}
      </div>
    }

    <div class="space-y-4">
      <!-- Direction -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Direction *</label>
        <select [value]="form().direction"
                (change)="onDirectionChange($any($event.target).value)"
                class="w-full border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent">
          @for (opt of directionOptions; track opt.value) {
            <option [value]="opt.value">{{ opt.label }}</option>
          }
        </select>
      </div>

      <!-- Operation Type (only for EXPENSE) -->
      @if (isExpense()) {
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Type d'opération *</label>
          <select [value]="form().operationType"
                  (change)="updateField('operationType', $any($event.target).value)"
                  class="w-full border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent">
            @for (opt of operationTypeOptions; track opt.value) {
              <option [value]="opt.value">{{ opt.label }}</option>
            }
          </select>
        </div>
      }

      <!-- Amount -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Montant (MAD) *</label>
        <input type="number" step="0.01" min="0.01"
               [value]="form().amount"
               (input)="updateField('amount', +$any($event.target).value)"
               class="w-full border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
      </div>

      <!-- Payment Mode -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Mode de paiement</label>
        <select (change)="updateField('paymentMode', $any($event.target).value || undefined)"
                class="w-full border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent">
          <option value="">— Sélectionner —</option>
          @for (opt of paymentModeOptions; track opt.value) {
            <option [value]="opt.value">{{ opt.label }}</option>
          }
        </select>
      </div>

      <!-- Payment Date -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Date de paiement</label>
        <input type="date"
               (change)="updateField('paymentDate', $any($event.target).value)"
               class="w-full border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
      </div>

      <!-- Reference -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Référence</label>
        <input type="text"
               (input)="updateField('paymentReference', $any($event.target).value)"
               class="w-full border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent" />
      </div>

      <!-- Description -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Description</label>
        <textarea rows="2"
                  (input)="updateField('description', $any($event.target).value)"
                  class="w-full border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"></textarea>
      </div>
    </div>

    <!-- Actions -->
    <div class="flex justify-end gap-3 mt-6">
      <button (click)="cancelled.emit()"
              class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md">
        Annuler
      </button>
      <button (click)="submit()" [disabled]="loading()"
              class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-md">
        {{ loading() ? 'Enregistrement...' : 'Enregistrer' }}
      </button>
    </div>
  </div>
</div>
```

---

### Task 25: Create financial-ledger component (global page)

**Files:**
- Create: `frontend/src/app/features/financial/ledger/financial-ledger.component.ts`
- Create: `frontend/src/app/features/financial/ledger/financial-ledger.component.html`

**Step 1: Component class**

```typescript
// financial-ledger.component.ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FinancialService } from '../../../services/financial.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  TransactionResponse,
  FinancialFilter,
  TransactionDirection,
  OperationType,
} from '../../../core/models/financial.model';
import { PageResponse } from '../../../core/models/case.model';
import { FinancialSummaryCardComponent } from '../shared/financial-summary-card/financial-summary-card.component';
import { TransactionFormComponent } from './transaction-form/transaction-form.component';

@Component({
  selector: 'app-financial-ledger',
  standalone: true,
  imports: [CommonModule, FormsModule, FinancialSummaryCardComponent, TransactionFormComponent],
  templateUrl: './financial-ledger.component.html',
})
export class FinancialLedgerComponent implements OnInit {
  private financialService = inject(FinancialService);
  authService = inject(AuthService);

  // Data
  data = signal<PageResponse<TransactionResponse> | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  exportLoading = signal(false);
  showCreateModal = signal(false);

  // Pagination
  page = signal(0);
  size = signal(20);

  // Filters
  filter = signal<FinancialFilter>({});

  readonly directionOptions: { value: TransactionDirection | ''; label: string }[] = [
    { value: '', label: 'Tous' },
    { value: 'REVENUE', label: 'Revenus' },
    { value: 'EXPENSE', label: 'Dépenses' },
  ];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.financialService
      .getTransactions(this.filter(), { page: this.page(), size: this.size() })
      .subscribe({
        next: (d) => { this.data.set(d); this.loading.set(false); },
        error: (err) => { this.error.set(err.error?.message ?? 'Erreur de chargement'); this.loading.set(false); },
      });
  }

  get summary() {
    const content = this.data()?.content ?? [];
    const totalRevenue  = content.filter((t) => t.direction === 'REVENUE').reduce((s, t) => s + t.amount, 0);
    const totalExpenses = content.filter((t) => t.direction === 'EXPENSE').reduce((s, t) => s + t.amount, 0);
    return {
      totalRevenue,
      totalExpenses,
      balance: totalRevenue - totalExpenses,
      transactionCount: this.data()?.totalElements ?? 0,
    };
  }

  onFilterChange(field: keyof FinancialFilter, value: string): void {
    this.filter.update((f) => ({ ...f, [field]: value || undefined }));
    this.page.set(0);
    this.load();
  }

  onPageChange(newPage: number): void {
    this.page.set(newPage);
    this.load();
  }

  softDelete(id: number): void {
    if (!confirm('Supprimer cette transaction ?')) return;
    this.financialService.softDeleteTransaction(id).subscribe({
      next: () => this.load(),
      error: (err) => this.error.set(err.error?.message ?? 'Échec de la suppression'),
    });
  }

  exportExcel(): void {
    this.exportLoading.set(true);
    this.financialService.exportExcel(this.filter()).subscribe({
      next: (blob) => {
        const url  = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'transactions.xlsx';
        link.click();
        URL.revokeObjectURL(url);
        this.exportLoading.set(false);
      },
      error: () => this.exportLoading.set(false),
    });
  }

  onTransactionSaved(): void {
    this.showCreateModal.set(false);
    this.load();
  }

  directionBadgeClass(direction: TransactionDirection): string {
    return direction === 'REVENUE'
      ? 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200'
      : 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200';
  }
}
```

**Step 2: Template**

```html
<!-- financial-ledger.component.html -->
<div class="space-y-6">
  <!-- Header -->
  <div class="flex items-center justify-between">
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Grand Livre</h1>
    <div class="flex gap-2">
      @if (authService.hasPermission('FINANCIAL_READ')()) {
        <button (click)="exportExcel()" [disabled]="exportLoading()"
                class="flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50">
          {{ exportLoading() ? 'Export...' : 'Exporter Excel' }}
        </button>
      }
      @if (authService.hasPermission('FINANCIAL_CREATE')()) {
        <button (click)="showCreateModal.set(true)"
                class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg">
          + Nouvelle transaction
        </button>
      }
    </div>
  </div>

  <!-- Summary Cards -->
  <app-financial-summary-card [summary]="summary" />

  <!-- Filters -->
  <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-4 flex flex-wrap gap-4">
    <select (change)="onFilterChange('direction', $any($event.target).value)"
            class="border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm">
      @for (opt of directionOptions; track opt.value) {
        <option [value]="opt.value">{{ opt.label }}</option>
      }
    </select>
    <input type="date" placeholder="Date début"
           (change)="onFilterChange('dateFrom', $any($event.target).value)"
           class="border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm" />
    <input type="date" placeholder="Date fin"
           (change)="onFilterChange('dateTo', $any($event.target).value)"
           class="border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm" />
  </div>

  <!-- Table -->
  <div class="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
    @if (loading()) {
      <div class="p-8 text-center text-gray-500 dark:text-gray-400">Chargement...</div>
    } @else if (error()) {
      <div class="p-8 text-center text-red-500">{{ error() }}</div>
    } @else {
      <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
        <thead class="bg-gray-50 dark:bg-gray-900">
          <tr>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Dossier</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Direction</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Type</th>
            <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Montant</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Date</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Référence</th>
            @if (authService.hasPermission('FINANCIAL_UPDATE')()) {
              <th class="px-4 py-3"></th>
            }
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
          @for (tx of data()?.content ?? []; track tx.id) {
            <tr class="hover:bg-gray-50 dark:hover:bg-gray-700">
              <td class="px-4 py-3 text-sm text-gray-900 dark:text-white font-mono">{{ tx.caseNumber }}</td>
              <td class="px-4 py-3">
                <span [class]="directionBadgeClass(tx.direction)"
                      class="px-2 py-0.5 rounded-full text-xs font-medium">
                  {{ tx.direction === 'REVENUE' ? 'Revenu' : 'Dépense' }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{{ tx.operationType }}</td>
              <td class="px-4 py-3 text-sm font-semibold text-right"
                  [class.text-green-600]="tx.direction === 'REVENUE'"
                  [class.text-red-600]="tx.direction === 'EXPENSE'">
                {{ tx.amount | number:'1.2-2' }} MAD
              </td>
              <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{{ tx.paymentDate ?? '—' }}</td>
              <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{{ tx.paymentReference ?? '—' }}</td>
              @if (authService.hasPermission('FINANCIAL_UPDATE')()) {
                <td class="px-4 py-3 text-right">
                  <button (click)="softDelete(tx.id)"
                          class="text-red-500 hover:text-red-700 text-xs">Supprimer</button>
                </td>
              }
            </tr>
          }
          @empty {
            <tr><td colspan="7" class="px-4 py-8 text-center text-gray-500 dark:text-gray-400">Aucune transaction.</td></tr>
          }
        </tbody>
      </table>

      <!-- Pagination -->
      @if ((data()?.totalPages ?? 0) > 1) {
        <div class="px-4 py-3 border-t border-gray-200 dark:border-gray-700 flex items-center justify-between">
          <span class="text-sm text-gray-600 dark:text-gray-400">
            {{ data()?.totalElements }} résultat(s)
          </span>
          <div class="flex gap-2">
            <button [disabled]="data()?.first" (click)="onPageChange(page() - 1)"
                    class="px-3 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded disabled:opacity-40">
              Précédent
            </button>
            <button [disabled]="data()?.last" (click)="onPageChange(page() + 1)"
                    class="px-3 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded disabled:opacity-40">
              Suivant
            </button>
          </div>
        </div>
      }
    }
  </div>
</div>

<!-- Create Modal -->
@if (showCreateModal()) {
  <app-transaction-form
    (saved)="onTransactionSaved()"
    (cancelled)="showCreateModal.set(false)" />
}
```

---

### Task 26: Create case-financial-tab component

**Files:**
- Create: `frontend/src/app/features/cases/case-detail/financial-tab/financial-tab.component.ts`
- Create: `frontend/src/app/features/cases/case-detail/financial-tab/financial-tab.component.html`

**Step 1: Component class**

```typescript
// financial-tab.component.ts
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FinancialService } from '../../../../services/financial.service';
import { AuthService } from '../../../../core/services/auth.service';
import { TransactionResponse } from '../../../../core/models/financial.model';
import { TransactionFormComponent } from '../../../financial/ledger/transaction-form/transaction-form.component';

@Component({
  selector: 'app-financial-tab',
  standalone: true,
  imports: [CommonModule, TransactionFormComponent],
  templateUrl: './financial-tab.component.html',
})
export class FinancialTabComponent implements OnInit {
  @Input({ required: true }) caseId!: number;

  private financialService = inject(FinancialService);
  authService = inject(AuthService);

  transactions = signal<TransactionResponse[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  showCreateModal = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.financialService.getTransactionsByCase(this.caseId).subscribe({
      next: (data) => { this.transactions.set(data); this.loading.set(false); },
      error: (err) => { this.error.set(err.error?.message ?? 'Erreur'); this.loading.set(false); },
    });
  }

  softDelete(id: number): void {
    if (!confirm('Supprimer cette transaction ?')) return;
    this.financialService.softDeleteTransaction(id).subscribe({
      next: () => this.load(),
    });
  }

  onSaved(): void {
    this.showCreateModal.set(false);
    this.load();
  }

  directionBadgeClass(direction: string): string {
    return direction === 'REVENUE'
      ? 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200'
      : 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200';
  }
}
```

**Step 2: Template**

```html
<!-- financial-tab.component.html -->
<div class="space-y-4">
  <div class="flex justify-end">
    @if (authService.hasPermission('FINANCIAL_CREATE')()) {
      <button (click)="showCreateModal.set(true)"
              class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg">
        + Nouvelle transaction
      </button>
    }
  </div>

  @if (loading()) {
    <p class="text-sm text-gray-500 dark:text-gray-400">Chargement...</p>
  } @else if (error()) {
    <p class="text-sm text-red-500">{{ error() }}</p>
  } @else {
    <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700 text-sm">
      <thead class="bg-gray-50 dark:bg-gray-900">
        <tr>
          <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Direction</th>
          <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
          <th class="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">Montant</th>
          <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
          @if (authService.hasPermission('FINANCIAL_UPDATE')()) {
            <th class="px-4 py-2"></th>
          }
        </tr>
      </thead>
      <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
        @for (tx of transactions(); track tx.id) {
          <tr>
            <td class="px-4 py-2">
              <span [class]="directionBadgeClass(tx.direction)"
                    class="px-2 py-0.5 rounded-full text-xs font-medium">
                {{ tx.direction === 'REVENUE' ? 'Revenu' : 'Dépense' }}
              </span>
            </td>
            <td class="px-4 py-2 text-gray-600 dark:text-gray-400">{{ tx.operationType }}</td>
            <td class="px-4 py-2 font-semibold text-right"
                [class.text-green-600]="tx.direction === 'REVENUE'"
                [class.text-red-600]="tx.direction === 'EXPENSE'">
              {{ tx.amount | number:'1.2-2' }} MAD
            </td>
            <td class="px-4 py-2 text-gray-600 dark:text-gray-400">{{ tx.paymentDate ?? '—' }}</td>
            @if (authService.hasPermission('FINANCIAL_UPDATE')()) {
              <td class="px-4 py-2 text-right">
                <button (click)="softDelete(tx.id)" class="text-red-500 hover:text-red-700 text-xs">
                  Supprimer
                </button>
              </td>
            }
          </tr>
        }
        @empty {
          <tr><td colspan="5" class="px-4 py-6 text-center text-gray-500">Aucune transaction.</td></tr>
        }
      </tbody>
    </table>
  }
</div>

@if (showCreateModal()) {
  <app-transaction-form
    [caseId]="caseId"
    (saved)="onSaved()"
    (cancelled)="showCreateModal.set(false)" />
}
```

---

### Task 27: Update case-detail.component — add Financial tab

**Files:**
- Modify: `frontend/src/app/features/cases/case-detail/case-detail.component.ts`
- Modify: `frontend/src/app/features/cases/case-detail/case-detail.component.html`

**Step 1: Update TypeScript — add Financial tab state and import**

In `case-detail.component.ts`:

1. Add `FinancialTabComponent` to imports array:
   ```typescript
   import { FinancialTabComponent } from './financial-tab/financial-tab.component';
   // Add to @Component imports: [..., FinancialTabComponent]
   ```

2. Update `activeTab` type to include `'financial'`:
   ```typescript
   activeTab = signal<'details' | 'children' | 'history' | 'financial'>('details');
   ```

3. Add permission check:
   ```typescript
   canViewFinancials = this.authService.hasPermission('FINANCIAL_READ');
   ```

**Step 2: Update HTML — add tab button + tab panel**

In `case-detail.component.html`, in the tab navigation bar, add the Financial tab button alongside existing tabs:
```html
@if (canViewFinancials()) {
  <button (click)="activeTab.set('financial')"
          [class.border-blue-500]="activeTab() === 'financial'"
          [class.text-blue-600]="activeTab() === 'financial'"
          class="px-4 py-2 text-sm font-medium border-b-2 border-transparent hover:border-gray-300">
    Financier
  </button>
}
```

Add the tab content panel after the existing `@if (activeTab() === 'history')` block:
```html
@if (activeTab() === 'financial' && caseData()) {
  <app-financial-tab [caseId]="caseData()!.id" />
}
```

Also update the financial summary sidebar card to use `totalRevenue` instead of `totalPayments`:
```html
<!-- Find in sidebar: case.financialSummary.totalPayments → replace with: -->
{{ caseData()!.financialSummary.totalRevenue | number:'1.2-2' }} MAD
```

**Step 3: Type-check**
```bash
cd frontend && pnpm tsc --noEmit
```

---

## Batch 9 — Frontend: Invoice Feature ✅ COMPLETED

### Task 28: Create invoice-list component

**Files:**
- Create: `frontend/src/app/features/financial/invoices/invoice-list/invoice-list.component.ts`
- Create: `frontend/src/app/features/financial/invoices/invoice-list/invoice-list.component.html`

**Step 1: Component class**

```typescript
// invoice-list.component.ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FinancialService } from '../../../../services/financial.service';
import { AuthService } from '../../../../core/services/auth.service';
import { InvoiceResponse, InvoiceStatus } from '../../../../core/models/financial.model';
import { PageResponse } from '../../../../core/models/case.model';

@Component({
  selector: 'app-invoice-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './invoice-list.component.html',
})
export class InvoiceListComponent implements OnInit {
  private financialService = inject(FinancialService);
  authService = inject(AuthService);

  data = signal<PageResponse<InvoiceResponse> | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  page = signal(0);
  size = signal(20);

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.financialService.getInvoices({ page: this.page(), size: this.size() }).subscribe({
      next: (d) => { this.data.set(d); this.loading.set(false); },
      error: (err) => { this.error.set(err.error?.message ?? 'Erreur'); this.loading.set(false); },
    });
  }

  onPageChange(newPage: number): void { this.page.set(newPage); this.load(); }

  softDelete(id: number): void {
    if (!confirm('Supprimer cette facture ?')) return;
    this.financialService.softDeleteInvoice(id).subscribe({ next: () => this.load() });
  }

  statusBadgeClass(status: InvoiceStatus): string {
    const map: Record<InvoiceStatus, string> = {
      DRAFT:     'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200',
      SENT:      'bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200',
      PAID:      'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200',
      CANCELLED: 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200',
    };
    return map[status] ?? '';
  }

  statusLabel(status: InvoiceStatus): string {
    const map: Record<InvoiceStatus, string> = {
      DRAFT: 'Brouillon', SENT: 'Envoyée', PAID: 'Payée', CANCELLED: 'Annulée',
    };
    return map[status] ?? status;
  }
}
```

**Step 2: Template**

```html
<!-- invoice-list.component.html -->
<div class="space-y-6">
  <div class="flex items-center justify-between">
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Factures</h1>
    @if (authService.hasPermission('INVOICE_CREATE')()) {
      <a routerLink="/financial/invoices/new"
         class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg">
        + Nouvelle facture
      </a>
    }
  </div>

  <div class="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
    @if (loading()) {
      <div class="p-8 text-center text-gray-500">Chargement...</div>
    } @else if (error()) {
      <div class="p-8 text-center text-red-500">{{ error() }}</div>
    } @else {
      <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
        <thead class="bg-gray-50 dark:bg-gray-900">
          <tr>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">N° Facture</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Dossier</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Statut</th>
            <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Total</th>
            <th class="px-4 py-3"></th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
          @for (inv of data()?.content ?? []; track inv.id) {
            <tr class="hover:bg-gray-50 dark:hover:bg-gray-700">
              <td class="px-4 py-3 text-sm font-mono font-medium text-gray-900 dark:text-white">
                {{ inv.invoiceNumber }}
              </td>
              <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{{ inv.caseNumber }}</td>
              <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{{ inv.issueDate }}</td>
              <td class="px-4 py-3">
                <span [class]="statusBadgeClass(inv.status)"
                      class="px-2 py-0.5 rounded-full text-xs font-medium">
                  {{ statusLabel(inv.status) }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm font-semibold text-right text-gray-900 dark:text-white">
                {{ inv.totalAmount | number:'1.2-2' }} MAD
              </td>
              <td class="px-4 py-3 text-right flex gap-2 justify-end">
                <a [routerLink]="['/financial/invoices', inv.id]"
                   class="text-blue-500 hover:text-blue-700 text-xs">Détail</a>
                @if (authService.hasPermission('INVOICE_MANAGE')()) {
                  <button (click)="softDelete(inv.id)" class="text-red-500 hover:text-red-700 text-xs">
                    Supprimer
                  </button>
                }
              </td>
            </tr>
          }
          @empty {
            <tr><td colspan="6" class="px-4 py-8 text-center text-gray-500">Aucune facture.</td></tr>
          }
        </tbody>
      </table>

      @if ((data()?.totalPages ?? 0) > 1) {
        <div class="px-4 py-3 border-t border-gray-200 dark:border-gray-700 flex justify-between items-center">
          <span class="text-sm text-gray-600">{{ data()?.totalElements }} facture(s)</span>
          <div class="flex gap-2">
            <button [disabled]="data()?.first" (click)="onPageChange(page() - 1)"
                    class="px-3 py-1 text-sm border rounded disabled:opacity-40">Précédent</button>
            <button [disabled]="data()?.last" (click)="onPageChange(page() + 1)"
                    class="px-3 py-1 text-sm border rounded disabled:opacity-40">Suivant</button>
          </div>
        </div>
      }
    }
  </div>
</div>
```

---

### Task 29: Create invoice-form component

**Files:**
- Create: `frontend/src/app/features/financial/invoices/invoice-form/invoice-form.component.ts`
- Create: `frontend/src/app/features/financial/invoices/invoice-form/invoice-form.component.html`

**Step 1: Component class**

```typescript
// invoice-form.component.ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FinancialService } from '../../../../services/financial.service';
import { CaseService } from '../../../../services/case.service';
import {
  InvoiceItemRequest,
  InvoiceRequest,
  OperationType,
} from '../../../../core/models/financial.model';
import { CaseSummaryResponse } from '../../../../core/models/case.model';

@Component({
  selector: 'app-invoice-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './invoice-form.component.html',
})
export class InvoiceFormComponent implements OnInit {
  private financialService = inject(FinancialService);
  private caseService = inject(CaseService);
  private router = inject(Router);

  loading = signal(false);
  error = signal<string | null>(null);
  cases = signal<CaseSummaryResponse[]>([]);

  // Form
  caseId = signal<number | null>(null);
  issueDate = signal('');
  dueDate = signal('');
  taxAmount = signal(0);
  notes = signal('');
  items = signal<InvoiceItemRequest[]>([{ description: '', operationType: 'OTHER', quantity: 1, unitPrice: 0 }]);

  readonly operationTypeOptions: { value: OperationType; label: string }[] = [
    { value: 'OPENING_FEE',      label: "Frais d'ouverture" },
    { value: 'PROCEDURE_FEE',    label: 'Frais de procédure' },
    { value: 'INTERVENTION_FEE', label: "Frais d'intervention" },
    { value: 'EXPERT_FEE',       label: "Frais d'expert" },
    { value: 'DOCUMENT_FEE',     label: 'Frais de document' },
    { value: 'NOTIFICATION_FEE', label: 'Frais de notification' },
    { value: 'JUDICIAL_TAX',     label: 'Taxe judiciaire' },
    { value: 'OTHER',            label: 'Autre' },
  ];

  ngOnInit(): void {
    // Load case list for the selector
    this.caseService.getCases({ page: 0, size: 100 }).subscribe({
      next: (page) => this.cases.set(page.content.map((c) => ({ id: c.id, fullCaseNumber: c.fullCaseNumber }))),
    });
  }

  get subtotal(): number {
    return this.items().reduce((s, item) => s + item.quantity * item.unitPrice, 0);
  }

  get total(): number {
    return this.subtotal + this.taxAmount();
  }

  addItem(): void {
    this.items.update((list) => [...list, { description: '', operationType: 'OTHER', quantity: 1, unitPrice: 0 }]);
  }

  removeItem(index: number): void {
    this.items.update((list) => list.filter((_, i) => i !== index));
  }

  updateItem(index: number, field: keyof InvoiceItemRequest, value: unknown): void {
    this.items.update((list) => {
      const updated = [...list];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  }

  submit(): void {
    if (!this.caseId() || !this.issueDate() || this.items().length === 0) {
      this.error.set('Dossier, date et au moins un article sont obligatoires.');
      return;
    }
    const request: InvoiceRequest = {
      caseId: this.caseId()!,
      issueDate: this.issueDate(),
      dueDate: this.dueDate() || undefined,
      taxAmount: this.taxAmount(),
      notes: this.notes() || undefined,
      items: this.items(),
    };
    this.loading.set(true);
    this.error.set(null);
    this.financialService.createInvoice(request).subscribe({
      next: (inv) => this.router.navigate(['/financial/invoices', inv.id]),
      error: (err) => { this.error.set(err.error?.message ?? 'Échec'); this.loading.set(false); },
    });
  }

  cancel(): void {
    this.router.navigate(['/financial/invoices']);
  }
}
```

**Step 2: Template** (dynamic FormArray-style with signal-based rows)

```html
<!-- invoice-form.component.html -->
<div class="max-w-3xl mx-auto space-y-6">
  <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Nouvelle facture</h1>

  @if (error()) {
    <div class="p-3 bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 rounded text-sm">
      {{ error() }}
    </div>
  }

  <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6 space-y-4">
    <!-- Case selector -->
    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Dossier *</label>
      <select (change)="caseId.set(+$any($event.target).value || null)"
              class="w-full border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm">
        <option value="">— Sélectionner un dossier —</option>
        @for (c of cases(); track c.id) {
          <option [value]="c.id">{{ c.fullCaseNumber }}</option>
        }
      </select>
    </div>

    <!-- Dates -->
    <div class="grid grid-cols-2 gap-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Date d'émission *</label>
        <input type="date" (change)="issueDate.set($any($event.target).value)"
               class="w-full border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm" />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Date d'échéance</label>
        <input type="date" (change)="dueDate.set($any($event.target).value)"
               class="w-full border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm" />
      </div>
    </div>

    <!-- Notes -->
    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Notes</label>
      <textarea rows="2" (input)="notes.set($any($event.target).value)"
                class="w-full border border-gray-300 dark:border-gray-600 rounded-md px-3 py-2 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm"></textarea>
    </div>
  </div>

  <!-- Line items -->
  <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6 space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold text-gray-900 dark:text-white">Articles</h2>
      <button (click)="addItem()"
              class="text-sm text-blue-600 hover:text-blue-800 font-medium">+ Ajouter</button>
    </div>

    @for (item of items(); track $index; let i = $index) {
      <div class="grid grid-cols-12 gap-2 items-end border-b border-gray-100 dark:border-gray-700 pb-4">
        <div class="col-span-4">
          <label class="block text-xs text-gray-500 mb-1">Description *</label>
          <input type="text" [value]="item.description"
                 (input)="updateItem(i, 'description', $any($event.target).value)"
                 class="w-full border border-gray-300 dark:border-gray-600 rounded px-2 py-1.5 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm" />
        </div>
        <div class="col-span-3">
          <label class="block text-xs text-gray-500 mb-1">Type</label>
          <select [value]="item.operationType"
                  (change)="updateItem(i, 'operationType', $any($event.target).value)"
                  class="w-full border border-gray-300 dark:border-gray-600 rounded px-2 py-1.5 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm">
            @for (opt of operationTypeOptions; track opt.value) {
              <option [value]="opt.value">{{ opt.label }}</option>
            }
          </select>
        </div>
        <div class="col-span-1">
          <label class="block text-xs text-gray-500 mb-1">Qté</label>
          <input type="number" min="1" [value]="item.quantity"
                 (input)="updateItem(i, 'quantity', +$any($event.target).value)"
                 class="w-full border border-gray-300 dark:border-gray-600 rounded px-2 py-1.5 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm" />
        </div>
        <div class="col-span-2">
          <label class="block text-xs text-gray-500 mb-1">P.U. (MAD)</label>
          <input type="number" step="0.01" min="0" [value]="item.unitPrice"
                 (input)="updateItem(i, 'unitPrice', +$any($event.target).value)"
                 class="w-full border border-gray-300 dark:border-gray-600 rounded px-2 py-1.5 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm" />
        </div>
        <div class="col-span-1 text-right text-sm font-semibold text-gray-700 dark:text-gray-300">
          {{ (item.quantity * item.unitPrice) | number:'1.2-2' }}
        </div>
        <div class="col-span-1 text-right">
          @if (items().length > 1) {
            <button (click)="removeItem(i)" class="text-red-500 hover:text-red-700 text-xs">✕</button>
          }
        </div>
      </div>
    }

    <!-- Totals -->
    <div class="space-y-2 text-right">
      <p class="text-sm text-gray-600 dark:text-gray-400">
        Sous-total: <span class="font-semibold">{{ subtotal | number:'1.2-2' }} MAD</span>
      </p>
      <div class="flex items-center justify-end gap-4">
        <label class="text-sm text-gray-600 dark:text-gray-400">TVA (MAD):</label>
        <input type="number" step="0.01" min="0" [value]="taxAmount()"
               (input)="taxAmount.set(+$any($event.target).value)"
               class="w-28 border border-gray-300 dark:border-gray-600 rounded px-2 py-1 bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm text-right" />
      </div>
      <p class="text-base font-bold text-gray-900 dark:text-white">
        Total: {{ total | number:'1.2-2' }} MAD
      </p>
    </div>
  </div>

  <!-- Actions -->
  <div class="flex justify-end gap-3">
    <button (click)="cancel()"
            class="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md">
      Annuler
    </button>
    <button (click)="submit()" [disabled]="loading()"
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-md">
      {{ loading() ? 'Création...' : 'Créer la facture' }}
    </button>
  </div>
</div>
```

---

### Task 30: Create invoice-detail component

**Files:**
- Create: `frontend/src/app/features/financial/invoices/invoice-detail/invoice-detail.component.ts`
- Create: `frontend/src/app/features/financial/invoices/invoice-detail/invoice-detail.component.html`

**Step 1: Component class**

```typescript
// invoice-detail.component.ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FinancialService } from '../../../../services/financial.service';
import { AuthService } from '../../../../core/services/auth.service';
import { InvoiceResponse, InvoiceStatus } from '../../../../core/models/financial.model';

@Component({
  selector: 'app-invoice-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './invoice-detail.component.html',
})
export class InvoiceDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private financialService = inject(FinancialService);
  authService = inject(AuthService);

  invoice = signal<InvoiceResponse | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  statusLoading = signal(false);

  // Valid next statuses for the current status
  readonly TRANSITIONS: Partial<Record<InvoiceStatus, InvoiceStatus[]>> = {
    DRAFT: ['SENT', 'CANCELLED'],
    SENT:  ['PAID', 'CANCELLED'],
  };

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.load(+id);
  }

  load(id: number): void {
    this.loading.set(true);
    this.financialService.getInvoice(id).subscribe({
      next: (inv) => { this.invoice.set(inv); this.loading.set(false); },
      error: (err) => { this.error.set(err.error?.message ?? 'Erreur'); this.loading.set(false); },
    });
  }

  nextStatuses(): InvoiceStatus[] {
    const inv = this.invoice();
    return inv ? (this.TRANSITIONS[inv.status] ?? []) : [];
  }

  transitionTo(status: InvoiceStatus): void {
    const inv = this.invoice();
    if (!inv || !confirm(`Passer la facture en statut "${status}" ?`)) return;
    this.statusLoading.set(true);
    this.financialService.updateInvoiceStatus(inv.id, { status }).subscribe({
      next: (updated) => { this.invoice.set(updated); this.statusLoading.set(false); },
      error: (err) => { this.error.set(err.error?.message ?? 'Échec'); this.statusLoading.set(false); },
    });
  }

  statusBadgeClass(status: InvoiceStatus): string {
    const map: Record<InvoiceStatus, string> = {
      DRAFT:     'bg-gray-100 text-gray-800',
      SENT:      'bg-blue-100 text-blue-800',
      PAID:      'bg-green-100 text-green-800',
      CANCELLED: 'bg-red-100 text-red-800',
    };
    return map[status] ?? '';
  }

  statusLabel(status: InvoiceStatus): string {
    const map: Record<InvoiceStatus, string> = {
      DRAFT: 'Brouillon', SENT: 'Envoyée', PAID: 'Payée', CANCELLED: 'Annulée',
    };
    return map[status] ?? status;
  }
}
```

**Step 2: Template**

```html
<!-- invoice-detail.component.html -->
@if (loading()) {
  <div class="p-8 text-center text-gray-500">Chargement...</div>
} @else if (error()) {
  <div class="p-8 text-center text-red-500">{{ error() }}</div>
} @else if (invoice(); as inv) {
  <div class="max-w-3xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <p class="text-sm text-gray-500 dark:text-gray-400">
          <a routerLink="/financial/invoices" class="hover:underline">Factures</a> /
          {{ inv.invoiceNumber }}
        </p>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white mt-1">{{ inv.invoiceNumber }}</h1>
      </div>
      <div class="flex items-center gap-3">
        <span [class]="statusBadgeClass(inv.status)"
              class="px-3 py-1 rounded-full text-sm font-medium">
          {{ statusLabel(inv.status) }}
        </span>
        @if (authService.hasPermission('INVOICE_MANAGE')()) {
          @for (next of nextStatuses(); track next) {
            <button (click)="transitionTo(next)" [disabled]="statusLoading()"
                    class="px-3 py-1 text-sm text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-md">
              → {{ statusLabel(next) }}
            </button>
          }
        }
      </div>
    </div>

    <!-- Meta -->
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6 grid grid-cols-2 gap-4 text-sm">
      <div>
        <p class="text-gray-500">Dossier</p>
        <p class="font-medium text-gray-900 dark:text-white">{{ inv.caseNumber }}</p>
      </div>
      <div>
        <p class="text-gray-500">Date d'émission</p>
        <p class="font-medium text-gray-900 dark:text-white">{{ inv.issueDate }}</p>
      </div>
      <div>
        <p class="text-gray-500">Date d'échéance</p>
        <p class="font-medium text-gray-900 dark:text-white">{{ inv.dueDate ?? '—' }}</p>
      </div>
      @if (inv.notes) {
        <div class="col-span-2">
          <p class="text-gray-500">Notes</p>
          <p class="text-gray-900 dark:text-white">{{ inv.notes }}</p>
        </div>
      }
    </div>

    <!-- Items table -->
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
      <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
        <thead class="bg-gray-50 dark:bg-gray-900">
          <tr>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Description</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
            <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Qté</th>
            <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">P.U.</th>
            <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Total</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
          @for (item of inv.items; track item.id) {
            <tr>
              <td class="px-4 py-3 text-sm text-gray-900 dark:text-white">{{ item.description }}</td>
              <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{{ item.operationType }}</td>
              <td class="px-4 py-3 text-sm text-right text-gray-900 dark:text-white">{{ item.quantity }}</td>
              <td class="px-4 py-3 text-sm text-right text-gray-900 dark:text-white">
                {{ item.unitPrice | number:'1.2-2' }}
              </td>
              <td class="px-4 py-3 text-sm font-semibold text-right text-gray-900 dark:text-white">
                {{ item.lineTotal | number:'1.2-2' }} MAD
              </td>
            </tr>
          }
        </tbody>
        <tfoot class="bg-gray-50 dark:bg-gray-900">
          <tr>
            <td colspan="4" class="px-4 py-2 text-sm text-right text-gray-500">Sous-total</td>
            <td class="px-4 py-2 text-sm font-medium text-right text-gray-900 dark:text-white">
              {{ inv.subtotal | number:'1.2-2' }} MAD
            </td>
          </tr>
          <tr>
            <td colspan="4" class="px-4 py-2 text-sm text-right text-gray-500">TVA</td>
            <td class="px-4 py-2 text-sm font-medium text-right text-gray-900 dark:text-white">
              {{ inv.taxAmount | number:'1.2-2' }} MAD
            </td>
          </tr>
          <tr>
            <td colspan="4" class="px-4 py-2 text-sm font-bold text-right text-gray-900 dark:text-white">Total</td>
            <td class="px-4 py-2 text-base font-bold text-right text-blue-600">
              {{ inv.totalAmount | number:'1.2-2' }} MAD
            </td>
          </tr>
        </tfoot>
      </table>
    </div>
  </div>
}
```

---

## Batch 10 — Final Verification ✅ COMPLETED

### Task 31: Full-stack verification

**Step 1: Backend clean build**
```bash
cd backend && mvn clean verify
```
Expected: `BUILD SUCCESS` — all Flyway migrations applied, all tests pass.

**Step 2: Frontend type-check + lint**
```bash
cd frontend && pnpm tsc --noEmit && pnpm lint
```
Expected: zero type errors, zero lint errors.

**Step 3: Run both servers and smoke-test**
```bash
# Terminal 1
cd backend && mvn spring-boot:run

# Terminal 2
cd frontend && pnpm dev
```

Smoke-test checklist:
- [ ] Login as `admin` → sidebar shows "Financial" link
- [ ] Navigate to `/financial/ledger` → page loads, summary cards visible
- [ ] Create a transaction → appears in table, direction badge correct
- [ ] Navigate to a case detail → "Financier" tab appears, shows case transactions
- [ ] Navigate to `/financial/invoices` → invoice list page loads
- [ ] Create an invoice with 2 line items → redirects to invoice detail, totals correct
- [ ] Transition invoice status DRAFT → SENT → valid, PAID → SENT blocked (error)
- [ ] Login as `test_viewer` → Financial nav item hidden (no FINANCIAL_READ), endpoints return 403

**Step 4: Post-implementation check — `CaseMapper` field name in `case-detail.component.html`**

Search the entire frontend for `totalPayments` and confirm zero occurrences:
```bash
cd frontend && grep -r "totalPayments" src/
```
Expected: no output.

---

## Key Implementation Notes

### Backend Gotchas
1. **Line endings:** All `.sql` and `.java` files must use **LF** endings. The `.gitattributes` enforces this via git, but if you create files on Windows, configure your editor to write LF.
2. **JPQL enum literals:** Use the fully-qualified enum in JPQL strings (e.g., `com.lawfirm.domain.model.FinancialTransaction.Direction.REVENUE`) OR use a parameter (`@Param`). The repository in Task 8 uses the fully-qualified form in the JPQL string literals — verify this compiles correctly in your environment.
3. **`BaseEntity.createdBy`:** The `FinancialTransactionMapper` maps `createdBy` via expression. Check your `BaseEntity` for the exact field name (it may be `createdBy` from Spring Data `@CreatedBy` or a custom field).
4. **`CaseRepository.findById`:** Used in `FinancialTransactionService` and `InvoiceService`. This already exists from the Case management feature.
5. **`InvoiceService.generateInvoiceNumber`:** Uses `invoice_number_seq` DB sequence (created in V47). The `EntityManager` is auto-wired by Spring. The `synchronized` keyword prevents race conditions within a single JVM; for multi-node deployments, the DB sequence itself is the authoritative lock.

### Frontend Gotchas
1. **`CaseService.getCases()`:** Used in `invoice-form.component` to populate the case selector. Verify the exact method signature in your `case.service.ts` — it may be `search()` or `getAll()` with different parameters. Adjust accordingly.
2. **`authService.hasPermission()`:** Returns a **signal** in this codebase (based on the existing case-detail pattern). Call it as `authService.hasPermission('FINANCIAL_READ')()` (note the trailing `()`) when used inside templates or computed values.
3. **`PageResponse<T>` import:** This is defined in `case.model.ts`. The `financial.service.ts` imports it from there — do not duplicate the definition.
4. **New file directories:** Create all intermediate directories before writing files. Angular's lazy loading requires the exact path to match the `loadComponent` import in `app.routes.ts`.
