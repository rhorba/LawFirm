# Client Management Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: use executing-plans skill to implement this plan task-by-task.

**Goal:** Build a full-stack Client Management module — individual/corporate/government client profiles, linked to cases, with search, soft-delete, and Excel export.

**Architecture:** Three Flyway migrations (V41–V43) create the `clients` table, seed permissions, and add `client_id` FK to `cases`. A Spring Boot service layer validates type-specific rules (age, required fields, uniqueness). Angular 18 standalone components follow the same inline-modal pattern as Lawyer management.

**Tech Stack:** Spring Boot 3.4 (Java 21), Angular 18, Flyway, MapStruct, Tailwind.

---

## Task 1: Database — Create `clients` table (V41)

**Files:**
- Create: `backend/src/main/resources/db/migration/V41__create_clients_table.sql`

**Step 1: Write migration**

```sql
-- V41: Create clients table
CREATE TABLE clients (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    version          BIGINT NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),

    client_type      VARCHAR(20)  NOT NULL,

    -- Common fields
    first_name       VARCHAR(100),
    last_name        VARCHAR(100),
    phone            VARCHAR(20),
    email            VARCHAR(100) UNIQUE,
    address          TEXT,
    notes            TEXT,
    active           BOOLEAN NOT NULL DEFAULT TRUE,

    -- INDIVIDUAL only
    cin              VARCHAR(20) UNIQUE,
    gender           VARCHAR(10),
    date_of_birth    DATE,

    -- CORPORATE / GOVERNMENT only
    company_name     VARCHAR(200),
    tax_number       VARCHAR(50) UNIQUE
);

CREATE INDEX idx_clients_type   ON clients(client_type);
CREATE INDEX idx_clients_active ON clients(active);
CREATE INDEX idx_clients_cin    ON clients(cin);
CREATE INDEX idx_clients_name   ON clients(last_name, first_name);
```

**Step 2: Verify**
```bash
cd backend && mvn flyway:info -Dspring-boot.run.profiles=dev
```
Expected: V41 listed as "Pending".

---

## Task 2: Database — Seed client permissions (V42)

**Files:**
- Create: `backend/src/main/resources/db/migration/V42__seed_client_permissions.sql`

**Step 1: Write migration**

```sql
-- V42: Seed client permissions
INSERT INTO permissions (name) VALUES
  ('CLIENT_READ'), ('CLIENT_CREATE'),
  ('CLIENT_UPDATE'), ('CLIENT_DELETE'), ('CLIENT_MANAGE')
ON CONFLICT (name) DO NOTHING;

-- Assign all to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('CLIENT_READ','CLIENT_CREATE','CLIENT_UPDATE','CLIENT_DELETE','CLIENT_MANAGE')
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
```

> **Note on H2 compatibility:** `ON CONFLICT DO NOTHING` is supported in H2 2.x (dev profile uses H2 2.2.x). This is safe for both dev and prod.

**Step 2: Verify**
```bash
mvn spring-boot:run
```
Expected: Backend starts without Flyway errors.

---

## Task 3: Database — Add `client_id` FK to `cases` (V43)

**Files:**
- Create: `backend/src/main/resources/db/migration/V43__add_client_id_to_cases.sql`

**Step 1: Write migration**

```sql
-- V43: Link cases to clients (nullable FK — existing cases have no client yet)
ALTER TABLE cases ADD COLUMN client_id BIGINT;
ALTER TABLE cases ADD CONSTRAINT fk_cases_client
    FOREIGN KEY (client_id) REFERENCES clients(id);
CREATE INDEX idx_cases_client ON cases(client_id);
```

**Step 2: Verify**
```bash
mvn spring-boot:run
```
Expected: All three migrations (V41–V43) applied successfully.

---

## Task 4: Domain — Enums and `Client` Entity

**Files:**
- Create: `backend/src/main/java/com/lawfirm/domain/model/ClientType.java`
- Create: `backend/src/main/java/com/lawfirm/domain/model/Gender.java`
- Create: `backend/src/main/java/com/lawfirm/domain/model/Client.java`
- Modify: `backend/src/main/java/com/lawfirm/domain/model/Case.java`

**Step 1: Create `ClientType.java`**

```java
package com.lawfirm.domain.model;

public enum ClientType {
    INDIVIDUAL, CORPORATE, GOVERNMENT
}
```

**Step 2: Create `Gender.java`**

```java
package com.lawfirm.domain.model;

public enum Gender {
    MALE, FEMALE
}
```

**Step 3: Create `Client.java`**

```java
package com.lawfirm.domain.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Client extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientType clientType;

    // Common
    @Column(length = 100) private String firstName;
    @Column(length = 100) private String lastName;
    @Column(length = 20)  private String phone;
    @Column(length = 100, unique = true) private String email;
    @Column(columnDefinition = "TEXT") private String address;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(nullable = false) @Builder.Default private Boolean active = true;

    // INDIVIDUAL only
    @Column(length = 20, unique = true) private String cin;
    @Enumerated(EnumType.STRING)
    @Column(length = 10) private Gender gender;
    @Column private LocalDate dateOfBirth;

    // CORPORATE / GOVERNMENT only
    @Column(length = 200) private String companyName;
    @Column(length = 50, unique = true) private String taxNumber;

    @OneToMany(mappedBy = "client")
    @Builder.Default
    private List<Case> cases = new ArrayList<>();

    public String getFullName() {
        return clientType == ClientType.INDIVIDUAL
            ? firstName + " " + lastName
            : companyName;
    }

    public Integer getAge() {
        if (dateOfBirth == null) return null;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
```

**Step 4: Add `client` association to `Case.java`**

