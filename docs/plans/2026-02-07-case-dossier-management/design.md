# Case/Dossier Management - Design Document

**Date:** 2026-02-07
**Status:** Design Complete - Ready for Implementation
**Priority:** 1 (Core Legal Operations)

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture & Data Flow](#architecture--data-flow)
3. [Domain Model](#domain-model)
4. [DTOs & Validation](#dtos--validation)
5. [Service Layer](#service-layer)
6. [Database Schema](#database-schema)
7. [REST API](#rest-api)
8. [Frontend Components](#frontend-components)
9. [Error Handling](#error-handling)
10. [Security & Permissions](#security--permissions)
11. [Testing Strategy](#testing-strategy)
12. [Bilingual Support](#bilingual-support)

---

## Overview

### Purpose
Implement comprehensive case/dossier management system for law firm operations. This is the core feature enabling lawyers to track legal cases from intake to closure with full lifecycle management.

### Key Requirements
- **Custom case numbering** with configurable templates per case type
- **Flexible workflows** with configurable statuses per case type
- **Bilingual support** (French/Arabic) for all reference data
- **Financial tracking** with separate transaction entity
- **Reference data** management (Tribunals, Case Types, Statuses)
- **Lawyer assignment** with dedicated Lawyer entity
- **Audit trail** for all case changes

### Business Context
Based on specific field requirements:
- **Année** (Year) - Case year
- **N° Dossier** (5 digits) - Sequential case number
- **Code N° dossier** - Full case number from template
- **Date enregistrement** - Registration date
- **Description Dossier** - Case description
- **Description affaire** - Matter description
- **Nature affaire** - Case type (PENAL, COMMERC, CIVIL, ADM)
- **Nom Tribunal / Tribunal code** - Court information
- **AVOCAT / Identifiant Fiscal** - Lawyer information
- **Financial fields** - Tracked separately in FinancialTransaction entity

---

## Architecture & Data Flow

### Hexagonal Architecture Layers

**Domain Layer** (`com.lawfirm.domain.case`):
- Entities: `Case`, `Tribunal`, `CaseType`, `CaseStatus`, `Lawyer`, `FinancialTransaction`
- Repositories: `CaseRepository`, `TribunalRepository`, `CaseTypeRepository`, etc.
- Specifications: `CaseSpecification` for dynamic filtering

**Application Layer** (`com.lawfirm.application.case`):
- DTOs: Request/Response objects
- Mappers: MapStruct interfaces
- Services: Business logic implementation
- Events: `CaseCreatedEvent`, `CaseUpdatedEvent`, `CaseStatusChangedEvent`

**Infrastructure Layer** (`com.lawfirm.infrastructure`):
- Security: Permission-based access control
- Configuration: Caching, auditing
- Integration: Future integrations (email, SMS)

**Presentation Layer** (`com.lawfirm.presentation.case`):
- Controllers: REST endpoints
- Exception handling: Custom exceptions

### Data Flow

```
User → Angular Component → CaseService (Angular)
    ↓
JWT Auth Interceptor → Backend Controller
    ↓
@PreAuthorize Permission Check → Service Layer
    ↓
MapStruct Mapper → Repository → PostgreSQL
    ↓
Publish Audit Events → AuditLog
    ↓
Return DTO → Angular Component → UI Display
```

---

## Domain Model

### Case Entity

```java
@Entity
@Table(name = "cases")
public class Case extends BaseEntity {

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer sequenceNumber; // 5-digit sequential

    @Column(nullable = false, unique = true, length = 255)
    private String fullCaseNumber; // Generated from template

    @Column(nullable = false)
    private LocalDate registrationDate;

    @Column(nullable = false, length = 500)
    private String caseDescription;

    @Column(columnDefinition = "TEXT")
    private String matterDescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tribunal_id", nullable = false)
    private Tribunal tribunal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_type_id", nullable = false)
    private CaseType caseType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_category_id")
    private CaseCategory caseCategory; // Optional: detailed classification

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lawyer_id", nullable = false)
    private Lawyer lawyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private CaseStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // Soft delete

    @OneToMany(mappedBy = "case", cascade = CascadeType.ALL)
    private List<FinancialTransaction> transactions = new ArrayList<>();
}
```

**Key Design Decisions:**
- `fullCaseNumber` is unique and immutable after creation
- `sequenceNumber` is generated atomically via `CaseSequenceService`
- Soft delete via `deletedAt` for audit trail
- Lazy fetch for relationships to prevent N+1 queries

### Tribunal Entity

```java
@Entity
@Table(name = "tribunals")
public class Tribunal extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code; // e.g., "TR_PIN_1"

    @Column(nullable = false, length = 255)
    private String nameFr; // French name

    @Column(nullable = false, length = 255)
    private String nameAr; // Arabic name

    @Column(nullable = false)
    private Boolean active = true;
}
```

**Seeded Data:**
- 130+ Moroccan courts (Administrative, Commercial, Appeal, First Instance, Cassation)
- Examples:
  - `TR_ADM_APPL_1`: Tribunal d'appel administratif de Rabat / محكمة الاستئناف الإدارية بالرباط
  - `TR_COM_PIN_2`: Tribunal commercial de Casablanca / المحكمة التجارية - الدار البيضاء
  - `TR_PIN_1`: Tribunal de 1ère instance de Rabat / المحكمة الابتدائية - الرباط

### CaseType Entity

```java
@Entity
@Table(name = "case_types")
public class CaseType extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code; // PENAL, COMMERC, CIVIL, ADM

    @Column(nullable = false, length = 100)
    private String nameFr;

    @Column(length = 100)
    private String nameAr;

    @Column(nullable = false, length = 255)
    private String numberFormatTemplate; // e.g., "{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}"

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "case_type_statuses",
        joinColumns = @JoinColumn(name = "case_type_id"),
        inverseJoinColumns = @JoinColumn(name = "status_id")
    )
    private Set<CaseStatus> allowedStatuses = new HashSet<>();
}
```

**Seeded Data:**
- `PENAL`: Pénale / جنائي
- `COMMERC`: Commerciale / تجاري
- `CIVIL`: Civile / مدني
- `ADM`: Administrative / إداري

**Template Placeholders:**
- `{YEAR}`: Current year (e.g., 2026)
- `{TRIBUNAL_CODE}`: Tribunal code (e.g., TR_PIN_1)
- `{CASETYPE}`: Case type code (e.g., PENAL)
- `{SEQ5}`: 5-digit zero-padded sequence (e.g., 00001)

**Example Generated Number:**
`2026-TR_PIN_1-PENAL-00001`

### CaseCategory Entity

```java
@Entity
@Table(name = "case_categories")
public class CaseCategory extends BaseEntity {

    @Column(nullable = false, unique = true, length = 10)
    private String code; // e.g., "7101", "8101", "1101"

    @Column(nullable = false, length = 255)
    private String nameAr; // Arabic description

    @Column(length = 255)
    private String nameFr; // French description (optional, can be added later)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_type_id")
    private CaseType caseType; // Links to parent case type (PENAL, COMMERC, CIVIL, ADM)

    @Column(nullable = false)
    private Boolean active = true;
}
```

**Purpose:**
Detailed classification system for Moroccan court cases. Provides granular categorization beyond the 4 main case types (PENAL, COMMERC, CIVIL, ADM). Each category code (e.g., 7101) represents a specific type of legal proceeding with its official Arabic name.

**Examples:**
- `7101`: القضايا الاستعجالية (Urgent/Emergency cases - Administrative)
- `8101`: الاستعجالي (Urgent - Commercial)
- `1101`: الاستعجالي (Urgent - Civil)
- `2101`: جنحي عادي تأديبي (Criminal - normal disciplinary)

**Seeded Data:**
300+ official case categories from Moroccan judicial system, organized by:
- **7xxx**: Administrative court cases
- **8xxx**: Commercial court cases
- **1xxx**: Civil first instance cases
- **2xxx**: Criminal cases
- **6xxx**: Execution/enforcement cases
- **3, 4, 6**: Court of cassation codes

**Integration with Case Entity:**
Cases can optionally specify a `CaseCategory` for detailed classification. The category's `caseType` must match the case's `caseType` for consistency.

### CaseStatus Entity

```java
@Entity
@Table(name = "case_statuses")
public class CaseStatus extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String nameFr;

    @Column(length = 100)
    private String nameAr;

    @Column(nullable = false)
    private Integer sortOrder = 0; // Display order

    @Column(nullable = false)
    private Boolean isTerminal = false; // CLOSED, ARCHIVED
}
```

**Seeded Data:**
- `DRAFT`: Brouillon / مسودة (sortOrder: 1)
- `OPEN`: Ouvert / مفتوح (sortOrder: 2)
- `IN_PROGRESS`: En cours / قيد التقدم (sortOrder: 3)
- `HEARING`: Audience / جلسة (sortOrder: 4)
- `JUDGMENT`: Jugement / حكم (sortOrder: 5)
- `CLOSED`: Clôturé / مغلق (sortOrder: 6, isTerminal: true)
- `ARCHIVED`: Archivé / مؤرشف (sortOrder: 7, isTerminal: true)

### Lawyer Entity

```java
@Entity
@Table(name = "lawyers")
public class Lawyer extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(unique = true, length = 50)
    private String taxId; // Identifiant Fiscal

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "lawyer")
    private List<Case> cases = new ArrayList<>();

    // Computed property
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
```

### FinancialTransaction Entity

```java
@Entity
@Table(name = "financial_transactions")
public class FinancialTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType transactionType; // PAYMENT, EXPENSE

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date")
    private LocalDate paymentDate; // Date encaissement

    @Column(name = "payment_reference", length = 100)
    private String paymentReference; // Référence paiement

    @Column(name = "lawyer_payment_year")
    private Integer lawyerPaymentYear; // Annee Versement AVOCAT

    @Column(name = "fiscal_year_from")
    private LocalDate fiscalYearFrom; // Exercice Fiscal Du

    @Column(name = "fiscal_year_to")
    private LocalDate fiscalYearTo; // Exercice Fiscal Au

    @Column(columnDefinition = "TEXT")
    private String description;
}

enum TransactionType {
    PAYMENT,
    EXPENSE
}
```

### CaseSequence Entity

```java
@Entity
@Table(name = "case_sequences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"year", "case_type_code"}))
public class CaseSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, length = 20)
    private String caseTypeCode;

    @Column(nullable = false)
    private Integer lastSequence = 0;
}
```

---

## DTOs & Validation

### Request DTOs

**CreateCaseRequest:**
```java
public record CreateCaseRequest(
    @NotBlank(message = "Case type code is required")
    @Size(max = 20)
    String caseTypeCode,

    @Size(max = 10)
    String caseCategoryCode, // Optional: detailed category code (e.g., "7101", "8101")

    @NotBlank(message = "Tribunal code is required")
    @Size(max = 50)
    String tribunalCode,

    @NotNull(message = "Lawyer ID is required")
    Long lawyerId,

    @NotNull(message = "Registration date is required")
    @PastOrPresent(message = "Registration date cannot be in the future")
    LocalDate registrationDate,

    @NotBlank(message = "Case description is required")
    @Size(max = 500, message = "Case description must not exceed 500 characters")
    String caseDescription,

    @Size(max = 1000, message = "Matter description must not exceed 1000 characters")
    String matterDescription,

    @Size(max = 50)
    String initialStatusCode // Optional, defaults to case type's initial status
) {}
```

**UpdateCaseRequest:**
```java
public record UpdateCaseRequest(
    @Size(max = 50)
    String tribunalCode,

    Long lawyerId,

    @PastOrPresent
    LocalDate registrationDate,

    @Size(max = 500)
    String caseDescription,

    @Size(max = 1000)
    String matterDescription

    // Note: year, sequenceNumber, fullCaseNumber, caseType are IMMUTABLE
) {}
```

**CaseSearchRequest:**
```java
public record CaseSearchRequest(
    Integer year,
    String caseTypeCode,
    String tribunalCode,
    Long lawyerId,
    String statusCode,
    LocalDate registrationDateFrom,
    LocalDate registrationDateTo,

    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size,
    String sortBy,
    String sortDirection // ASC, DESC
) {}
```

**ChangeStatusRequest:**
```java
public record ChangeStatusRequest(
    @NotBlank(message = "Status code is required")
    String statusCode,

    @Size(max = 500)
    String reason // Optional reason for audit
) {}
```

### Response DTOs

**CaseResponse (full details):**
```java
public record CaseResponse(
    Long id,
    Long version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    Integer year,
    Integer sequenceNumber,
    String fullCaseNumber,
    LocalDate registrationDate,
    String caseDescription,
    String matterDescription,

    TribunalResponse tribunal,
    CaseTypeResponse caseType,
    CaseCategoryResponse caseCategory, // Optional: detailed classification
    LawyerSummary lawyer,
    CaseStatusResponse status,

    FinancialSummary financialSummary // Computed
) {}
```

**CaseSummary (list view):**
```java
public record CaseSummary(
    Long id,
    String fullCaseNumber,
    String caseDescription,
    String tribunalNameFr,
    String caseTypeNameFr,
    String lawyerName,
    String statusNameFr,
    LocalDate registrationDate
) {}
```

**TribunalResponse:**
```java
public record TribunalResponse(
    Long id,
    String code,
    String nameFr,
    String nameAr,
    Boolean active
) {}
```

**CaseTypeResponse:**
```java
public record CaseTypeResponse(
    Long id,
    String code,
    String nameFr,
    String nameAr,
    String numberFormatTemplate,
    Boolean active,
    List<CaseStatusResponse> allowedStatuses
) {}
```

**CaseStatusResponse:**
```java
public record CaseStatusResponse(
    Long id,
    String code,
    String nameFr,
    String nameAr,
    Integer sortOrder,
    Boolean isTerminal
) {}
```

**CaseCategoryResponse:**
```java
public record CaseCategoryResponse(
    Long id,
    String code,
    String nameAr,
    String nameFr,
    String caseTypeCode
) {}
```

**LawyerSummary:**
```java
public record LawyerSummary(
    Long id,
    String fullName,
    String taxId,
    Boolean active
) {}
```

**FinancialSummary:**
```java
public record FinancialSummary(
    BigDecimal totalPayments,
    BigDecimal totalExpenses,
    BigDecimal balance,
    Integer transactionCount
) {}
```

### MapStruct Mappers

**CaseMapper:**
```java
@Mapper(componentModel = "spring", uses = {
    TribunalMapper.class,
    CaseTypeMapper.class,
    LawyerMapper.class,
    CaseStatusMapper.class
})
public interface CaseMapper {

    CaseResponse toResponse(Case case);

    CaseSummary toSummary(Case case);

    List<CaseResponse> toResponseList(List<Case> cases);

    List<CaseSummary> toSummaryList(List<Case> cases);

    @Mapping(target = "financialSummary", expression = "java(calculateFinancialSummary(case))")
    CaseResponse toResponseWithFinancials(Case case);

    default FinancialSummary calculateFinancialSummary(Case case) {
        // Calculate from transactions
    }
}
```

---

## Service Layer

### CaseService

**Core Operations:**

**1. Create Case:**
```java
@Transactional
public CaseResponse createCase(CreateCaseRequest request, UserPrincipal currentUser) {
    // 1. Validate references
    CaseType caseType = caseTypeRepository.findByCodeAndActiveTrue(request.caseTypeCode())
        .orElseThrow(() -> new ResourceNotFoundException("CaseType", "code", request.caseTypeCode()));

    Tribunal tribunal = tribunalRepository.findByCodeAndActiveTrue(request.tribunalCode())
        .orElseThrow(() -> new ResourceNotFoundException("Tribunal", "code", request.tribunalCode()));

    Lawyer lawyer = lawyerRepository.findByIdAndActiveTrue(request.lawyerId())
        .orElseThrow(() -> new ResourceNotFoundException("Lawyer", "id", request.lawyerId()));

    // 2. Determine initial status
    CaseStatus initialStatus = determineInitialStatus(caseType, request.initialStatusCode());

    // 3. Generate case number
    int year = Year.now().getValue();
    int sequenceNumber = caseSequenceService.getNextSequence(year, caseType.getCode());
    String fullCaseNumber = caseNumberGenerator.generate(
        caseType.getNumberFormatTemplate(),
        year,
        tribunal.getCode(),
        caseType.getCode(),
        sequenceNumber
    );

    // 4. Build and save case
    Case case = Case.builder()
        .year(year)
        .sequenceNumber(sequenceNumber)
        .fullCaseNumber(fullCaseNumber)
        .registrationDate(request.registrationDate())
        .caseDescription(request.caseDescription())
        .matterDescription(request.matterDescription())
        .tribunal(tribunal)
        .caseType(caseType)
        .lawyer(lawyer)
        .status(initialStatus)
        .build();

    case = caseRepository.save(case);

    // 5. Publish audit event
    auditPublisher.publishCaseCreated(case, currentUser);

    return caseMapper.toResponse(case);
}
```

**2. Update Case:**
```java
@Transactional
public CaseResponse updateCase(Long id, UpdateCaseRequest request, UserPrincipal currentUser) {
    Case case = caseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Case", "id", id));

    // Track changes for audit
    Map<String, Object> changes = new HashMap<>();

    // Update mutable fields only
    if (request.tribunalCode() != null) {
        Tribunal tribunal = tribunalRepository.findByCodeAndActiveTrue(request.tribunalCode())
            .orElseThrow(() -> new ResourceNotFoundException("Tribunal", "code", request.tribunalCode()));
        changes.put("tribunal", Map.of("old", case.getTribunal().getCode(), "new", tribunal.getCode()));
        case.setTribunal(tribunal);
    }

    if (request.lawyerId() != null) {
        Lawyer lawyer = lawyerRepository.findByIdAndActiveTrue(request.lawyerId())
            .orElseThrow(() -> new ResourceNotFoundException("Lawyer", "id", request.lawyerId()));
        changes.put("lawyer", Map.of("old", case.getLawyer().getId(), "new", lawyer.getId()));
        case.setLawyer(lawyer);
    }

    // Update other fields...

    case = caseRepository.save(case);
    auditPublisher.publishCaseUpdated(case, changes, currentUser);

    return caseMapper.toResponse(case);
}
```

**3. Change Status:**
```java
@Transactional
public CaseResponse changeStatus(Long id, ChangeStatusRequest request, UserPrincipal currentUser) {
    Case case = caseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Case", "id", id));

    CaseStatus newStatus = caseStatusRepository.findByCode(request.statusCode())
        .orElseThrow(() -> new ResourceNotFoundException("CaseStatus", "code", request.statusCode()));

    // Validate status is allowed for this case type
    if (!case.getCaseType().getAllowedStatuses().contains(newStatus)) {
        throw new InvalidStatusTransitionException(
            "Status " + newStatus.getCode() + " is not allowed for case type " + case.getCaseType().getCode()
        );
    }

    CaseStatus oldStatus = case.getStatus();
    case.setStatus(newStatus);

    case = caseRepository.save(case);
    auditPublisher.publishStatusChanged(case, oldStatus, newStatus, request.reason(), currentUser);

    return caseMapper.toResponse(case);
}
```

**4. Search Cases:**
```java
@Transactional(readOnly = true)
public Page<CaseSummary> searchCases(CaseSearchRequest request) {
    Specification<Case> spec = CaseSpecification.builder()
        .year(request.year())
        .caseTypeCode(request.caseTypeCode())
        .tribunalCode(request.tribunalCode())
        .lawyerId(request.lawyerId())
        .statusCode(request.statusCode())
        .registrationDateFrom(request.registrationDateFrom())
        .registrationDateTo(request.registrationDateTo())
        .notDeleted()
        .build();

    Pageable pageable = PageRequest.of(
        request.page(),
        request.size(),
        Sort.by(Sort.Direction.fromString(request.sortDirection()), request.sortBy())
    );

    Page<Case> cases = caseRepository.findAll(spec, pageable);
    return cases.map(caseMapper::toSummary);
}
```

### CaseSequenceService

**Atomic Sequence Generation:**
```java
@Service
public class CaseSequenceService {

    @Transactional
    public synchronized int getNextSequence(int year, String caseTypeCode) {
        CaseSequence sequence = caseSequenceRepository
            .findByYearAndCaseTypeCode(year, caseTypeCode)
            .orElseGet(() -> {
                CaseSequence newSeq = new CaseSequence();
                newSeq.setYear(year);
                newSeq.setCaseTypeCode(caseTypeCode);
                newSeq.setLastSequence(0);
                return caseSequenceRepository.save(newSeq);
            });

        int nextSequence = sequence.getLastSequence() + 1;
        sequence.setLastSequence(nextSequence);
        caseSequenceRepository.save(sequence);

        return nextSequence;
    }
}
```

**Alternative with SELECT FOR UPDATE:**
```java
@Query("SELECT cs FROM CaseSequence cs WHERE cs.year = :year AND cs.caseTypeCode = :caseTypeCode FOR UPDATE")
Optional<CaseSequence> findByYearAndCaseTypeCodeForUpdate(@Param("year") int year, @Param("caseTypeCode") String caseTypeCode);
```

### CaseNumberGenerator

**Template Parsing:**
```java
@Service
public class CaseNumberGenerator {

    public String generate(String template, int year, String tribunalCode, String caseTypeCode, int sequence) {
        String result = template;

        result = result.replace("{YEAR}", String.valueOf(year));
        result = result.replace("{TRIBUNAL_CODE}", tribunalCode);
        result = result.replace("{CASETYPE}", caseTypeCode);
        result = result.replace("{SEQ5}", String.format("%05d", sequence));

        // Validate no unreplaced placeholders
        if (result.contains("{") || result.contains("}")) {
            throw new InvalidCaseNumberFormatException("Invalid template: " + template);
        }

        return result;
    }

    public String preview(String template, String tribunalCode, String caseTypeCode) {
        // Preview with example values
        return generate(template, Year.now().getValue(), tribunalCode, caseTypeCode, 1);
    }
}
```

### Supporting Services

**TribunalService:**
- `findAll()` - List all active tribunals
- `findByCode(String code)` - Get single tribunal
- `createTribunal(TribunalRequest)` - Admin only
- `updateTribunal(Long id, TribunalRequest)` - Admin only
- `deactivateTribunal(Long id)` - Admin only (soft delete)

**CaseTypeService:**
- `findAll()` - List all active case types
- `findByCode(String code)` - Get single case type
- `updateNumberFormatTemplate(String code, String template)` - Admin only
- `updateAllowedStatuses(String code, List<String> statusCodes)` - Admin only

**LawyerService:**
- `findAll()` - List all active lawyers
- `findById(Long id)` - Get single lawyer
- `createLawyer(LawyerRequest)` - Create lawyer
- `updateLawyer(Long id, LawyerRequest)` - Update lawyer
- `deactivateLawyer(Long id)` - Deactivate lawyer
- `getCaseCount(Long lawyerId)` - Count active cases for lawyer (workload)

**FinancialTransactionService:**
- `createTransaction(Long caseId, TransactionRequest)` - Add payment/expense
- `getTransactionsByCase(Long caseId)` - List all transactions for case
- `calculateSummary(Long caseId)` - Calculate financial summary

---

## Database Schema

### Flyway Migrations

**V17__create_tribunals_table.sql:**
```sql
CREATE TABLE tribunals (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name_fr VARCHAR(255) NOT NULL,
    name_ar VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_tribunals_code ON tribunals(code);
CREATE INDEX idx_tribunals_active ON tribunals(active);
```

**V18__seed_tribunals.sql:**
```sql
-- Administrative Appeal Courts
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_ADM_APPL_1', 'Tribunal d''appel administratif de Rabat', 'محكمة الاستئناف الإدارية بالرباط'),
('TR_ADM_APPL_2', 'Tribunal d''appel administratif de Marrakech', 'محكمة الإستئناف الإدارية بمراكش');

-- Administrative First Instance Courts
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_ADM_PIN_1', 'Tribunal administratif de Rabat', 'المحكمة الإدارية بالرباط'),
('TR_ADM_PIN_2', 'Tribunal administratif de Casablanca', 'المحكمة الإدارية بالدار البيضاء'),
('TR_ADM_PIN_3', 'Tribunal administratif de Fes', 'المحكمة الإدارية بفاس'),
('TR_ADM_PIN_4', 'Tribunal administratif de Meknes', 'المحكمة الإدارية بمكناس'),
('TR_ADM_PIN_5', 'Tribunal administratif de Oujda', 'المحكمة الإدارية بوجدة'),
('TR_ADM_PIN_6', 'Tribunal administratif de Marrakech', 'المحكمة الإدارية بمراكش'),
('TR_ADM_PIN_7', 'Tribunal administratif de Agadir', 'المحكمة الإدارية بأكادير');

-- Commercial Appeal Courts
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_COM_APPL_1', 'Tribunal d''appel commercial de Casablanca', 'محكمة الاستئناف التجارية - الدار البيضاء'),
('TR_COM_APPL_2', 'Tribunal d''appel commercial de Marrakech', 'محكمة الاستئناف التجارية - مراكش'),
('TR_COM_APPL_3', 'Tribunal d''appel commercial de Fes', 'محكمة الاستئناف التجارية - فاس');

-- Commercial First Instance Courts
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_COM_PIN_1', 'Tribunal commercial de Rabat', 'المحكمة التجارية - الرباط'),
('TR_COM_PIN_2', 'Tribunal commercial de Casablanca', 'المحكمة التجارية - الدار البيضاء'),
('TR_COM_PIN_3', 'Tribunal commercial de Agadir', 'المحكمة التجارية - أكادير'),
('TR_COM_PIN_4', 'Tribunal commercial de Marrakech', 'المحكمة التجارية مراكش'),
('TR_COM_PIN_5', 'Tribunal commercial de Fes', 'المحكمة التجارية بفاس'),
('TR_COM_PIN_6', 'Tribunal commercial de Meknes', 'المحكمة التجارية بمكناس'),
('TR_COM_PIN_7', 'Tribunal commercial de Oujda', 'المحكمة التجارية بوجدة'),
('TR_COM_PIN_8', 'Tribunal commercial de Tanger', 'المحكمة التجارية بطنجة');

-- Appeal Courts (22 courts)
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_APPL_1', 'Tribunal d''appel de Rabat', 'محكمة الاستئناف - بالرباط'),
('TR_APPL_2', 'Tribunal d''appel de Laayoune', 'محكمة الاستئناف العيون'),
('TR_APPL_3', 'Tribunal d''appel de Agadir', 'محكمة الاستئناف أكادير'),
('TR_APPL_4', 'Tribunal d''appel de Ouarzazate', 'محكمة الاستئناف ورزازات'),
('TR_APPL_5', 'Tribunal d''appel de Kenitra', 'محكمة الاستئناف القنيطرة'),
('TR_APPL_6', 'Tribunal d''appel de Settat', 'محكمة الاستئناف سطات'),
('TR_APPL_7', 'Tribunal d''appel de Khouribga', 'محكمة الاستئناف - خريبكة'),
('TR_APPL_8', 'Tribunal d''appel de Casablanca', 'محكمة الاستئناف - الدر البيضاء'),
('TR_APPL_9', 'Tribunal d''appel de Marrakech', 'محكمة الاستئناف - مراكش'),
('TR_APPL_10', 'Tribunal d''appel de Safi', 'محكمة الاستئناف - آسفي'),
('TR_APPL_11', 'Tribunal d''appel de Tanger', 'محكمة الاستئناف - طنجة'),
('TR_APPL_12', 'Tribunal d''appel de Tetouan', 'محكمة الاستئناف - تطوان'),
('TR_APPL_13', 'Tribunal d''appel de Al Hoceima', 'محكمة الاستئناف - الحسيمة'),
('TR_APPL_14', 'Tribunal d''appel de Taza', 'محكمة الاستئناف - تازة'),
('TR_APPL_15', 'Tribunal d''appel de Oujda', 'محكمة الاستئناف - وجدة'),
('TR_APPL_16', 'Tribunal d''appel de Meknes', 'محكمة الاستئناف - مكناس'),
('TR_APPL_17', 'Tribunal d''appel de El Jadida', 'محكمة الاستئناف - الجديدة'),
('TR_APPL_18', 'Tribunal d''appel de Beni Mellal', 'محكمة الاستئناف - بني ملال'),
('TR_APPL_19', 'Tribunal d''appel de Errachidia', 'محكمة الاستئناف - الرشيدية'),
('TR_APPL_20', 'Tribunal d''appel de Nador', 'محكمة الاستئناف - الناظور'),
('TR_APPL_21', 'Tribunal d''appel de Fes', 'محكمة الاستئناف - فاس'),
('TR_APPL_22', 'Tribunal d''appel de Guelmim', 'محكمة الاستئناف - كلميم');

-- First Instance Courts (80+ courts - sample shown)
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_PIN_1', 'Tribunal de 1ère instance de Rabat', 'المحكمة الابتدائية - الرباط'),
('TR_PIN_2', 'Tribunal de 1ère instance de Salé', 'المحكمة الابتدائية سلا'),
('TR_PIN_3', 'Tribunal de 1ère instance de Temara', 'المحكمة الابتدائية - تمارة'),
('TR_PIN_31', 'Tribunal de 1ère instance civile de Casablanca', 'المحكمة الابتدائية المدنية بالدار البيضاء'),
('TR_PIN_37', 'Tribunal de 1ère instance de Marrakech', 'المحكمة الابتدائية - مراكش'),
('TR_PIN_44', 'Tribunal de 1ère instance de Tanger', 'المحكمة الابتدائية - طنجة'),
('TR_PIN_76', 'Tribunal de 1ère instance de Fes', 'المحكمة الابتدائية - فاس');
-- ... (continue for all 83 first instance courts)

-- Court of Cassation
INSERT INTO tribunals (code, name_fr, name_ar) VALUES
('TR_CASS_1', 'Cour de cassation de Rabat', 'محكمة النقض بالرباط');
```

**V19__create_case_types_table.sql:**
```sql
CREATE TABLE case_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name_fr VARCHAR(100) NOT NULL,
    name_ar VARCHAR(100),
    number_format_template VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_case_types_code ON case_types(code);
CREATE INDEX idx_case_types_active ON case_types(active);
```

**V20__seed_case_types.sql:**
```sql
INSERT INTO case_types (code, name_fr, name_ar, number_format_template) VALUES
('PENAL', 'Pénale', 'جنائي', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}'),
('COMMERC', 'Commerciale', 'تجاري', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}'),
('CIVIL', 'Civile', 'مدني', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}'),
('ADM', 'Administrative', 'إداري', '{YEAR}-{TRIBUNAL_CODE}-{CASETYPE}-{SEQ5}');
```

**V21__create_case_categories_table.sql:**
```sql
CREATE TABLE case_categories (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name_ar VARCHAR(255) NOT NULL,
    name_fr VARCHAR(255),
    case_type_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (case_type_id) REFERENCES case_types(id)
);

CREATE INDEX idx_case_categories_code ON case_categories(code);
CREATE INDEX idx_case_categories_case_type ON case_categories(case_type_id);
CREATE INDEX idx_case_categories_active ON case_categories(active);
```

**V22__seed_case_categories.sql:**
```sql
-- Administrative Court Categories (7xxx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7101', 'القضايا الاستعجالية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7102', 'الأوامر المبنية على طلب', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7103', 'المصادقة على الحجز', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7104', 'المعاشات المدنية والعسكرية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7105', 'الوضعية الفردية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7106', 'إيقاف التنفيذ', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7107', 'المنازعات الإنتخابية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7108', 'نزع الملكية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7109', 'تحصيل ديون الخزينة', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7110', 'دعوى الإلغاء', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7111', 'فحص الشرعية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7112', 'المسؤولية الإدارية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7113', 'المنازعات الضريبية في الموضوع', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7114', 'العقود الإدارية والصفقات', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7115', 'المختلفة', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7116', 'المساعدة القضائية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7129', 'الوضعية الفردية المادة 11 من ظ 10 - 09 - 1993', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7130', 'المنازعات المادة 11 من ظ 10 - 09 - 1993', id FROM case_types WHERE code = 'ADM';

-- Administrative Appeal Categories (72xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7201', 'الاستعجالي', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7202', 'الاستعجالي )مستأنف)', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7203', 'غرفة المشورة', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7204', 'المساعدة القضائية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7205', 'قضاء الإلغاء', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7206', 'المسؤولية الإدارية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7207', 'العقود الإدارية والصفقات', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7208', 'الوضعية الفردية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7209', 'المنازعات الضريبية في الموضوع', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7210', 'المعاشات المدنية والعسكرية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7211', 'نزع الملكية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7212', 'المنازعات الإنتخابية', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7213', 'تحصيل ديون الخزينة', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7214', 'إيقاف التنفيذ', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7215', 'فحص الشرعية', id FROM case_types WHERE code = 'ADM';

-- Administrative Notification/Service (73xx-75xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7301', 'التبليغ', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7302', 'الإنابات الواردة تبليغ', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7401', 'التنفيذ', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7402', 'الإنابات الواردة تنفيذ', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7501', 'التبليغ بناء على طلب', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7502', 'تبليغ تلقائي', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7503', 'لإنابات الواردة تبليغ', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7504', 'التبليغ التلقائي ""الحيازة""', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7507', 'التبليغ التلقائي ""الإنتخابات""', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7508', 'التبليغ التلقائي ""نزع الملكية""', id FROM case_types WHERE code = 'ADM';

-- Administrative Execution (76xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7601', 'التنفيذ', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7602', 'الإنابات الواردة تنفيذ', id FROM case_types WHERE code = 'ADM';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '7603', 'الإنابات الصادرة تنفيذ', id FROM case_types WHERE code = 'ADM';

-- Commercial Court Categories (81xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8101', 'الاستعجالي', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8102', 'الأمر بالأداء', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8103', 'الأوامر المبنية على طلب', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8104', 'استرجاع السيارات', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8105', 'الحجز لدى الغير', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8106', 'الحجز التحفظي', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8107', 'رفع الحجز', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8108', 'مسطرة التوفيق ""ظهير 24 ماي 1955""', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8109', 'إيقاف التنفيذ', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8110', 'صعوبة التنفيذ', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8111', 'التوزيع الودي', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8112', 'التسوية الودية', id FROM case_types WHERE code = 'COMMERC';

-- Commercial Substance Cases (82xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8201', 'العقود التجارية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8202', 'المنازعات بين التجار بشأن أعمال تجارية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8203', 'الأوراق التجارية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8204', 'منازعات شركاء في شركة تجارية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8205', 'الأصول التجارية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8206', 'منازعات ظهير 24 ماي 1955', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8207', 'أكرية المحلات التجارية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8208', 'تصحيح الحجز', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8209', 'معاملات مؤسسات التمويل', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8210', 'المعاملات البنكية والقروض', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8211', 'المنافسة غير المشروعة والتقليد', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8212', 'مشورة متنوعة', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8213', 'التنفيذ', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8214', 'منازعات السجل التجاري', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8215', 'غرامات السجل التجاري', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8216', 'التعرض على أمر بالأداء', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8217', 'إيقاف تنفيذ أمر بالأداء', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8218', 'قضايا التأمين', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8220', 'المسؤولية البنكية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8221', 'المعاملات البنكية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8222', 'لقروض البنكية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8223', 'الأوامر بالأداء', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8224', 'الأوامر المبنية على طلب', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8225', 'الأوامر الاستعجالية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8226', 'الحجز لدى الغير', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8227', 'الاختصاص', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8228', 'المنازعات الناشئة بين الشركات التجارية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8229', 'الطعن في مقررات مكتب الملكية الصناعية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8230', 'الطعن بالبطلان في الأحكام التحكيمية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8231', 'إصلاح خطأ مادي', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8232', 'قضايا أخرى', id FROM case_types WHERE code = 'COMMERC';

-- Company Difficulties (83xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8301', 'صعوبات المقاولة', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8302', 'التسوية القضائية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8303', 'التصفية القضائية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8304', 'الطلبات الأخرى المعروضة على القاضي المنتدب', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8305', 'تمديد فترة إعداد الحل', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8306', 'إعداد الحل', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8307', 'تحديد الأتعاب', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8308', 'فسخ المخطط', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8309', 'متابعة تنفيذ المخطط', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8310', 'العقوبات', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8311', 'الإستبدال', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8312', 'قفل المسطرة', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8313', 'طلبات تحقيق الدين المعروضة على القاضي المنتدب', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8314', 'تمدبد فترة الاستغلال', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8315', 'مسطرة الإنقاذ', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8316', 'حصر مخطط الإستمرارية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8317', 'اختيار عرض التفويت', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8318', 'تحويل التسوية إلى تصفية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8319', 'طلبات أخرى في صعوبات المقاولة', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8320', 'تمديد التسوية القضائية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8321', 'تمديد التصفية القضائية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8322', 'قفل مسطرة التسوية القضائية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8323', 'قفل مسطرة التصفية القضائية', id FROM case_types WHERE code = 'COMMERC';

-- Commercial Notifications (84xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8401', 'تبليغ الأحكام و الأوامر و القرارات محلي', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8402', 'تبليغ الأحكام و الأوامر و القرارات إنابة', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8403', 'تبليغ أوامر القاضي المنتدب محلي', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8404', 'تبليغ أوامر القاضي المنتدب إنابة', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8405', 'تبليغ أحكام غرفة المشورة', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8406', 'تبليغ تلقائي', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8407', 'تبليغات أخرى', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8408', 'تبليغ الأوامر الاستعجالية', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8409', 'تبليغ الأوامر بالأداء', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8410', 'تبليغ الإنذارات', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8411', 'تبليغ أحكام الموضوع', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8412', 'تبليغ أحكام صعوبات المقاولة', id FROM case_types WHERE code = 'COMMERC';

-- Commercial Executions (85xx) - Local
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8501', 'الأداءات (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8502', 'الإفراغات (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8503', 'الإسترجاعات (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8504', 'حجز تحفظي على منقول (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8505', 'حجز تحفظي على أصل تجاري (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8506', 'حجز تحفظي على باخرة (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8507', 'حجز تحفظي على عقار (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8508', 'حجز تنفيذي على منقول (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8509', 'حجز تنفيذي على أصل تجاري (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8510', 'حجز تنفيذي على باخرة (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8511', 'حجز تنفيذي على عقار (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8512', 'تحقيق الرهن على أصل تجاري (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8513', 'تحقيق الرهن على عقار (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8514', 'حجز لدى الغير (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8515', 'البيوعات (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8516', 'البيوعات المتعلقة بصعوبات المقاولة (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8517', 'رفع الحجز (محلي)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8519', 'تنفيذات أخرى (محلي)', id FROM case_types WHERE code = 'COMMERC';

-- Commercial Executions (85xx) - Delegations
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8521', 'الأداءات (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8522', 'الإفراغات (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8523', 'الإسترجاعات (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8524', 'حجز تحفظي على منقول (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8525', 'حجز تحفظي على أصل تجاري (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8526', 'حجز تحفظي على باخرة (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8527', 'حجز تحفظي على عقار (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8528', 'حجز تنفيذي على منقول (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8529', 'حجز تنفيذي على أصل تجاري (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8530', 'حجز تنفيذي على باخرة (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8531', 'حجز تنفيذي على عقار (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8532', 'تحقيق الرهن على أصل تجاري (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8533', 'تحقيق الرهن على عقار (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8534', 'حجز لدى الغير (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8535', 'البيوعات (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8536', 'البيوعات المتعلقة بصعوبات المقاولة (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8537', 'رفع الحجز (إنابة)', id FROM case_types WHERE code = 'COMMERC';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '8538', 'تنفيذات أخرى (إنابة)', id FROM case_types WHERE code = 'COMMERC';

-- Civil Court Categories (11xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1101', 'الاستعجالي', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1102', 'الأمر بالأداء', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1103', 'الحجز التحفظي', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1104', 'الحجز لدى الغير', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1105', 'التقييد الاحتياطي', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1106', 'الإنذار العقاري', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1107', 'الإنذار بأداء الكراء', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1108', 'الإحالة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1109', 'باقي الأوامر المبنية على طلب', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1110', 'تبليغ إنذار ظ.ه. 64 - 99', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1111', 'المصادقة على الإنذار ظ.ه. 64 - 99', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1112', 'المصادقة على الحجز', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1113', 'الإلغاء ق ق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1114', 'طلبات الإستفادة من صندوق التكافل العائلي', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1115', 'محاولة الصلح ""ظ 24 ماي 1955 ""', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1120', 'الطعن ضد مقررات اتعاب هيئة المحامين', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1121', 'صعوبة التنفيذ', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1122', 'اداء اليميين المهنية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1123', 'إيقاف التنفيذ', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1124', 'الطعون ضد مقررات مجلس هيئة المحامين و نزاعات المحامين', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1125', 'مخالفات العدول المهنية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1126', 'مخالفات المفوضين القضائين المهنية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1127', 'مخالفات الموثقين المهنية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1128', 'تجريح القضاة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1154', 'المصادقة على الحجز لدى الغير', id FROM case_types WHERE code = 'CIVIL';

-- Civil Substance Cases (12xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1201', 'المدني المتنوع', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1202', 'المسؤولية التقصيرية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1203', 'التجاري', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1204', 'الإداري', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1205', 'المدني النهائي', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1206', 'المدني مقاطعات', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1207', 'تذييل بالصيغة التنفيذية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1208', 'المسطرة التأديبية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1209', 'قضايا المشورة مدني', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1210', 'مدني عبري', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1211', 'المنازعات الإنتخابية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1220', 'الأوامر بالأداء', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1221', 'القضايا الاستعجالية', id FROM case_types WHERE code = 'CIVIL';

-- Civil Appeals (125x)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1251', 'المدني المتنوع المستأنف', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1252', 'المسؤولية التقصيرية المستأنفة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1253', 'التجاري المستأنف', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1254', 'الإداري المستأنف', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1255', 'المدني النهائي المستأنف', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1256', 'المدني مقاطعات المستأنف', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1257', 'غرفة المشورة للقضايا المدنية المستأنفة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1258', 'مدني عبري المستأنف', id FROM case_types WHERE code = 'CIVIL';

-- Rent Cases (13xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1301', 'أداء واجبات الكراء', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1302', 'الإفراغ', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1303', 'الأداء والإفراغ', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1304', 'مراجعة السومة الكرائية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1305', 'قضايا المشورة أكرية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1351', 'أداء واجبات الكراء المستأنف', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1352', 'مراجعة السومة الكرائية المستأنفة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1353', 'غرفة المشورة لقضايا الأكرية المستأنفة', id FROM case_types WHERE code = 'CIVIL';

-- Real Estate Cases (14xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1401', 'الاستعجالي', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1402', 'الاستعجالي', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1403', 'العقار في طور التحفيظ', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1404', 'القضايا العقارية العينية المختلطة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1405', 'قضايا المشورة عقار', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1406', 'العقار العبري', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1451', 'العقار العادي المستأنف', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1452', 'العقار المحفظ المستأنف', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1453', 'العقار في طور التحفيظ المستأنف', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1454', 'القضايا العقارية العينية المختلطة المستأنف', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1455', 'قضايا المشورة عقار المستأنف', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1456', 'العقار العبري المستأنف', id FROM case_types WHERE code = 'CIVIL';

-- Labor Cases (15xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1501', 'نزاعات الشغل', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1502', 'حوادث الشغل', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1503', 'الأمراض المهنية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1504', 'قضايا المشورة نزاعات الشغل', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1505', 'قضايا المشورة حوادث الشغل', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1551', 'نزاعات الشغل المستأنفة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1552', 'حوادث الشغل المستأنفة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1553', 'الأمراض المهنية المستأنفة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1554', 'غرفة المشورة لقضايا نزاعات الشغل المستأنفة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1555', 'غرفة المشورة لقضايا حوادث الشغل المستأنفة', id FROM case_types WHERE code = 'CIVIL';

-- Family Law Cases (16xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1601', 'الإصلاح والتغيير', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1602', 'تسجيل الولادة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1603', 'تسجيل الوفاة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1604', 'إضافة بيانات', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1605', 'الزواج عادي', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1606', 'النفقة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1607', 'التطليق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1608', 'الطلاق ) الإذن بالطلاق + المقرر النهائي)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1609', 'الحضانة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1610', 'الرجوع لبيت الزوجية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1611', 'ثبوت الزوجية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1612', 'صلة الرحم', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1613', 'النسب', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1614', 'التذييل بالصيغة التنفيذية)أسرة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1615', 'الميراث', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1616', 'زواج القاصرين', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1617', 'كفالة الأطفال المهملين', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1618', 'الإذن بالتعدد', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1619', 'النيابة الشرعية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1620', 'قضايا الأحوال الشخصية الأخرى', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1621', 'غرفة المشورة لقضايا الأسرة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1622', 'مراجعة لوازم الطلاق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1623', 'التحجير', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1624', 'الزواج المختلط', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1625', 'التصريح بالإهمال', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1626', 'التطليق للشقاق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1627', 'التطليق للضرر', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1628', 'التطليق لعدم الإنفاق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1629', 'التطليق للغيبة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1630', 'التطليق للعيب', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1631', 'التطليق بسبب الإيلاء والهجر', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1632', 'التطليق لإخلال الزوج بشرط من شروط العقد', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1633', 'الطلاق قبل البناء', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1634', 'الطلاق المملك', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1635', 'الطلاق الإتفاقي', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1636', 'الطلاق بالخلع', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1637', 'فسخ عقد زواج', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1638', 'بطلان الزواج', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1639', 'الأحوال الشخصية العبرية المغربية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1640', 'الأحوال الشخصية للأجانب', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1641', 'دعاوى الجنسية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1642', 'كفالة الأطفال غير المهملين', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1643', 'النيابة القانونية', id FROM case_types WHERE code = 'CIVIL';

-- Family Law Appeals (165x)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1651', 'النفقة المستأنفة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1652', 'غرفة المشورة لقضايا الأسرة المستأنفة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1653', 'مراجعة لوازم الطلاق المستأنفة', id FROM case_types WHERE code = 'CIVIL';

-- Proximity Justice (17xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1701', 'طلب الأداء ق ق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1702', 'الحجز لدى الغير ق ق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1703', 'الإنذار بأداء الكراء ق ق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1704', 'تبليغ إنذار ظ. ه. 64 - 99 ق ق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1705', 'المصادقة على الإنذار ظ. ه. 64 - 99 ق ق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1706', 'المصادقة على الحجز لدى الغير ق ق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1707', 'المدني المتنوع ق ق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1708', 'المسؤولية التقصيرية ق ق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1709', 'التجاري ق ق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1710', 'اداء واجبات الكراء ق ق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '1711', 'مراجعة السومة الكرائية ق ق', id FROM case_types WHERE code = 'CIVIL';

-- Criminal Cases (21xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2101', 'جنحي عادي تأديبي', id FROM case_types WHERE code = 'PENAL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2102', 'جنحي عادي ضبطي', id FROM case_types WHERE code = 'PENAL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2103', 'جنحي تلبسي اعتقال', id FROM case_types WHERE code = 'PENAL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2104', 'جنحي تلبسي سراح', id FROM case_types WHERE code = 'PENAL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2105', 'جنحي ضبطي اعتقال', id FROM case_types WHERE code = 'PENAL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2106', 'جنحي ضبطي سراح', id FROM case_types WHERE code = 'PENAL';

-- Criminal Appeals (28xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2801', 'جنحي عادي استئنافي', id FROM case_types WHERE code = 'PENAL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2802', 'جنحي تلبسي استئنافي اعتقال', id FROM case_types WHERE code = 'PENAL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '2803', 'جنحي تلبسي استئنافي سراح', id FROM case_types WHERE code = 'PENAL';

-- Execution/Enforcement Cases (61xx-68xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6101', 'البيوعات العقارية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6102', 'الحجز التحفظي على عقار', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6103', 'بيع الأصول التجارية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6104', 'التصفية القضائية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6105', 'الانذارات العقارية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6106', 'التركات الشاغرة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6107', 'الحجز التنفيذي على عقار', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6108', 'تحقيق الرهن على عقار', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6109', 'الحجز التحفظي على أصل تجاري', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6110', 'تحقيق الرهن على أصل تجاري', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6111', 'الحجز التحفظي على سفينة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6112', 'الحجز االتنفيذي على سفينة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6150', 'تنفيذ الأمر بالأداء', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6151', 'تنفيذ الأوامر المختلفة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6152', 'تنفيذ الأوامر الإستعجالية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6153', 'مختلف تنفيذات مؤسسة الرئيس', id FROM case_types WHERE code = 'CIVIL';

-- Civil Executions (62xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6201', 'مختلف قضايا التنفيذ المدني', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6202', 'الحجز التحفظي على منقول', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6203', 'الحجز لدى الغير', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6204', 'الإفراغات', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6205', 'الحجز التنفيذي على منقول', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6206', 'تنفيذ الأداءات', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6207', 'تنفيذ الإسترجاعات', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6208', 'تنفيذات المسؤولية المدنية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6209', 'تنفيذات حوادث السير الدعوى المدنية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6210', 'تنفيذات منازعات الشغل', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6211', 'تنفيذات حوادث الشغل', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6212', 'تنفيذ أحكام قضاء القرب', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6213', 'التنفيذ التلقائي', id FROM case_types WHERE code = 'CIVIL';

-- Family Executions (625x)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6250', 'تنفيذ النفقة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6251', 'تنفيذ الرجوع لبيت الزوجية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6252', 'تنفيذ مستحقات الطلاق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6253', 'تنفيذ مستحقات التطليق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6254', 'تنفيذ الأحوال الشخصية المختلفة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6255', 'تنفيذات أخرى قضاء الأسرة', id FROM case_types WHERE code = 'CIVIL';

-- Delegations (63xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6301', 'الانابات الصادرة عقار', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6302', 'الانابات الواردة عقار', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6303', 'الانابات الصادرة تنفيذ مدني', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6304', 'الانابات الواردة تنفيذ مدني', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6305', 'تنفيذ الأداءات (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6306', 'تنفيذ الإسترجاعات (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6307', 'تنفيذات المسؤولية المدنية (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6308', 'تنفيذات حوادث السير الدعوى المدنية (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6309', 'تنفيذات منازعات الشغل (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6310', 'تنفيذات حوادث الشغل الإنابات الواردة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6311', 'تنفيذ أحكام قضاء القرب (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6312', 'التنفيذ التلقائي (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6350', 'تنفيذ النفقة (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6351', 'تنفيذ الرجوع لبيت الزوجية (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6352', 'تنفيذ مستحقات الطلاق (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6353', 'تنفيذ مستحقات التطليق (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6354', 'تنفيذ الأحوال الشخصية المختلفة (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6355', 'تنفيذات أخرى قضاء الأسرة (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';

-- Notifications (65xx-68xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6501', 'تبليغات الإنذارات', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6502', 'تبليغات الأوامر بالأداء', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6503', 'تبليغ ملفات أوامر الرئيس (التبليغ)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6504', 'تبليغات الأوامر الإستعجالية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6505', 'تبليغات الحجز لدى الغير', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6506', 'تبليغات الإلغاء ""مدني""', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6507', 'مختلف تبيلغات مؤسسة الرئيس', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6601', 'تبليغات النفقة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6602', 'تبليغات الطلاق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6603', 'تبليغات التطليق', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6604', 'تبليغات الأحوال الشخصية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6605', 'تبليغ تلقائي أسرة', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6606', 'تبليغ ملفات قضايا الأسرة (التبليغ)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6701', 'تبليغات المسؤولية التقصيرية', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6702', 'تبليغات الدعوى المدنية حوادث السير', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6703', 'تبليغ ملفات قضاياالأكرية (التبليغ)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6704', 'تبليغ ملفات قضايا العقار (التبليغ)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6705', 'تبليغ ملفات القضايا الإجتماعية )التبليغ)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6706', 'تبليغ تلقائي مدني', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6707', 'تبليغات قضاء القرب ""مدني""', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6708', 'التبليغ (التبليغ المدني)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6709', 'تبليغات العقار في طور التحفيظ', id FROM case_types WHERE code = 'CIVIL';

-- Incoming Delegations Notifications (68xx)
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6801', 'تبليغات الحجز لدى الغير (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6802', 'تبليغات الإلغاء ""مدني"" (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6803', 'مختلف تبيلغات مؤسسة الرئيس (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6804', 'تبليغات المسؤولية التقصيرية (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6805', 'تبليغات الدعوى المدنية حوادث السير (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6806', 'تبليغات الأكرية (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6807', 'تبليغات العقار (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6808', 'تبليغات الإجتماعي (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6809', 'تبليغ تلقائي مدني (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6810', 'تبليغات قضاء القرب ""مدني"" (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6811', 'مختلف التبليغات المدنية (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6820', 'تبليغات النفقة (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6821', 'تبليغات الطلاق (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6822', 'تبليغات التطليق (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6823', 'تبليغات الأحوال الشخصية (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6824', 'تبليغ تلقائي أسرة (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';
INSERT INTO case_categories (code, name_ar, case_type_id)
SELECT '6825', 'مختلف تبليغات الأسرة (إنابات واردة)', id FROM case_types WHERE code = 'CIVIL';

-- Court of Cassation codes
INSERT INTO case_categories (code, name_ar) VALUES
('1', 'رمز مدني لمحكمة النقض'),
('2', 'الغرفة الإدارية'),
('3', 'رمز تجاري لمحكمة النقض'),
('4', 'الرمز الإداري لمحكمة النقد'),
('6', 'رمز جنائي لمحكمة النقض');
```

**Note:** This is a comprehensive list of 300+ case categories. The migration file should include all categories provided by the user.

**V23__create_case_statuses_table.sql:**
```sql
CREATE TABLE case_statuses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name_fr VARCHAR(100) NOT NULL,
    name_ar VARCHAR(100),
    sort_order INT NOT NULL DEFAULT 0,
    is_terminal BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_case_statuses_code ON case_statuses(code);
CREATE INDEX idx_case_statuses_sort_order ON case_statuses(sort_order);
```

**V24__seed_case_statuses.sql:**
```sql
INSERT INTO case_statuses (code, name_fr, name_ar, sort_order, is_terminal) VALUES
('DRAFT', 'Brouillon', 'مسودة', 1, false),
('OPEN', 'Ouvert', 'مفتوح', 2, false),
('IN_PROGRESS', 'En cours', 'قيد التقدم', 3, false),
('HEARING', 'Audience', 'جلسة', 4, false),
('JUDGMENT', 'Jugement', 'حكم', 5, false),
('CLOSED', 'Clôturé', 'مغلق', 6, true),
('ARCHIVED', 'Archivé', 'مؤرشف', 7, true);
```

**V25__create_case_type_statuses_table.sql:**
```sql
CREATE TABLE case_type_statuses (
    case_type_id BIGINT NOT NULL,
    status_id BIGINT NOT NULL,
    PRIMARY KEY (case_type_id, status_id),
    FOREIGN KEY (case_type_id) REFERENCES case_types(id) ON DELETE CASCADE,
    FOREIGN KEY (status_id) REFERENCES case_statuses(id) ON DELETE CASCADE
);

CREATE INDEX idx_case_type_statuses_case_type ON case_type_statuses(case_type_id);
CREATE INDEX idx_case_type_statuses_status ON case_type_statuses(status_id);
```

**V26__seed_case_type_statuses.sql:**
```sql
-- Assign all statuses to all case types initially
INSERT INTO case_type_statuses (case_type_id, status_id)
SELECT ct.id, cs.id
FROM case_types ct
CROSS JOIN case_statuses cs;
```

**V27__create_lawyers_table.sql:**
```sql
CREATE TABLE lawyers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    tax_id VARCHAR(50) UNIQUE,
    email VARCHAR(100),
    phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_lawyers_tax_id ON lawyers(tax_id);
CREATE INDEX idx_lawyers_active ON lawyers(active);
CREATE INDEX idx_lawyers_full_name ON lawyers(first_name, last_name);
```

**V28__create_case_sequences_table.sql:**
```sql
CREATE TABLE case_sequences (
    id BIGSERIAL PRIMARY KEY,
    year INT NOT NULL,
    case_type_code VARCHAR(20) NOT NULL,
    last_sequence INT NOT NULL DEFAULT 0,
    UNIQUE(year, case_type_code)
);

CREATE INDEX idx_case_sequences_year_type ON case_sequences(year, case_type_code);
```

**V29__create_cases_table.sql:**
```sql
CREATE TABLE cases (
    id BIGSERIAL PRIMARY KEY,
    year INT NOT NULL,
    sequence_number INT NOT NULL,
    full_case_number VARCHAR(255) NOT NULL UNIQUE,
    registration_date DATE NOT NULL,
    case_description VARCHAR(500) NOT NULL,
    matter_description TEXT,
    tribunal_id BIGINT NOT NULL,
    case_type_id BIGINT NOT NULL,
    case_category_id BIGINT,
    lawyer_id BIGINT NOT NULL,
    status_id BIGINT NOT NULL,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (tribunal_id) REFERENCES tribunals(id),
    FOREIGN KEY (case_type_id) REFERENCES case_types(id),
    FOREIGN KEY (case_category_id) REFERENCES case_categories(id),
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id),
    FOREIGN KEY (status_id) REFERENCES case_statuses(id)
);

CREATE UNIQUE INDEX idx_cases_full_number ON cases(full_case_number);
CREATE INDEX idx_cases_year ON cases(year);
CREATE INDEX idx_cases_tribunal ON cases(tribunal_id);
CREATE INDEX idx_cases_case_type ON cases(case_type_id);
CREATE INDEX idx_cases_case_category ON cases(case_category_id);
CREATE INDEX idx_cases_lawyer ON cases(lawyer_id);
CREATE INDEX idx_cases_status ON cases(status_id);
CREATE INDEX idx_cases_registration_date ON cases(registration_date);
CREATE INDEX idx_cases_deleted_at ON cases(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_cases_year_sequence ON cases(year, sequence_number);
```

**V30__create_financial_transactions_table.sql:**
```sql
CREATE TABLE financial_transactions (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('PAYMENT', 'EXPENSE')),
    amount DECIMAL(15,2) NOT NULL,
    payment_date DATE,
    payment_reference VARCHAR(100),
    lawyer_payment_year INT,
    fiscal_year_from DATE,
    fiscal_year_to DATE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
);

CREATE INDEX idx_transactions_case ON financial_transactions(case_id);
CREATE INDEX idx_transactions_payment_date ON financial_transactions(payment_date);
CREATE INDEX idx_transactions_type ON financial_transactions(transaction_type);
CREATE INDEX idx_transactions_lawyer_payment_year ON financial_transactions(lawyer_payment_year);
```

**V31__add_case_permissions.sql:**
```sql
-- Add case management permissions
INSERT INTO permissions (name, description) VALUES
('CASE_READ', 'Can view cases'),
('CASE_CREATE', 'Can create cases'),
('CASE_UPDATE', 'Can update cases'),
('CASE_DELETE', 'Can delete cases'),
('CASE_MANAGE', 'Full case management access'),
('LAWYER_READ', 'Can view lawyers'),
('LAWYER_CREATE', 'Can create lawyers'),
('LAWYER_UPDATE', 'Can update lawyers'),
('LAWYER_DELETE', 'Can delete lawyers'),
('LAWYER_MANAGE', 'Full lawyer management access'),
('TRIBUNAL_READ', 'Can view tribunals'),
('TRIBUNAL_MANAGE', 'Can manage tribunal reference data'),
('CASETYPE_READ', 'Can view case types'),
('CASETYPE_MANAGE', 'Can manage case type reference data');

-- Assign to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
AND p.name IN (
    'CASE_READ', 'CASE_CREATE', 'CASE_UPDATE', 'CASE_DELETE', 'CASE_MANAGE',
    'LAWYER_READ', 'LAWYER_CREATE', 'LAWYER_UPDATE', 'LAWYER_DELETE', 'LAWYER_MANAGE',
    'TRIBUNAL_READ', 'TRIBUNAL_MANAGE',
    'CASETYPE_READ', 'CASETYPE_MANAGE'
);
```

---

## REST API

### Case Endpoints

**Base URL:** `/api/cases`

**1. Create Case**
```
POST /api/cases
Authorization: Bearer {token}
Permission: CASE_CREATE
Content-Type: application/json

Request Body: CreateCaseRequest
Response: 201 Created, CaseResponse
```

**2. Get Case by ID**
```
GET /api/cases/{id}
Authorization: Bearer {token}
Permission: CASE_READ

Response: 200 OK, CaseResponse
```

**3. Search/List Cases**
```
GET /api/cases?year=2026&caseTypeCode=PENAL&page=0&size=20
Authorization: Bearer {token}
Permission: CASE_READ

Query Parameters:
- year: Integer (optional)
- caseTypeCode: String (optional)
- tribunalCode: String (optional)
- lawyerId: Long (optional)
- statusCode: String (optional)
- registrationDateFrom: LocalDate (optional)
- registrationDateTo: LocalDate (optional)
- page: Integer (default: 0)
- size: Integer (default: 20, max: 100)
- sortBy: String (default: "createdAt")
- sortDirection: String (default: "DESC")

Response: 200 OK, Page<CaseSummary>
```

**4. Update Case**
```
PUT /api/cases/{id}
Authorization: Bearer {token}
Permission: CASE_UPDATE
Content-Type: application/json

Request Body: UpdateCaseRequest
Response: 200 OK, CaseResponse
```

**5. Change Status**
```
PATCH /api/cases/{id}/status
Authorization: Bearer {token}
Permission: CASE_UPDATE
Content-Type: application/json

Request Body: ChangeStatusRequest
Response: 200 OK, CaseResponse
```

**6. Delete Case (Soft Delete)**
```
DELETE /api/cases/{id}
Authorization: Bearer {token}
Permission: CASE_DELETE

Response: 204 No Content
```

### Reference Data Endpoints

**Tribunals:**
```
GET /api/tribunals - List all active tribunals (TRIBUNAL_READ)
GET /api/tribunals/{id} - Get single tribunal (TRIBUNAL_READ)
POST /api/tribunals - Create tribunal (TRIBUNAL_MANAGE)
PUT /api/tribunals/{id} - Update tribunal (TRIBUNAL_MANAGE)
DELETE /api/tribunals/{id} - Deactivate tribunal (TRIBUNAL_MANAGE)
```

**Case Types:**
```
GET /api/case-types - List all active case types (CASETYPE_READ)
GET /api/case-types/{code} - Get single case type (CASETYPE_READ)
PUT /api/case-types/{code}/template - Update number format template (CASETYPE_MANAGE)
PUT /api/case-types/{code}/statuses - Update allowed statuses (CASETYPE_MANAGE)
```

**Case Statuses:**
```
GET /api/case-statuses - List all statuses (CASE_READ)
GET /api/case-statuses?caseTypeCode=PENAL - List statuses for case type (CASE_READ)
```

**Lawyers:**
```
GET /api/lawyers - List all active lawyers (LAWYER_READ)
GET /api/lawyers/{id} - Get single lawyer (LAWYER_READ)
POST /api/lawyers - Create lawyer (LAWYER_CREATE)
PUT /api/lawyers/{id} - Update lawyer (LAWYER_UPDATE)
DELETE /api/lawyers/{id} - Deactivate lawyer (LAWYER_DELETE)
GET /api/lawyers/{id}/cases - Count active cases (LAWYER_READ)
```

**Financial Transactions:**
```
GET /api/cases/{caseId}/transactions - List transactions (CASE_READ)
POST /api/cases/{caseId}/transactions - Create transaction (CASE_UPDATE)
GET /api/cases/{caseId}/transactions/summary - Get financial summary (CASE_READ)
```

---

## Frontend Components

### Module Structure

```
frontend/src/app/features/cases/
├── components/
│   ├── case-list/
│   │   ├── case-list.component.ts
│   │   ├── case-list.component.html
│   │   └── case-list.component.scss
│   ├── case-detail/
│   │   ├── case-detail.component.ts
│   │   ├── case-detail.component.html
│   │   └── case-detail.component.scss
│   ├── case-form/
│   │   ├── case-form.component.ts
│   │   ├── case-form.component.html
│   │   └── case-form.component.scss
│   ├── case-status-badge/
│   │   ├── case-status-badge.component.ts
│   │   └── case-status-badge.component.html
│   └── case-filters/
│       ├── case-filters.component.ts
│       └── case-filters.component.html
├── services/
│   ├── case.service.ts
│   ├── tribunal.service.ts
│   ├── case-type.service.ts
│   └── lawyer.service.ts
├── models/
│   ├── case.model.ts
│   ├── tribunal.model.ts
│   └── case-filters.model.ts
└── routes.ts
```

### Services

**CaseService:**
```typescript
@Injectable({ providedIn: 'root' })
export class CaseService {
  private http = inject(HttpClient);
  private baseUrl = '/api/cases';

  // TanStack Query integration
  getCases = injectQuery(() => ({
    queryKey: ['cases', this.filters()],
    queryFn: () => this.http.get<Page<CaseSummary>>(
      this.baseUrl,
      { params: this.buildParams(this.filters()) }
    ).toPromise()
  }));

  getCase = (id: number) => injectQuery(() => ({
    queryKey: ['case', id],
    queryFn: () => this.http.get<CaseResponse>(`${this.baseUrl}/${id}`).toPromise()
  }));

  createCase = injectMutation(() => ({
    mutationFn: (request: CreateCaseRequest) =>
      this.http.post<CaseResponse>(this.baseUrl, request).toPromise(),
    onSuccess: () => {
      this.queryClient.invalidateQueries({ queryKey: ['cases'] });
      this.toast.success('Case created successfully');
    }
  }));

  updateCase = injectMutation(() => ({
    mutationFn: ({ id, request }: { id: number, request: UpdateCaseRequest }) =>
      this.http.put<CaseResponse>(`${this.baseUrl}/${id}`, request).toPromise(),
    onSuccess: (_, { id }) => {
      this.queryClient.invalidateQueries({ queryKey: ['case', id] });
      this.queryClient.invalidateQueries({ queryKey: ['cases'] });
      this.toast.success('Case updated successfully');
    }
  }));

  changeStatus = injectMutation(() => ({
    mutationFn: ({ id, request }: { id: number, request: ChangeStatusRequest }) =>
      this.http.patch<CaseResponse>(`${this.baseUrl}/${id}/status`, request).toPromise(),
    onSuccess: (_, { id }) => {
      this.queryClient.invalidateQueries({ queryKey: ['case', id] });
      this.queryClient.invalidateQueries({ queryKey: ['cases'] });
    }
  }));

  deleteCase = injectMutation(() => ({
    mutationFn: (id: number) =>
      this.http.delete<void>(`${this.baseUrl}/${id}`).toPromise(),
    onSuccess: () => {
      this.queryClient.invalidateQueries({ queryKey: ['cases'] });
      this.toast.success('Case deleted successfully');
    }
  }));
}
```

### Components

**CaseListComponent:**
```typescript
@Component({
  selector: 'app-case-list',
  standalone: true,
  imports: [CommonModule, RouterLink, CaseStatusBadgeComponent, CaseFiltersComponent],
  templateUrl: './case-list.component.html'
})
export class CaseListComponent {
  private caseService = inject(CaseService);
  private router = inject(Router);

  filters = signal<CaseSearchParams>({
    page: 0,
    size: 20,
    sortBy: 'registrationDate',
    sortDirection: 'DESC'
  });

  cases = this.caseService.getCases();

  onFilterChange(newFilters: CaseSearchParams) {
    this.filters.set(newFilters);
  }

  onPageChange(page: number) {
    this.filters.update(f => ({ ...f, page }));
  }

  viewCase(id: number) {
    this.router.navigate(['/cases', id]);
  }

  editCase(id: number) {
    this.router.navigate(['/cases', id, 'edit']);
  }

  deleteCase(id: number) {
    if (confirm('Are you sure you want to delete this case?')) {
      this.caseService.deleteCase.mutate(id);
    }
  }
}
```

**Template (case-list.component.html):**
```html
<div class="container mx-auto px-4 py-6">
  <!-- Header -->
  <div class="flex justify-between items-center mb-6">
    <h1 class="text-3xl font-bold">Cases / Dossiers</h1>
    <button
      *ngIf="hasPermission('CASE_CREATE')"
      routerLink="/cases/new"
      class="btn btn-primary">
      <svg class="w-5 h-5 mr-2"><!-- Plus icon --></svg>
      New Case
    </button>
  </div>

  <!-- Filters -->
  <app-case-filters
    [filters]="filters()"
    (filtersChange)="onFilterChange($event)"
    class="mb-6 block" />

  <!-- Loading State -->
  <div *ngIf="cases.isLoading()" class="text-center py-12">
    <div class="spinner"></div>
  </div>

  <!-- Error State -->
  <div *ngIf="cases.isError()" class="alert alert-error">
    Error loading cases: {{ cases.error()?.message }}
  </div>

  <!-- Data Table -->
  <div *ngIf="cases.isSuccess()" class="bg-white shadow rounded-lg overflow-hidden">
    <table class="min-w-full divide-y divide-gray-200">
      <thead class="bg-gray-50">
        <tr>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
            Case Number
          </th>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
            Description
          </th>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
            Tribunal
          </th>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
            Type
          </th>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
            Lawyer
          </th>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
            Status
          </th>
          <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
            Date
          </th>
          <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
            Actions
          </th>
        </tr>
      </thead>
      <tbody class="bg-white divide-y divide-gray-200">
        <tr *ngFor="let case of cases.data()?.content" class="hover:bg-gray-50">
          <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
            {{ case.fullCaseNumber }}
          </td>
          <td class="px-6 py-4 text-sm text-gray-900">
            {{ case.caseDescription }}
          </td>
          <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
            {{ case.tribunalNameFr }}
          </td>
          <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
            {{ case.caseTypeNameFr }}
          </td>
          <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
            {{ case.lawyerName }}
          </td>
          <td class="px-6 py-4 whitespace-nowrap">
            <app-case-status-badge [status]="case.statusNameFr" />
          </td>
          <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
            {{ case.registrationDate | date:'dd/MM/yyyy' }}
          </td>
          <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
            <button (click)="viewCase(case.id)" class="text-blue-600 hover:text-blue-900 mr-3">
              View
            </button>
            <button
              *ngIf="hasPermission('CASE_UPDATE')"
              (click)="editCase(case.id)"
              class="text-indigo-600 hover:text-indigo-900 mr-3">
              Edit
            </button>
            <button
              *ngIf="hasPermission('CASE_DELETE')"
              (click)="deleteCase(case.id)"
              class="text-red-600 hover:text-red-900">
              Delete
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Pagination -->
    <div class="bg-white px-4 py-3 border-t border-gray-200">
      <app-pagination
        [currentPage]="cases.data()?.number"
        [totalPages]="cases.data()?.totalPages"
        [totalElements]="cases.data()?.totalElements"
        (pageChange)="onPageChange($event)" />
    </div>
  </div>
</div>
```

**CaseFormComponent:**
```typescript
@Component({
  selector: 'app-case-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './case-form.component.html'
})
export class CaseFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private caseService = inject(CaseService);
  private tribunalService = inject(TribunalService);
  private caseTypeService = inject(CaseTypeService);
  private lawyerService = inject(LawyerService);

  caseId = signal<number | null>(null);
  isEditMode = computed(() => this.caseId() !== null);

  tribunals = this.tribunalService.getTribunals();
  caseTypes = this.caseTypeService.getCaseTypes();
  lawyers = this.lawyerService.getLawyers();

  form = this.fb.group({
    caseTypeCode: ['', Validators.required],
    tribunalCode: ['', Validators.required],
    lawyerId: [null as number | null, Validators.required],
    registrationDate: [new Date().toISOString().split('T')[0], Validators.required],
    caseDescription: ['', [Validators.required, Validators.maxLength(500)]],
    matterDescription: ['', Validators.maxLength(1000)],
    initialStatusCode: ['']
  });

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.caseId.set(+id);
      this.loadCase(+id);
    }
  }

  loadCase(id: number) {
    this.caseService.getCase(id).subscribe(caseData => {
      this.form.patchValue({
        tribunalCode: caseData.tribunal.code,
        lawyerId: caseData.lawyer.id,
        registrationDate: caseData.registrationDate,
        caseDescription: caseData.caseDescription,
        matterDescription: caseData.matterDescription
      });

      // Disable immutable fields in edit mode
      this.form.get('caseTypeCode')?.disable();
    });
  }

  onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const formValue = this.form.getRawValue();

    if (this.isEditMode()) {
      this.caseService.updateCase.mutate({
        id: this.caseId()!,
        request: formValue as UpdateCaseRequest
      }, {
        onSuccess: () => this.router.navigate(['/cases', this.caseId()])
      });
    } else {
      this.caseService.createCase.mutate(formValue as CreateCaseRequest, {
        onSuccess: (response) => this.router.navigate(['/cases', response.id])
      });
    }
  }

  cancel() {
    if (this.isEditMode()) {
      this.router.navigate(['/cases', this.caseId()]);
    } else {
      this.router.navigate(['/cases']);
    }
  }
}
```

**CaseStatusBadgeComponent:**
```typescript
@Component({
  selector: 'app-case-status-badge',
  standalone: true,
  template: `
    <span [ngClass]="badgeClasses()">
      {{ status() }}
    </span>
  `
})
export class CaseStatusBadgeComponent {
  status = input.required<string>();
  size = input<'sm' | 'md' | 'lg'>('md');

  badgeClasses = computed(() => {
    const baseClasses = 'inline-flex items-center rounded-full font-medium';
    const sizeClasses = {
      sm: 'px-2 py-0.5 text-xs',
      md: 'px-2.5 py-0.5 text-sm',
      lg: 'px-3 py-1 text-base'
    };

    const colorClasses = this.getColorClasses(this.status());

    return `${baseClasses} ${sizeClasses[this.size()]} ${colorClasses}`;
  });

  private getColorClasses(status: string): string {
    const statusLower = status.toLowerCase();

    if (statusLower.includes('brouillon') || statusLower.includes('draft')) {
      return 'bg-gray-100 text-gray-800';
    }
    if (statusLower.includes('ouvert') || statusLower.includes('open')) {
      return 'bg-blue-100 text-blue-800';
    }
    if (statusLower.includes('cours') || statusLower.includes('progress')) {
      return 'bg-yellow-100 text-yellow-800';
    }
    if (statusLower.includes('audience') || statusLower.includes('hearing')) {
      return 'bg-purple-100 text-purple-800';
    }
    if (statusLower.includes('jugement') || statusLower.includes('judgment')) {
      return 'bg-orange-100 text-orange-800';
    }
    if (statusLower.includes('clôturé') || statusLower.includes('closed')) {
      return 'bg-green-100 text-green-800';
    }
    if (statusLower.includes('archivé') || statusLower.includes('archived')) {
      return 'bg-slate-100 text-slate-800';
    }

    return 'bg-gray-100 text-gray-800';
  }
}
```

### Routing

```typescript
export const caseRoutes: Routes = [
  {
    path: '',
    component: CaseListComponent,
    canActivate: [authGuard],
    data: { permission: 'CASE_READ' }
  },
  {
    path: 'new',
    component: CaseFormComponent,
    canActivate: [authGuard],
    data: { permission: 'CASE_CREATE' }
  },
  {
    path: ':id',
    component: CaseDetailComponent,
    canActivate: [authGuard],
    data: { permission: 'CASE_READ' }
  },
  {
    path: ':id/edit',
    component: CaseFormComponent,
    canActivate: [authGuard],
    data: { permission: 'CASE_UPDATE' }
  }
];
```

---

## Error Handling

### Custom Exceptions

**InvalidCaseNumberFormatException:**
```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCaseNumberFormatException extends RuntimeException {
    public InvalidCaseNumberFormatException(String message) {
        super(message);
    }
}
```

**InvalidStatusTransitionException:**
```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
```

**CaseNumberAlreadyExistsException:**
```java
@ResponseStatus(HttpStatus.CONFLICT)
public class CaseNumberAlreadyExistsException extends RuntimeException {
    public CaseNumberAlreadyExistsException(String caseNumber) {
        super("Case number already exists: " + caseNumber);
    }
}
```

**SequenceGenerationException:**
```java
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SequenceGenerationException extends RuntimeException {
    public SequenceGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### GlobalExceptionHandler Extensions

```java
@ExceptionHandler(InvalidCaseNumberFormatException.class)
public ResponseEntity<ErrorResponse> handleInvalidCaseNumberFormat(
    InvalidCaseNumberFormatException ex,
    Locale locale
) {
    ErrorResponse error = ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .message(messageSource.getMessage("error.case.invalid_number_format", null, locale))
        .detail(ex.getMessage())
        .timestamp(LocalDateTime.now())
        .build();
    return ResponseEntity.badRequest().body(error);
}

@ExceptionHandler(InvalidStatusTransitionException.class)
public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(
    InvalidStatusTransitionException ex,
    Locale locale
) {
    ErrorResponse error = ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .message(messageSource.getMessage("error.case.invalid_status_transition", null, locale))
        .detail(ex.getMessage())
        .timestamp(LocalDateTime.now())
        .build();
    return ResponseEntity.badRequest().body(error);
}
```

### Business Validation Rules

**Immutable Fields:**
- `year`, `sequenceNumber`, `fullCaseNumber`, `caseType` cannot be changed after creation
- Attempting to update these fields should throw `IllegalArgumentException`

**Delete Constraints:**
- Cannot delete case with financial transactions (or cascade delete with warning/confirmation)
- Soft delete sets `deletedAt` timestamp

**Reference Data Constraints:**
- Cannot assign inactive lawyer, tribunal, or case type
- Must validate existence before assignment

**Date Validation:**
- Registration date cannot be in the future
- Fiscal year dates must be valid ranges

---

## Security & Permissions

### Permission Model

**Case Permissions:**
- `CASE_READ` - View cases
- `CASE_CREATE` - Create new cases
- `CASE_UPDATE` - Update existing cases (including status changes)
- `CASE_DELETE` - Soft delete cases
- `CASE_MANAGE` - Full access (all above)

**Lawyer Permissions:**
- `LAWYER_READ` - View lawyers
- `LAWYER_CREATE` - Create lawyers
- `LAWYER_UPDATE` - Update lawyers
- `LAWYER_DELETE` - Deactivate lawyers
- `LAWYER_MANAGE` - Full access

**Reference Data Permissions:**
- `TRIBUNAL_READ` - View tribunals (public, all users)
- `TRIBUNAL_MANAGE` - Manage tribunal data (admin only)
- `CASETYPE_READ` - View case types (public, all users)
- `CASETYPE_MANAGE` - Manage case types and templates (admin only)

### Controller Security

```java
@RestController
@RequestMapping("/api/cases")
@PreAuthorize("isAuthenticated()")
public class CaseController {

    @GetMapping
    @PreAuthorize("hasPermission(null, 'CASE_READ')")
    public ResponseEntity<Page<CaseSummary>> getCases(@Valid CaseSearchRequest request) {
        // ...
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'CASE_CREATE')")
    public ResponseEntity<CaseResponse> createCase(@Valid @RequestBody CreateCaseRequest request) {
        // ...
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CASE_UPDATE')")
    public ResponseEntity<CaseResponse> updateCase(
        @PathVariable Long id,
        @Valid @RequestBody UpdateCaseRequest request
    ) {
        // ...
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CASE_DELETE')")
    public ResponseEntity<Void> deleteCase(@PathVariable Long id) {
        // ...
    }
}
```

### Audit Trail

**Events Published:**
- `CaseCreatedEvent` - When case is created
- `CaseUpdatedEvent` - When case is updated (with change details)
- `CaseStatusChangedEvent` - When status changes
- `CaseDeletedEvent` - When case is soft deleted

**Audit Log Fields:**
- Entity type: "Case"
- Entity ID: Case ID
- Action: CREATE, UPDATE, STATUS_CHANGE, DELETE
- User: Current user ID and username
- Timestamp: Event timestamp
- Changes: JSON of old vs new values
- IP Address: Request IP
- User Agent: Browser/client info

---

## Testing Strategy

### Unit Tests

**CaseServiceTest:**
```java
@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @Mock private CaseRepository caseRepository;
    @Mock private TribunalRepository tribunalRepository;
    @Mock private CaseTypeRepository caseTypeRepository;
    @Mock private LawyerRepository lawyerRepository;
    @Mock private CaseStatusRepository caseStatusRepository;
    @Mock private CaseSequenceService caseSequenceService;
    @Mock private CaseNumberGenerator caseNumberGenerator;
    @Mock private AuditPublisher auditPublisher;
    @Mock private CaseMapper caseMapper;

    @InjectMocks private CaseService caseService;

    @Test
    void createCase_WithValidData_ShouldSucceed() {
        // Given
        CreateCaseRequest request = new CreateCaseRequest(
            "PENAL", "TR_PIN_1", 1L, LocalDate.now(),
            "Test case", "Test matter", null
        );

        when(caseTypeRepository.findByCodeAndActiveTrue("PENAL"))
            .thenReturn(Optional.of(mockCaseType()));
        when(tribunalRepository.findByCodeAndActiveTrue("TR_PIN_1"))
            .thenReturn(Optional.of(mockTribunal()));
        when(lawyerRepository.findByIdAndActiveTrue(1L))
            .thenReturn(Optional.of(mockLawyer()));
        when(caseSequenceService.getNextSequence(2026, "PENAL"))
            .thenReturn(1);
        when(caseNumberGenerator.generate(any(), eq(2026), eq("TR_PIN_1"), eq("PENAL"), eq(1)))
            .thenReturn("2026-TR_PIN_1-PENAL-00001");

        // When
        CaseResponse response = caseService.createCase(request, mockUser());

        // Then
        assertThat(response).isNotNull();
        verify(caseRepository).save(any(Case.class));
        verify(auditPublisher).publishCaseCreated(any(), any());
    }

    @Test
    void createCase_WithInactiveLawyer_ShouldThrowException() {
        // Given
        CreateCaseRequest request = new CreateCaseRequest(
            "PENAL", "TR_PIN_1", 1L, LocalDate.now(),
            "Test case", "Test matter", null
        );

        when(lawyerRepository.findByIdAndActiveTrue(1L))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> caseService.createCase(request, mockUser()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Lawyer");
    }

    @Test
    void changeStatus_WithDisallowedStatus_ShouldThrowException() {
        // Test invalid status transition
    }
}
```

**CaseSequenceServiceTest:**
```java
@ExtendWith(MockitoExtension.class)
class CaseSequenceServiceTest {

    @Test
    void getNextSequence_FirstTime_ShouldReturnOne() {
        // Test first sequence generation
    }

    @Test
    void getNextSequence_Existing_ShouldIncrement() {
        // Test sequence increment
    }

    @Test
    void getNextSequence_NewYear_ShouldResetToOne() {
        // Test year rollover
    }
}
```

### Integration Tests

**CaseControllerIntegrationTest:**
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CaseControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private CaseRepository caseRepository;

    private String authToken;

    @BeforeEach
    void setUp() {
        // Login and get token
        authToken = authenticateAsAdmin();
    }

    @Test
    void createCase_EndToEnd_ShouldSucceed() {
        // Given
        CreateCaseRequest request = new CreateCaseRequest(...);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<CreateCaseRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<CaseResponse> response = restTemplate.postForEntity(
            "/api/cases",
            entity,
            CaseResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getFullCaseNumber()).matches("\\d{4}-TR_.*-\\w+-\\d{5}");

        // Verify in database
        Case savedCase = caseRepository.findById(response.getBody().getId()).orElseThrow();
        assertThat(savedCase.getFullCaseNumber()).isEqualTo(response.getBody().getFullCaseNumber());
    }

    @Test
    void searchCases_WithFilters_ShouldReturnFilteredResults() {
        // Test search functionality
    }
}
```

**CaseSequenceConcurrencyTest:**
```java
@Test
void generateSequence_Concurrent_ShouldNotProduceDuplicates() throws Exception {
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    Set<Integer> generatedSequences = ConcurrentHashMap.newKeySet();

    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < threadCount; i++) {
        futures.add(executor.submit(() -> {
            int seq = caseSequenceService.getNextSequence(2026, "PENAL");
            generatedSequences.add(seq);
        }));
    }

    for (Future<?> future : futures) {
        future.get();
    }

    assertThat(generatedSequences).hasSize(threadCount);
    assertThat(generatedSequences).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
}
```

### Frontend Tests

**case.service.spec.ts:**
```typescript
describe('CaseService', () => {
  let service: CaseService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CaseService]
    });

    service = TestBed.inject(CaseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should create a case', () => {
    const request: CreateCaseRequest = { /* ... */ };
    const expectedResponse: CaseResponse = { /* ... */ };

    service.createCase.mutate(request);

    const req = httpMock.expectOne('/api/cases');
    expect(req.request.method).toBe('POST');
    req.flush(expectedResponse);
  });
});
```

---

## Bilingual Support

### Implementation Strategy

**Backend:**
1. Store both French and Arabic names in database (`name_fr`, `name_ar`)
2. Accept-Language header determines response language preference
3. DTOs include both languages, frontend selects display language
4. Error messages use Spring MessageSource with locale support

**Frontend:**
1. Language toggle in header (FR/AR/EN)
2. Store preference in localStorage
3. Apply RTL CSS classes when Arabic selected
4. Display appropriate name field based on current locale

**Example:**
```typescript
@Component({
  selector: 'app-tribunal-display',
  template: `
    <span [class.rtl]="isArabic()">
      {{ isArabic() ? tribunal.nameAr : tribunal.nameFr }}
    </span>
  `
})
export class TribunalDisplayComponent {
  locale = inject(LocaleService);
  isArabic = computed(() => this.locale.current() === 'ar');
}
```

**RTL CSS:**
```css
.rtl {
  direction: rtl;
  text-align: right;
  font-family: 'Arabic Font', sans-serif;
}
```

---

## Implementation Checklist

### Phase 1: Database & Domain
- [ ] Create Flyway migrations V17-V31
- [ ] Seed tribunals (130+ courts)
- [ ] Seed case types (4 types)
- [ ] Seed case categories (300+ detailed classifications)
- [ ] Seed case statuses (7 statuses)
- [ ] Seed case type-status relationships
- [ ] Add case permissions to V31

### Phase 2: Backend Entities & Repositories
- [ ] Create Tribunal entity and repository
- [ ] Create CaseType entity and repository
- [ ] Create CaseCategory entity and repository
- [ ] Create CaseStatus entity and repository
- [ ] Create Lawyer entity and repository
- [ ] Create CaseSequence entity and repository
- [ ] Create Case entity and repository
- [ ] Create FinancialTransaction entity and repository
- [ ] Add JPA Specifications for Case filtering

### Phase 3: Backend Application Layer
- [ ] Create all Request DTOs with validation
- [ ] Create all Response DTOs
- [ ] Create MapStruct mappers
- [ ] Implement CaseNumberGenerator
- [ ] Implement CaseSequenceService
- [ ] Implement CaseService
- [ ] Implement TribunalService
- [ ] Implement CaseTypeService
- [ ] Implement LawyerService
- [ ] Implement FinancialTransactionService
- [ ] Add audit events

### Phase 4: Backend Controllers
- [ ] Implement CaseController with all endpoints
- [ ] Implement TribunalController
- [ ] Implement CaseTypeController
- [ ] Implement LawyerController
- [ ] Add OpenAPI documentation
- [ ] Add custom exceptions
- [ ] Extend GlobalExceptionHandler

### Phase 5: Frontend Services
- [ ] Create CaseService with TanStack Query
- [ ] Create TribunalService
- [ ] Create CaseTypeService
- [ ] Create LawyerService
- [ ] Create models/interfaces

### Phase 6: Frontend Components
- [ ] Create CaseListComponent
- [ ] Create CaseDetailComponent
- [ ] Create CaseFormComponent
- [ ] Create CaseStatusBadgeComponent
- [ ] Create CaseFiltersComponent
- [ ] Add routing configuration
- [ ] Implement permission guards

### Phase 7: Testing
- [ ] Write unit tests for CaseService
- [ ] Write unit tests for CaseSequenceService
- [ ] Write unit tests for CaseNumberGenerator
- [ ] Write integration tests for CaseController
- [ ] Write concurrency test for sequence generation
- [ ] Write frontend service tests
- [ ] Write frontend component tests

### Phase 8: Documentation & Polish
- [ ] Update README with case management features
- [ ] Add API documentation in Swagger
- [ ] Test bilingual support (FR/AR)
- [ ] Test RTL layout
- [ ] Verify all permissions work correctly
- [ ] Test audit logging
- [ ] Performance testing with large datasets

---

## Next Steps

After design approval, proceed to implementation planning using the `/writing-plans` skill to create detailed, batch-based implementation tasks.

**Estimated Implementation Effort:** 3-5 days for experienced developer

**Dependencies:**
- Existing foundation (User, Role, Permission, Audit) must be functional
- PostgreSQL database
- Angular 18 frontend with existing auth infrastructure

**Success Criteria:**
- All migrations run successfully
- Case number generation works correctly (no duplicates)
- Status transitions respect case type rules
- Bilingual support works for French and Arabic
- All permissions properly enforced
- Audit trail captures all case changes
- Frontend displays cases with filters and pagination
- Unit and integration tests pass with >70% coverage