Add this field inside `Case.java`, after the existing `parentCase` field:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "client_id")
private Client client;
```

Also add the import at the top: `import com.lawfirm.domain.model.Client;` (already in same package, so no import needed).

**Step 5: Verify compilation**
```bash
mvn clean compile
```
Expected: BUILD SUCCESS, no errors.

---

## Task 5: Domain — `ClientRepository`

**Files:**
- Create: `backend/src/main/java/com/lawfirm/domain/repository/ClientRepository.java`

**Step 1: Create repository**

```java
package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.Client;
import com.lawfirm.domain.model.ClientType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByCin(String cin);
    Optional<Client> findByTaxNumber(String taxNumber);
    Optional<Client> findByEmail(String email);
    boolean existsByCin(String cin);
    boolean existsByTaxNumber(String taxNumber);
    boolean existsByEmail(String email);

    @Query("SELECT c FROM Client c WHERE c.active = true AND " +
           "(:search IS NULL OR " +
           "LOWER(c.firstName)   LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(c.lastName)    LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(c.companyName) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(c.cin)         LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(c.email)       LIKE LOWER(CONCAT('%',:search,'%'))) AND " +
           "(:type IS NULL OR c.clientType = :type)")
    Page<Client> search(
        @Param("search") String search,
        @Param("type") ClientType type,
        Pageable pageable
    );
}
```

**Step 2: Verify**
```bash
mvn clean compile
```

---

## Task 6: Application — DTOs

**Files:**
- Create: `backend/src/main/java/com/lawfirm/application/dto/request/CreateClientRequest.java`
- Create: `backend/src/main/java/com/lawfirm/application/dto/request/UpdateClientRequest.java`
- Create: `backend/src/main/java/com/lawfirm/application/dto/response/ClientResponse.java`
- Create: `backend/src/main/java/com/lawfirm/application/dto/response/ClientSummary.java`

**Step 1: `CreateClientRequest.java`**

```java
package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.ClientType;
import com.lawfirm.domain.model.Gender;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateClientRequest(
    @NotNull ClientType clientType,
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Size(max = 20) @Pattern(regexp = "^\\+?[0-9\\s\\-]{7,20}$", message = "Invalid phone format") String phone,
    @Email @Size(max = 100) String email,
    String address,
    String notes,
    @Size(max = 20) String cin,
    Gender gender,
    LocalDate dateOfBirth,
    @Size(max = 200) String companyName,
    @Size(max = 50) String taxNumber
) {}
```

**Step 2: `UpdateClientRequest.java`**

```java
package com.lawfirm.application.dto.request;

import com.lawfirm.domain.model.Gender;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UpdateClientRequest(
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Size(max = 20) @Pattern(regexp = "^\\+?[0-9\\s\\-]{7,20}$", message = "Invalid phone format") String phone,
    @Email @Size(max = 100) String email,
    String address,
    String notes,
    @Size(max = 20) String cin,
    Gender gender,
    LocalDate dateOfBirth,
    @Size(max = 200) String companyName,
    @Size(max = 50) String taxNumber
) {}
```

**Step 3: `ClientResponse.java`**

```java
package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.ClientType;
import com.lawfirm.domain.model.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClientResponse(
    Long id,
    String fullName,
    ClientType clientType,
    String firstName,
    String lastName,
    String phone,
    String email,
    String address,
    String notes,
    String cin,
    Gender gender,
    LocalDate dateOfBirth,
    Integer age,
    String companyName,
    String taxNumber,
    Boolean active,
    int caseCount,
    LocalDateTime createdAt
) {}
```

**Step 4: `ClientSummary.java`**

```java
package com.lawfirm.application.dto.response;

import com.lawfirm.domain.model.ClientType;

public record ClientSummary(
    Long id,
    String fullName,
    ClientType clientType,
    String cin,
    String phone,
    String email,
    Boolean active,
    int caseCount
) {}
```

**Step 5: Verify**
```bash
mvn clean compile
```

---

## Task 7: Application — `ClientMapper`

**Files:**
- Create: `backend/src/main/java/com/lawfirm/application/mapper/ClientMapper.java`

**Step 1: Create mapper**

```java
package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.request.CreateClientRequest;
import com.lawfirm.application.dto.request.UpdateClientRequest;
import com.lawfirm.application.dto.response.ClientResponse;
import com.lawfirm.application.dto.response.ClientSummary;
import com.lawfirm.domain.model.Client;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "fullName",  expression = "java(client.getFullName())")
    @Mapping(target = "age",       expression = "java(client.getAge())")
    @Mapping(target = "caseCount", expression = "java(client.getCases().size())")
    ClientResponse toResponse(Client client);

    @Mapping(target = "fullName",  expression = "java(client.getFullName())")
    @Mapping(target = "caseCount", expression = "java(client.getCases().size())")
    ClientSummary toSummary(Client client);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "active",    constant = "true")
    @Mapping(target = "cases",     ignore = true)
    Client toEntity(CreateClientRequest request);

    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "createdAt",  ignore = true)
    @Mapping(target = "updatedAt",  ignore = true)
    @Mapping(target = "version",    ignore = true)
    @Mapping(target = "active",     ignore = true)
    @Mapping(target = "clientType", ignore = true)
    @Mapping(target = "cases",      ignore = true)
    void updateEntity(UpdateClientRequest request, @MappingTarget Client client);
}
```

**Step 2: Verify**
```bash
mvn clean compile
```
Expected: MapStruct generates `ClientMapperImpl` without errors.

---

## Task 8: Application — `ClientService`

**Files:**
- Create: `backend/src/main/java/com/lawfirm/application/service/ClientService.java`

**Step 1: Create service**

```java
package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.CreateClientRequest;
import com.lawfirm.application.dto.request.UpdateClientRequest;
import com.lawfirm.application.dto.response.ClientResponse;
import com.lawfirm.application.dto.response.ClientSummary;
import com.lawfirm.application.mapper.ClientMapper;
import com.lawfirm.domain.model.Client;
import com.lawfirm.domain.model.ClientType;
import com.lawfirm.domain.repository.ClientRepository;
import com.lawfirm.presentation.exception.BusinessRuleException;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

import static com.lawfirm.domain.model.ClientType.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    // ── Read ────────────────────────────────────────────────────────────────

    public ClientResponse findById(Long id) {
        return clientMapper.toResponse(getOrThrow(id));
    }

    public Page<ClientSummary> search(String search, ClientType type, int page, int size) {
        String term = (search != null && search.isBlank()) ? null : search;
        return clientRepository.search(term, type, PageRequest.of(page, size))
            .map(clientMapper::toSummary);
    }

    // ── Write ───────────────────────────────────────────────────────────────

    @Transactional
    public ClientResponse create(CreateClientRequest request) {
        validateByType(request);
        validateAge(request.clientType(), request.dateOfBirth());
        checkUniqueness(request.cin(), request.taxNumber(), request.email(), null);
        Client saved = clientRepository.save(clientMapper.toEntity(request));
        return clientMapper.toResponse(saved);
    }

    @Transactional
    public ClientResponse update(Long id, UpdateClientRequest request) {
        Client client = getOrThrow(id);
        checkUniquenessOnUpdate(request, client);
        validateAge(client.getClientType(), request.dateOfBirth());
        clientMapper.updateEntity(request, client);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Transactional
    public void deactivate(Long id) {
        Client client = getOrThrow(id);
        client.setActive(false);
        clientRepository.save(client);
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private Client getOrThrow(Long id) {
        return clientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
    }

    private void validateByType(CreateClientRequest r) {
        if (r.clientType() == INDIVIDUAL
                && (r.firstName() == null || r.firstName().isBlank()
                    || r.lastName() == null || r.lastName().isBlank())) {
            throw new BusinessRuleException("Individual clients require first and last name");
        }
        if ((r.clientType() == CORPORATE || r.clientType() == GOVERNMENT)
                && (r.companyName() == null || r.companyName().isBlank())) {
            throw new BusinessRuleException("Corporate/Government clients require a company name");
        }
    }

    private void validateAge(ClientType type, LocalDate dob) {
        if (type == INDIVIDUAL && dob != null) {
            int age = Period.between(dob, LocalDate.now()).getYears();
            if (age < 18 || age > 100) {
                throw new BusinessRuleException("Client must be between 18 and 100 years old");
            }
        }
    }

    private void checkUniqueness(String cin, String taxNumber, String email, Long excludeId) {
        if (cin != null && !cin.isBlank()) {
            clientRepository.findByCin(cin).ifPresent(existing -> {
                if (excludeId == null || !existing.getId().equals(excludeId))
                    throw new BusinessRuleException("CIN already registered: " + cin);
            });
        }
        if (taxNumber != null && !taxNumber.isBlank()) {
            clientRepository.findByTaxNumber(taxNumber).ifPresent(existing -> {
                if (excludeId == null || !existing.getId().equals(excludeId))
                    throw new BusinessRuleException("Tax number already registered: " + taxNumber);
            });
        }
        if (email != null && !email.isBlank()) {
            clientRepository.findByEmail(email).ifPresent(existing -> {
                if (excludeId == null || !existing.getId().equals(excludeId))
                    throw new BusinessRuleException("Email already registered: " + email);
            });
        }
    }

    private void checkUniquenessOnUpdate(UpdateClientRequest r, Client client) {
        checkUniqueness(r.cin(), r.taxNumber(), r.email(), client.getId());
    }
}
```

**Step 2: Verify**
```bash
mvn clean compile
```

---

## Task 9: Application — `ClientExportService`

**Files:**
- Create: `backend/src/main/java/com/lawfirm/application/service/ClientExportService.java`

**Step 1: Create export service**

> Uses Apache POI (already on classpath from `CaseExportService`).

```java
package com.lawfirm.application.service;

import com.lawfirm.domain.model.Client;
import com.lawfirm.domain.model.ClientType;
import com.lawfirm.domain.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientExportService {

    private final ClientRepository clientRepository;

    private static final String[] HEADERS = {
        "Full Name", "Type", "CIN", "Company Name", "Tax Number",
        "Phone", "Email", "Address", "Active", "Case Count", "Date of Birth", "Registered At"
    };

    @Transactional(readOnly = true)
    public byte[] export(String search, ClientType type) {
        String term = (search != null && search.isBlank()) ? null : search;
        List<Client> clients = clientRepository
            .search(term, type, Pageable.unpaged())
            .getContent();
        return buildExcel(clients);
    }

    private byte[] buildExcel(List<Client> clients) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Clients");

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Client c : clients) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.getFullName() != null ? c.getFullName() : "");
                row.createCell(1).setCellValue(c.getClientType().name());
                row.createCell(2).setCellValue(c.getCin() != null ? c.getCin() : "");
                row.createCell(3).setCellValue(c.getCompanyName() != null ? c.getCompanyName() : "");
                row.createCell(4).setCellValue(c.getTaxNumber() != null ? c.getTaxNumber() : "");
                row.createCell(5).setCellValue(c.getPhone() != null ? c.getPhone() : "");
                row.createCell(6).setCellValue(c.getEmail() != null ? c.getEmail() : "");
                row.createCell(7).setCellValue(c.getAddress() != null ? c.getAddress() : "");
                row.createCell(8).setCellValue(Boolean.TRUE.equals(c.getActive()) ? "Yes" : "No");
                row.createCell(9).setCellValue(c.getCases().size());
                row.createCell(10).setCellValue(c.getDateOfBirth() != null ? c.getDateOfBirth().toString() : "");
                row.createCell(11).setCellValue(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
            }

            for (int i = 0; i < HEADERS.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate client Excel export", e);
        }
    }
}
```

**Step 2: Verify**
```bash
mvn clean compile
```

---

## Task 10: Presentation — `ClientController`

**Files:**
- Create: `backend/src/main/java/com/lawfirm/presentation/controller/ClientController.java`

**Step 1: Create controller**

```java
package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.request.CreateClientRequest;
import com.lawfirm.application.dto.request.UpdateClientRequest;
import com.lawfirm.application.dto.response.ClientResponse;
import com.lawfirm.application.dto.response.ClientSummary;
import com.lawfirm.application.service.ClientExportService;
import com.lawfirm.application.service.ClientService;
import com.lawfirm.domain.model.ClientType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Client management")
public class ClientController {

    private final ClientService clientService;
    private final ClientExportService clientExportService;

    @GetMapping
    @PreAuthorize("hasPermission(null, 'CLIENT_READ')")
    @Operation(summary = "Search/list clients")
    public ResponseEntity<Page<ClientSummary>> search(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) ClientType type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(clientService.search(search, type, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CLIENT_READ')")
    @Operation(summary = "Get client by ID")
    public ResponseEntity<ClientResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'CLIENT_CREATE')")
    @Operation(summary = "Create client")
    public ResponseEntity<ClientResponse> create(
        @Valid @RequestBody CreateClientRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CLIENT_UPDATE')")
    @Operation(summary = "Update client")
    public ResponseEntity<ClientResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateClientRequest request
    ) {
        return ResponseEntity.ok(clientService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'CLIENT_DELETE')")
    @Operation(summary = "Soft-deactivate client")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        clientService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    @PreAuthorize("hasPermission(null, 'CLIENT_READ')")
    @Operation(summary = "Export clients to Excel")
    public void export(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) ClientType type,
        HttpServletResponse response
    ) throws IOException {
        byte[] xlsx = clientExportService.export(search, type);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=clients-export.xlsx");
        response.getOutputStream().write(xlsx);
    }
}
```

**Step 2: Full backend build**
```bash
mvn clean compile
```
Expected: BUILD SUCCESS — all 10 new files compile cleanly.

---

## Task 11: Backend Integration Test — Start & Smoke Test

**Step 1: Start backend**
```bash
mvn spring-boot:run
```

**Step 2: Smoke test via curl or Swagger UI**
```bash
# POST a new individual client
curl -s -X POST http://localhost:8080/api/clients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "clientType": "INDIVIDUAL",
    "firstName": "Ahmed",
    "lastName": "Benali",
    "cin": "AB123456",
    "dateOfBirth": "1990-05-15",
    "phone": "+212600000001",
    "email": "ahmed@example.com"
  }'

# Expected: 201 Created, body contains id and fullName: "Ahmed Benali"

# GET list
curl -s http://localhost:8080/api/clients \
  -H "Authorization: Bearer <TOKEN>"

# Expected: 200 OK, page with one result
```

---

## Task 12: Frontend — TypeScript Model

**Files:**
- Create: `frontend/src/app/core/models/client.model.ts`

**Step 1: Create model**

```typescript
export type ClientType = 'INDIVIDUAL' | 'CORPORATE' | 'GOVERNMENT';
export type Gender = 'MALE' | 'FEMALE';

export interface ClientSummary {
  id: number;
  fullName: string;
  clientType: ClientType;
  cin?: string;
  phone?: string;
  email?: string;
  active: boolean;
  caseCount: number;
}

export interface ClientResponse {
  id: number;
  fullName: string;
  clientType: ClientType;
  firstName?: string;
  lastName?: string;
  phone?: string;
  email?: string;
  address?: string;
  notes?: string;
  cin?: string;
  gender?: Gender;
  dateOfBirth?: string;
  age?: number;
  companyName?: string;
  taxNumber?: string;
  active: boolean;
  caseCount: number;
  createdAt: string;
}

export interface CreateClientRequest {
  clientType: ClientType;
  firstName?: string;
  lastName?: string;
  phone?: string;
  email?: string;
  address?: string;
  notes?: string;
  cin?: string;
  gender?: Gender;
  dateOfBirth?: string;
  companyName?: string;
  taxNumber?: string;
}

export interface UpdateClientRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  email?: string;
  address?: string;
  notes?: string;
  cin?: string;
  gender?: Gender;
  dateOfBirth?: string;
  companyName?: string;
  taxNumber?: string;
}

export interface ClientSearchParams {
  search?: string;
  type?: ClientType;
  page?: number;
  size?: number;
}
```

**Step 2: Verify TypeScript**
```bash
cd frontend && pnpm exec tsc --noEmit
```

---

## Task 13: Frontend — `ClientService`

**Files:**
- Create: `frontend/src/app/services/client.service.ts`

**Step 1: Create Angular service**

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ClientSummary, ClientResponse,
  CreateClientRequest, UpdateClientRequest,
  ClientSearchParams
} from '../core/models/client.model';
import { PageResponse } from '../core/models/case.model';  // reuse existing PageResponse

@Injectable({ providedIn: 'root' })
export class ClientService {
  private http = inject(HttpClient);
  private readonly base = '/api/clients';

  search(params: ClientSearchParams): Observable<PageResponse<ClientSummary>> {
    let httpParams = new HttpParams();
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.type)   httpParams = httpParams.set('type', params.type);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    return this.http.get<PageResponse<ClientSummary>>(this.base, { params: httpParams });
  }

  getById(id: number): Observable<ClientResponse> {
    return this.http.get<ClientResponse>(`${this.base}/${id}`);
  }

  create(request: CreateClientRequest): Observable<ClientResponse> {
    return this.http.post<ClientResponse>(this.base, request);
  }

  update(id: number, request: UpdateClientRequest): Observable<ClientResponse> {
    return this.http.put<ClientResponse>(`${this.base}/${id}`, request);
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  exportUrl(search?: string, type?: string): string {
    let params = '';
    const parts: string[] = [];
    if (search) parts.push(`search=${encodeURIComponent(search)}`);
    if (type)   parts.push(`type=${type}`);
    if (parts.length) params = '?' + parts.join('&');
    return `${this.base}/export${params}`;
  }
}
```

**Step 2: Verify TypeScript**
```bash
pnpm exec tsc --noEmit
```

---

## Task 14: Frontend — `ClientListComponent`

**Files:**
- Create: `frontend/src/app/features/clients/client-list/client-list.component.ts`
- Create: `frontend/src/app/features/clients/client-list/client-list.component.html`

**Step 1: Create the component TypeScript**

```typescript
import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, debounceTime, takeUntil } from 'rxjs';
import { ClientService } from '../../../services/client.service';
import { AuthService } from '../../../core/services/auth.service';
import { ClientSummary, ClientResponse, ClientType, CreateClientRequest, UpdateClientRequest, PageResponse } from '../../../core/models/client.model';

@Component({
  selector: 'app-client-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './client-list.component.html',
})
export class ClientListComponent implements OnInit, OnDestroy {
  private clientService = inject(ClientService);
  authService = inject(AuthService);
  private destroy$ = new Subject<void>();

  Math = Math;

  // Data
  clients = signal<PageResponse<ClientSummary> | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);

  // Pagination
  page = signal(0);
  size = signal(20);

  // Filters
  search = signal('');
  typeFilter = signal<ClientType | ''>('');

  // Export
  exportLoading = signal(false);

  // Modal
  showModal = signal(false);
  editingClient = signal<ClientResponse | null>(null);
  modalLoading = signal(false);
  modalError = signal<string | null>(null);

  // Form
  form = signal<Partial<CreateClientRequest>>({ clientType: 'INDIVIDUAL' });

  readonly typeOptions: { value: ClientType; label: string }[] = [
    { value: 'INDIVIDUAL', label: 'Individual' },
    { value: 'CORPORATE', label: 'Corporate' },
    { value: 'GOVERNMENT', label: 'Government' },
  ];

  readonly genderOptions = [
    { value: 'MALE', label: 'Male' },
    { value: 'FEMALE', label: 'Female' },
  ];

  isIndividual = computed(() => this.form().clientType === 'INDIVIDUAL');
  isCorporate  = computed(() => this.form().clientType === 'CORPORATE' || this.form().clientType === 'GOVERNMENT');

  private searchSubject = new Subject<string>();

  ngOnInit(): void {
    this.searchSubject.pipe(debounceTime(300), takeUntil(this.destroy$))
      .subscribe(() => { this.page.set(0); this.loadClients(); });
    this.loadClients();
  }

  ngOnDestroy(): void {
    this.destroy$.next(); this.destroy$.complete();
  }

  loadClients(): void {
    this.loading.set(true);
    this.error.set(null);
    this.clientService.search({
      search: this.search() || undefined,
      type:   this.typeFilter() || undefined,
      page:   this.page(),
      size:   this.size(),
    }).subscribe({
      next:  (data) => { this.clients.set(data); this.loading.set(false); },
      error: (err)  => { this.error.set(err.error?.message ?? 'Failed to load clients'); this.loading.set(false); },
    });
  }

  onSearchChange(value: string): void {
    this.search.set(value);
    this.searchSubject.next(value);
  }

  onTypeFilterChange(value: string): void {
    this.typeFilter.set(value as ClientType | '');
    this.page.set(0);
    this.loadClients();
  }

  onPageChange(newPage: number): void {
    this.page.set(newPage);
    this.loadClients();
  }

  clearFilters(): void {
    this.search.set('');
    this.typeFilter.set('');
    this.page.set(0);
    this.loadClients();
  }

  // ── Modal ──────────────────────────────────────────────────────────────

  openCreate(): void {
    this.editingClient.set(null);
    this.form.set({ clientType: 'INDIVIDUAL' });
    this.modalError.set(null);
    this.showModal.set(true);
  }

  openEdit(id: number): void {
    this.modalLoading.set(true);
    this.showModal.set(true);
    this.modalError.set(null);
    this.clientService.getById(id).subscribe({
      next: (client) => {
        this.editingClient.set(client);
        this.form.set({
          clientType:   client.clientType,
          firstName:    client.firstName,
          lastName:     client.lastName,
          phone:        client.phone,
          email:        client.email,
          address:      client.address,
          notes:        client.notes,
          cin:          client.cin,
          gender:       client.gender,
          dateOfBirth:  client.dateOfBirth,
          companyName:  client.companyName,
          taxNumber:    client.taxNumber,
        });
        this.modalLoading.set(false);
      },
      error: (err) => {
        this.modalError.set(err.error?.message ?? 'Failed to load client');
        this.modalLoading.set(false);
      },
    });
  }

  closeModal(): void {
    this.showModal.set(false);
    this.editingClient.set(null);
    this.modalError.set(null);
  }

  onTypeChange(value: string): void {
    this.form.update(f => ({ ...f, clientType: value as ClientType }));
  }

  updateField(field: string, value: string): void {
    this.form.update(f => ({ ...f, [field]: value || undefined }));
  }

  saveClient(): void {
    this.modalLoading.set(true);
    this.modalError.set(null);
    const editing = this.editingClient();
    if (editing) {
      const req: UpdateClientRequest = {
        firstName:   this.form().firstName,
        lastName:    this.form().lastName,
        phone:       this.form().phone,
        email:       this.form().email,
        address:     this.form().address,
        notes:       this.form().notes,
        cin:         this.form().cin,
        gender:      this.form().gender,
        dateOfBirth: this.form().dateOfBirth,
        companyName: this.form().companyName,
        taxNumber:   this.form().taxNumber,
      };
      this.clientService.update(editing.id, req).subscribe({
        next:  () => { this.closeModal(); this.loadClients(); },
        error: (err) => { this.modalError.set(err.error?.message ?? 'Update failed'); this.modalLoading.set(false); },
      });
    } else {
      this.clientService.create(this.form() as CreateClientRequest).subscribe({
        next:  () => { this.closeModal(); this.loadClients(); },
        error: (err) => { this.modalError.set(err.error?.message ?? 'Create failed'); this.modalLoading.set(false); },
      });
    }
  }

  deactivateClient(id: number): void {
    if (!confirm('Deactivate this client?')) return;
    this.clientService.deactivate(id).subscribe({
      next:  () => this.loadClients(),
      error: (err) => this.error.set(err.error?.message ?? 'Deactivation failed'),
    });
  }

  exportClients(): void {
    this.exportLoading.set(true);
    const url = this.clientService.exportUrl(this.search() || undefined, this.typeFilter() || undefined);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'clients-export.xlsx';
    link.click();
    setTimeout(() => this.exportLoading.set(false), 1000);
  }

  typeBadgeClass(type: ClientType): string {
    const map: Record<ClientType, string> = {
      INDIVIDUAL: 'bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200',
      CORPORATE:  'bg-purple-100 dark:bg-purple-900 text-purple-800 dark:text-purple-200',
      GOVERNMENT: 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200',
    };
    return map[type] ?? '';
  }
}
```

**Step 2: Create the HTML template**

```html
<!-- client-list.component.html -->
<div class="p-6 space-y-4">

  <!-- Header -->
  <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Clients</h1>
    <div class="flex gap-2">
      @if (authService.hasPermission('CLIENT_READ')) {
        <button (click)="exportClients()" [disabled]="exportLoading()"
          class="px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700
                 disabled:opacity-50 flex items-center gap-2">
          <span class="material-icons text-base">download</span>
          {{ exportLoading() ? 'Exporting...' : 'Export' }}
        </button>
      }
      @if (authService.hasPermission('CLIENT_CREATE')) {
        <button (click)="openCreate()"
          class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium
                 rounded-lg flex items-center gap-2">
          <span class="material-icons text-base">add</span>
          New Client
        </button>
      }
    </div>
  </div>

  <!-- Filters -->
  <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
    <div class="flex flex-col sm:flex-row gap-3">
      <!-- Search -->
      <div class="flex-1 relative">
        <span class="material-icons absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-base">search</span>
        <input type="text" placeholder="Search by name, CIN, email..."
          [value]="search()"
          (input)="onSearchChange($any($event.target).value)"
          class="w-full pl-9 pr-4 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                 bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                 focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none" />
      </div>
      <!-- Type filter -->
      <select [value]="typeFilter()" (change)="onTypeFilterChange($any($event.target).value)"
        class="px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
               bg-white dark:bg-gray-700 text-gray-900 dark:text-white">
        <option value="">All Types</option>
        @for (t of typeOptions; track t.value) {
          <option [value]="t.value">{{ t.label }}</option>
        }
      </select>
      @if (search() || typeFilter()) {
        <button (click)="clearFilters()"
          class="px-3 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white">
          Clear
        </button>
      }
    </div>
  </div>

  <!-- Table -->
  <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
    @if (loading()) {
      <div class="flex justify-center items-center h-40">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
      </div>
    } @else if (error()) {
      <div class="p-6 text-center text-red-600 dark:text-red-400">{{ error() }}</div>
    } @else if (!clients()?.content?.length) {
      <div class="p-12 text-center text-gray-500 dark:text-gray-400">
        <span class="material-icons text-5xl mb-3 block">people_outline</span>
        No clients found.
      </div>
    } @else {
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 dark:bg-gray-700/50 border-b border-gray-200 dark:border-gray-700">
            <tr>
              <th class="px-4 py-3 text-left font-semibold text-gray-700 dark:text-gray-300">Full Name</th>
              <th class="px-4 py-3 text-left font-semibold text-gray-700 dark:text-gray-300">Type</th>
              <th class="px-4 py-3 text-left font-semibold text-gray-700 dark:text-gray-300">CIN / Tax No.</th>
              <th class="px-4 py-3 text-left font-semibold text-gray-700 dark:text-gray-300">Contact</th>
              <th class="px-4 py-3 text-center font-semibold text-gray-700 dark:text-gray-300">Cases</th>
              <th class="px-4 py-3 text-center font-semibold text-gray-700 dark:text-gray-300">Status</th>
              <th class="px-4 py-3 text-right font-semibold text-gray-700 dark:text-gray-300">Actions</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
            @for (client of clients()!.content; track client.id) {
              <tr class="hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors">
                <td class="px-4 py-3 font-medium text-gray-900 dark:text-white">{{ client.fullName }}</td>
                <td class="px-4 py-3">
                  <span class="px-2 py-0.5 text-xs font-semibold rounded-full" [class]="typeBadgeClass(client.clientType)">
                    {{ client.clientType }}
                  </span>
                </td>
                <td class="px-4 py-3 text-gray-600 dark:text-gray-400">
                  {{ client.cin || '—' }}
                </td>
                <td class="px-4 py-3 text-gray-600 dark:text-gray-400">
                  <div>{{ client.phone || '' }}</div>
                  <div class="text-xs text-gray-400">{{ client.email || '' }}</div>
                </td>
                <td class="px-4 py-3 text-center text-gray-600 dark:text-gray-400">{{ client.caseCount }}</td>
                <td class="px-4 py-3 text-center">
                  @if (client.active) {
                    <span class="px-2 py-0.5 text-xs font-semibold rounded-full bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200">Active</span>
                  } @else {
                    <span class="px-2 py-0.5 text-xs font-semibold rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400">Inactive</span>
                  }
                </td>
                <td class="px-4 py-3 text-right">
                  <div class="flex justify-end gap-1">
                    @if (authService.hasPermission('CLIENT_UPDATE')) {
                      <button (click)="openEdit(client.id)"
                        class="p-1.5 text-gray-500 hover:text-indigo-600 dark:text-gray-400 dark:hover:text-indigo-400 rounded">
                        <span class="material-icons text-base">edit</span>
                      </button>
                    }
                    @if (authService.hasPermission('CLIENT_DELETE') && client.active) {
                      <button (click)="deactivateClient(client.id)"
                        class="p-1.5 text-gray-500 hover:text-red-600 dark:text-gray-400 dark:hover:text-red-400 rounded">
                        <span class="material-icons text-base">person_off</span>
                      </button>
                    }
                  </div>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      @if (clients()!.totalPages > 1) {
        <div class="px-4 py-3 border-t border-gray-200 dark:border-gray-700 flex items-center justify-between">
          <span class="text-xs text-gray-500 dark:text-gray-400">
            {{ clients()!.totalElements }} clients — page {{ page() + 1 }} of {{ clients()!.totalPages }}
          </span>
          <div class="flex gap-2">
            <button (click)="onPageChange(page() - 1)" [disabled]="page() === 0"
              class="px-3 py-1 text-xs border border-gray-300 dark:border-gray-600 rounded
                     disabled:opacity-40 hover:bg-gray-50 dark:hover:bg-gray-700
                     text-gray-700 dark:text-gray-300">
              Prev
            </button>
            <button (click)="onPageChange(page() + 1)" [disabled]="page() + 1 >= clients()!.totalPages"
              class="px-3 py-1 text-xs border border-gray-300 dark:border-gray-600 rounded
                     disabled:opacity-40 hover:bg-gray-50 dark:hover:bg-gray-700
                     text-gray-700 dark:text-gray-300">
              Next
            </button>
          </div>
        </div>
      }
    }
  </div>
</div>

<!-- ── Modal ──────────────────────────────────────────────────────────────── -->
@if (showModal()) {
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
       (click)="closeModal()" (keydown.escape)="closeModal()">
    <div class="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto"
         (click)="$event.stopPropagation()">

      <div class="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
          {{ editingClient() ? 'Edit Client' : 'New Client' }}
        </h2>
        <button (click)="closeModal()" class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200">
          <span class="material-icons">close</span>
        </button>
      </div>

      @if (modalLoading()) {
        <div class="flex justify-center p-8">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
        </div>
      } @else {
        <div class="p-6 space-y-4">
          @if (modalError()) {
            <div class="p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-sm text-red-700 dark:text-red-400">
              {{ modalError() }}
            </div>
          }

          <!-- Client Type (create-only) -->
          @if (!editingClient()) {
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Client Type *</label>
              <select [value]="form().clientType" (change)="onTypeChange($any($event.target).value)"
                class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white">
                @for (t of typeOptions; track t.value) {
                  <option [value]="t.value">{{ t.label }}</option>
                }
              </select>
            </div>
          }

          <!-- INDIVIDUAL fields -->
          @if (isIndividual()) {
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">First Name *</label>
                <input type="text" [value]="form().firstName || ''"
                  (input)="updateField('firstName', $any($event.target).value)"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                         bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Last Name *</label>
                <input type="text" [value]="form().lastName || ''"
                  (input)="updateField('lastName', $any($event.target).value)"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                         bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">CIN *</label>
                <input type="text" [value]="form().cin || ''"
                  (input)="updateField('cin', $any($event.target).value)"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                         bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Gender</label>
                <select [value]="form().gender || ''" (change)="updateField('gender', $any($event.target).value)"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                         bg-white dark:bg-gray-700 text-gray-900 dark:text-white">
                  <option value="">—</option>
                  @for (g of genderOptions; track g.value) {
                    <option [value]="g.value">{{ g.label }}</option>
                  }
                </select>
              </div>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Date of Birth</label>
              <input type="date" [value]="form().dateOfBirth || ''"
                (input)="updateField('dateOfBirth', $any($event.target).value)"
                class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
            </div>
          }

          <!-- CORPORATE / GOVERNMENT fields -->
          @if (isCorporate()) {
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Company Name *</label>
              <input type="text" [value]="form().companyName || ''"
                (input)="updateField('companyName', $any($event.target).value)"
                class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Tax Number</label>
                <input type="text" [value]="form().taxNumber || ''"
                  (input)="updateField('taxNumber', $any($event.target).value)"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                         bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">First Name</label>
                <input type="text" [value]="form().firstName || ''"
                  (input)="updateField('firstName', $any($event.target).value)"
                  placeholder="Contact person"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                         bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
              </div>
            </div>
          }

          <!-- Common fields -->
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Phone</label>
              <input type="tel" [value]="form().phone || ''"
                (input)="updateField('phone', $any($event.target).value)"
                class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Email</label>
              <input type="email" [value]="form().email || ''"
                (input)="updateField('email', $any($event.target).value)"
                class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Address</label>
            <input type="text" [value]="form().address || ''"
              (input)="updateField('address', $any($event.target).value)"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                     bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Notes</label>
            <textarea rows="2" [value]="form().notes || ''"
              (input)="updateField('notes', $any($event.target).value)"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg
                     bg-white dark:bg-gray-700 text-gray-900 dark:text-white resize-none"></textarea>
          </div>
        </div>

        <div class="flex justify-end gap-3 px-6 pb-6">
          <button (click)="closeModal()"
            class="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 border border-gray-300 dark:border-gray-600
                   rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700">
            Cancel
          </button>
          <button (click)="saveClient()" [disabled]="modalLoading()"
            class="px-4 py-2 text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700
                   rounded-lg disabled:opacity-50">
            {{ editingClient() ? 'Save Changes' : 'Create Client' }}
          </button>
        </div>
      }
    </div>
  </div>
}
```

**Step 3: Verify TypeScript compilation**
```bash
pnpm exec tsc --noEmit
```

---

## Task 15: Frontend — Route & Sidebar

**Files:**
- Modify: `frontend/src/app/app.routes.ts`
- Modify: `frontend/src/app/features/layout/sidebar/sidebar.component.ts`

**Step 1: Add route to `app.routes.ts`**

Add inside the `children` array, after the `lawyers` route:

```typescript
{
  path: 'clients',
  loadComponent: () =>
    import('./features/clients/client-list/client-list.component')
      .then(m => m.ClientListComponent),
},
```

**Step 2: Add nav item to sidebar**

In `sidebar.component.ts`, add to the `navItems` array after the Lawyers entry:

```typescript
{ label: 'Clients', icon: 'people', route: '/clients', permission: 'CLIENT_READ' },
```

**Step 3: Verify build**
```bash
pnpm build
```
Expected: Build succeeds, new `/clients` chunk appears in output.

---

## Task 16: End-to-End Verification

**Step 1: Start full stack**
```bash
# Terminal 1
cd backend && mvn spring-boot:run

# Terminal 2
cd frontend && pnpm dev
```

**Step 2: Manual walkthrough**
1. Login as admin at `http://localhost:4200`
2. Click "Clients" in sidebar — list loads empty
3. Click "New Client" → set type=Individual, fill firstName/lastName/CIN/dateOfBirth → Save
4. Verify client appears in table
5. Click Edit — modal loads with existing data, change phone → Save Changes
6. Click deactivate icon — client disappears from active list
7. Set type filter to Corporate → click New Client → fill companyName → Save
8. Click Export → downloads `.xlsx` file

**Step 3: Backend lint check**
```bash
cd backend && mvn checkstyle:check
```

**Step 4: Frontend lint**
```bash
cd frontend && pnpm lint
```

---

## Execution Progress

| Batch | Tasks | Status | Date |
|-------|-------|--------|------|
| Batch 1 | Tasks 1–3: Flyway migrations (V41, V42, V43) | ✅ COMPLETED | 2026-02-27 |
| Batch 2 | Tasks 4–6: Domain entities, Repository, DTOs | ✅ COMPLETED | 2026-02-27 |
| Batch 3 | Tasks 7–9: Mapper, Service, ExportService | ✅ COMPLETED | 2026-02-27 |
| Batch 4 | Tasks 10–11: Controller, Backend smoke test | ✅ COMPLETED | 2026-02-27 |
| Batch 5 | Tasks 12–14: TS model, Angular service, ClientListComponent | ✅ COMPLETED | 2026-02-27 |
| Batch 6 | Tasks 15–16: Route/Sidebar, E2E verification | ✅ COMPLETED | 2026-02-27 |

---

## Summary — Files Created/Modified

| Action | File |
|--------|------|
| Create | `backend/.../db/migration/V41__create_clients_table.sql` |
| Create | `backend/.../db/migration/V42__seed_client_permissions.sql` |
| Create | `backend/.../db/migration/V43__add_client_id_to_cases.sql` |
| Create | `backend/.../domain/model/ClientType.java` |
| Create | `backend/.../domain/model/Gender.java` |
| Create | `backend/.../domain/model/Client.java` |
| Modify | `backend/.../domain/model/Case.java` (add `client` FK field) |
| Create | `backend/.../domain/repository/ClientRepository.java` |
| Create | `backend/.../dto/request/CreateClientRequest.java` |
| Create | `backend/.../dto/request/UpdateClientRequest.java` |
| Create | `backend/.../dto/response/ClientResponse.java` |
| Create | `backend/.../dto/response/ClientSummary.java` |
| Create | `backend/.../mapper/ClientMapper.java` |
| Create | `backend/.../service/ClientService.java` |
| Create | `backend/.../service/ClientExportService.java` |
| Create | `backend/.../controller/ClientController.java` |
| Create | `frontend/.../core/models/client.model.ts` |
| Create | `frontend/.../services/client.service.ts` |
| Create | `frontend/.../features/clients/client-list/client-list.component.ts` |
| Create | `frontend/.../features/clients/client-list/client-list.component.html` |
| Modify | `frontend/.../app.routes.ts` (add `/clients` route) |
| Modify | `frontend/.../sidebar/sidebar.component.ts` (add nav item) |
