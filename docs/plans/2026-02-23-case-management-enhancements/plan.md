# Case/Dossier Management Enhancements — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: use executing-plans skill to implement this plan task-by-task.

---

## Execution Progress — ✅ ALL 16 TASKS COMPLETE

| Task | Status | Notes |
|---|---|---|
| Task 1: Migrations V37–V39 | ✅ DONE | V37 (case_lawyers, case_templates), V38 (alter cases), V39 (PENAL type+categories+statuses) |
| Task 2: Enums | ✅ DONE | CasePriority.java, CaseOutcome.java created |
| Task 3: Case Entity | ✅ DONE | ManyToMany lawyers, 7 new fields added |
| Task 4: CaseTemplate Entity & Repo | ✅ DONE | CaseTemplate.java + CaseTemplateRepository.java |
| Task 5: DTOs | ✅ DONE | CreateCaseRequest, UpdateCaseRequest, CaseResponse updated; CaseSummaryResponse, CaseTemplateRequest, CaseTemplateResponse created |
| Task 6: CaseMapper | ✅ DONE | ManyToMany lawyers, parentCase mapping, toLawyerNames helper; CaseTemplateMapper created |
| Task 7: AuditLogService | ✅ DONE | log(), findByResource() added; BusinessRuleException created + handler in GlobalExceptionHandler |
| Task 8: CaseService | ✅ DONE | Multi-lawyer, new fields, status guards, delete guard, audit events, findChildren, findAllByCriteria |
| Task 9: CaseExportService | ✅ DONE | Apache POI added to pom.xml; CaseExportService.exportToExcel() created |
| Task 10: CaseTemplateService | ✅ DONE | CaseTemplateService.java created (findAll, create, delete) |
| Task 11: Controllers | ✅ DONE | CaseController: +export, +children, +history endpoints; CaseTemplateController created |
| Task 12: Frontend Types & CaseService | ✅ DONE | case.model.ts updated (CasePriority, CaseOutcome, new interfaces); CaseService: +exportCases, +getCaseChildren, +getCaseHistory, +getTemplates, +createTemplate, +deleteTemplate |
| Task 13: CaseListComponent | ✅ DONE | +priority filter, +Export Excel button, +priority badge column; CaseSummary DTO extended with priority |
| Task 14: CaseTemplatesComponent | ✅ DONE | New modal component created at features/cases/components/case-templates/ |
| Task 15: CaseFormComponent | ✅ DONE | Multi-lawyer checklist, new fields (priority, opposingParty, outcome, outcomeNotes, initialPaymentDate, fiscalYear), template modal integration |
| Task 16: CaseDetailComponent | ✅ DONE | Lawyers badges, new fields displayed, tabs for Audit/Children/History |

---

**Goal:** Implement 9 missing features and 3 partial fixes for the Case/Dossier Management module, bringing it from ~60% to 100% of specified requirements.

**Architecture:** Three Flyway migrations alter the schema (V37–V39), backend changes touch domain, application, and presentation layers following the existing hexagonal pattern, and the Angular frontend updates 3 existing components and adds 1 new modal component. All mapping is done via MapStruct; no manual mapping.

**Design doc:** `docs/plans/2026-02-23-case-management-enhancements/design.md`

**Tech Stack:** Spring Boot 3.4 (Java 21), Angular 18, Flyway, MapStruct, Tailwind CSS, Apache POI 5.3.0.

---

## Task 1: Database Migrations V37, V38, V39

**Files:**
- Create: `backend/src/main/resources/db/migration/V37__create_case_lawyers_and_templates.sql`
- Create: `backend/src/main/resources/db/migration/V38__enhance_cases_table.sql`
- Create: `backend/src/main/resources/db/migration/V39__seed_penal_case_type.sql`

> **IMPORTANT — Windows line endings:** After creating each `.sql` file, verify it uses LF (not CRLF). Run:
> `git add backend/src/main/resources/db/migration/V3*.sql` — git will normalize via `.gitattributes`.

**Step 1: Create V37 — new tables (must come before V38 which references them)**

```sql
-- V37__create_case_lawyers_and_templates.sql

-- Many-to-many: lawyers assigned to a case
CREATE TABLE case_lawyers (
    case_id   BIGINT NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    lawyer_id BIGINT NOT NULL REFERENCES lawyers(id),
    PRIMARY KEY (case_id, lawyer_id)
);

CREATE INDEX idx_case_lawyers_lawyer ON case_lawyers(lawyer_id);

-- Case templates: pre-fill type + category when creating a case
CREATE TABLE case_templates (
    id                 BIGSERIAL    PRIMARY KEY,
    name               VARCHAR(100) NOT NULL UNIQUE,
    case_type_code     VARCHAR(20)  NOT NULL,
    case_category_code VARCHAR(20)  NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT now()
);
```

**Step 2: Create V38 — alter `cases` table**

```sql
-- V38__enhance_cases_table.sql

-- 1. Migrate existing single lawyer to join table before dropping column
INSERT INTO case_lawyers (case_id, lawyer_id)
SELECT id, lawyer_id
FROM   cases
WHERE  lawyer_id IS NOT NULL;

-- 2. Drop old single-lawyer FK column
ALTER TABLE cases DROP COLUMN lawyer_id;

-- 3. Add all new columns
ALTER TABLE cases
    ADD COLUMN opposing_party       VARCHAR(255),
    ADD COLUMN outcome              VARCHAR(20),
    ADD COLUMN outcome_notes        TEXT,
    ADD COLUMN priority             VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN initial_payment_date DATE,
    ADD COLUMN fiscal_year          SMALLINT,
    ADD COLUMN parent_case_id       BIGINT REFERENCES cases(id);

-- 4. Indexes
CREATE INDEX idx_cases_priority ON cases(priority);
CREATE INDEX idx_cases_parent   ON cases(parent_case_id);
```

**Step 3: Create V39 — seed PENAL case type + its mappings**

```sql
-- V39__seed_penal_case_type.sql

-- Insert PENAL case type (bilingual)
INSERT INTO case_types (code, name_fr, name_ar, number_format_template)
VALUES ('PENAL', 'Pénal', 'جنائي', 'PENAL/{TRIBUNAL}/{YEAR}/{SEQUENCE}');

-- Map PENAL to relevant categories (reuse existing Criminal-related categories)
-- Use same category codes as CIVIL for now; adjust per business need
INSERT INTO case_type_categories (case_type_code, case_category_code)
SELECT 'PENAL', code FROM case_categories WHERE code IN ('1104', '8103');

-- Map PENAL to allowed statuses (all 7 statuses, same as CIVIL)
INSERT INTO case_type_statuses (case_type_code, case_status_code)
SELECT 'PENAL', code FROM case_statuses;
```

> **Note:** Confirm exact category codes and the join table names by checking V22 and V26 migrations before running.

**Step 4: Verify migrations compile**

```bash
cd backend
mvn flyway:validate -Dspring-boot.run.profiles=dev
# Or just start the app — Flyway runs on startup
mvn spring-boot:run
# Check logs: "Successfully applied 3 migrations" (V37, V38, V39)
```

Expected: Application starts, no Flyway checksum errors, `case_lawyers` and `case_templates` tables exist, `cases` table has 7 new columns.

---

## Task 2: New Enums — CasePriority & CaseOutcome

**Files:**
- Create: `backend/src/main/java/com/lawfirm/domain/model/CasePriority.java`
- Create: `backend/src/main/java/com/lawfirm/domain/model/CaseOutcome.java`

**Step 1: Create `CasePriority` enum**

```java
package com.lawfirm.domain.model;

public enum CasePriority {
    URGENT,
    HIGH,
    NORMAL,
    LOW
}
```

**Step 2: Create `CaseOutcome` enum**

```java
package com.lawfirm.domain.model;

public enum CaseOutcome {
    WON,
    LOST,
    SETTLED,
    DISMISSED
}
```

**Step 3: Verify compilation**

```bash
mvn compile
```

Expected: No errors.

---

## Task 3: Update Case Entity

**Files:**
- Modify: `backend/src/main/java/com/lawfirm/domain/model/Case.java`

**Step 1: Replace single-lawyer `@ManyToOne` with `@ManyToMany`**

Find this block in `Case.java`:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "lawyer_id")
private Lawyer lawyer;
```

Replace with:
```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "case_lawyers",
    joinColumns = @JoinColumn(name = "case_id"),
    inverseJoinColumns = @JoinColumn(name = "lawyer_id")
)
private Set<Lawyer> lawyers = new HashSet<>();
```

Add import: `import java.util.HashSet; import java.util.Set;`

**Step 2: Add 7 new fields** (after the existing `matterDescription` field):

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private CasePriority priority = CasePriority.NORMAL;

@Column(length = 255)
private String opposingParty;

@Enumerated(EnumType.STRING)
@Column(length = 20)
private CaseOutcome outcome;

@Column(columnDefinition = "TEXT")
private String outcomeNotes;

private LocalDate initialPaymentDate;

private Short fiscalYear;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_case_id")
private Case parentCase;
```

Add imports: `import com.lawfirm.domain.model.CasePriority; import com.lawfirm.domain.model.CaseOutcome;`

**Step 3: Verify entity compiles**

```bash
mvn compile
```

Expected: No errors. The removed `lawyer` field will cause MapStruct/service compilation errors — these are fixed in Tasks 5 & 7.

---

## Task 4: CaseTemplate Entity & Repository

**Files:**
- Create: `backend/src/main/java/com/lawfirm/domain/model/CaseTemplate.java`
- Create: `backend/src/main/java/com/lawfirm/domain/repository/CaseTemplateRepository.java`

**Step 1: Create `CaseTemplate` entity**

```java
package com.lawfirm.domain.model;

import com.lawfirm.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "case_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CaseTemplate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String caseTypeCode;

    @Column(nullable = false, length = 20)
    private String caseCategoryCode;
}
```

**Step 2: Create `CaseTemplateRepository`**

```java
package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.CaseTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaseTemplateRepository extends JpaRepository<CaseTemplate, Long> {
    boolean existsByName(String name);
}
```

**Step 3: Verify**

```bash
mvn compile
```

---

## Task 5: Update DTOs

**Files:**
- Modify: `backend/src/main/java/com/lawfirm/application/dto/request/CreateCaseRequest.java`
- Modify: `backend/src/main/java/com/lawfirm/application/dto/request/UpdateCaseRequest.java`
- Modify: `backend/src/main/java/com/lawfirm/application/dto/response/CaseResponse.java`
- Create: `backend/src/main/java/com/lawfirm/application/dto/response/CaseSummaryResponse.java`
- Create: `backend/src/main/java/com/lawfirm/application/dto/request/CaseTemplateRequest.java`
- Create: `backend/src/main/java/com/lawfirm/application/dto/response/CaseTemplateResponse.java`

**Step 1: Update `CreateCaseRequest`**

Replace `Long lawyerId` with `Set<Long> lawyerIds` and add new fields:

```java
// Replace:
@NotNull Long lawyerId,

// With:
@NotEmpty(message = "At least one lawyer must be assigned")
Set<Long> lawyerIds,

// Add after existing fields:
CasePriority priority,                        // defaults to NORMAL in service if null
@Size(max = 255) String opposingParty,
CaseOutcome outcome,
@Size(max = 1000) String outcomeNotes,
LocalDate initialPaymentDate,
Short fiscalYear,
Long parentCaseId
```

Add imports for `CasePriority`, `CaseOutcome`, `Set`, `jakarta.validation.constraints.NotEmpty`.

**Step 2: Apply same changes to `UpdateCaseRequest`**

Same as Step 1 — replace `lawyerId` with `lawyerIds` and add the 7 new fields.

**Step 3: Create `CaseSummaryResponse`** (used for `parentCase` field — avoids recursion)

```java
package com.lawfirm.application.dto.response;

public record CaseSummaryResponse(
    Long id,
    String fullCaseNumber
) {}
```

**Step 4: Update `CaseResponse`**

```java
// Replace:
LawyerResponse lawyer,

// With:
List<LawyerResponse> lawyers,

// Add after existing fields:
CasePriority priority,
String opposingParty,
CaseOutcome outcome,
String outcomeNotes,
LocalDate initialPaymentDate,
Short fiscalYear,
CaseSummaryResponse parentCase
```

Add necessary imports.

**Step 5: Create `CaseTemplateRequest`**

```java
package com.lawfirm.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CaseTemplateRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 20)  String caseTypeCode,
    @NotBlank @Size(max = 20)  String caseCategoryCode
) {}
```

**Step 6: Create `CaseTemplateResponse`**

```java
package com.lawfirm.application.dto.response;

public record CaseTemplateResponse(
    Long id,
    String name,
    String caseTypeCode,
    String caseCategoryCode
) {}
```

**Step 7: Verify**

```bash
mvn compile
```

Expected: Compilation errors only from `CaseMapper` (fixed in Task 6) and `CaseService` (fixed in Task 7).

---

## Task 6: Update CaseMapper (MapStruct)

**Files:**
- Modify: `backend/src/main/java/com/lawfirm/application/mapper/CaseMapper.java`
- Create: `backend/src/main/java/com/lawfirm/application/mapper/CaseTemplateMapper.java`

**Step 1: Fix `toResponse()` — update lawyer mapping**

In `CaseMapper.java`, update the `@Mapping` for lawyer to map the `Set<Lawyer>` to `List<LawyerResponse>`:

```java
// Remove any @Mapping for the old 'lawyer' field.
// Add:
@Mapping(target = "lawyers", source = "lawyers")
@Mapping(target = "parentCase", source = "parentCase", qualifiedByName = "toCaseSummary")
CaseResponse toResponse(Case entity);

@Named("toCaseSummary")
default CaseSummaryResponse toCaseSummary(Case parentCase) {
    if (parentCase == null) return null;
    return new CaseSummaryResponse(parentCase.getId(), parentCase.getFullCaseNumber());
}
```

MapStruct will automatically use the existing `LawyerMapper.toResponse(Lawyer)` to map each element of `Set<Lawyer>` to `LawyerResponse` if `LawyerMapper` is declared in `@Mapper(uses = {...})`. Verify `LawyerMapper` is in the `uses` list; add it if not.

Also add mappings for the 7 new fields (they map by name automatically since field names match, but confirm no conflicts):

```java
// These map automatically by name — no explicit @Mapping needed:
// priority, opposingParty, outcome, outcomeNotes, initialPaymentDate, fiscalYear
```

**Step 2: Update `toSummary()` if it references `lawyer`**

Check `CaseMapper.toSummary()`. If it maps a `lawyerName` from `case.getLawyer()`, update it to use the first lawyer in the set (or a comma-joined list):

```java
@Named("toLawyerNames")
default String toLawyerNames(Set<Lawyer> lawyers) {
    if (lawyers == null || lawyers.isEmpty()) return null;
    return lawyers.stream()
        .map(l -> l.getFirstName() + " " + l.getLastName())
        .collect(java.util.stream.Collectors.joining(", "));
}
// Then use: @Mapping(target = "lawyerName", source = "lawyers", qualifiedByName = "toLawyerNames")
```

**Step 3: Create `CaseTemplateMapper`**

```java
package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.request.CaseTemplateRequest;
import com.lawfirm.application.dto.response.CaseTemplateResponse;
import com.lawfirm.domain.model.CaseTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CaseTemplateMapper {
    CaseTemplateResponse toResponse(CaseTemplate entity);
    CaseTemplate toEntity(CaseTemplateRequest request);
}
```

**Step 4: Verify**

```bash
mvn compile
```

Expected: MapStruct annotation processor generates implementations with no errors.

---

## Task 7: Enhance AuditLogService

**Files:**
- Modify: `backend/src/main/java/com/lawfirm/application/service/AuditLogService.java`
- Modify: `backend/src/main/java/com/lawfirm/domain/repository/AuditLogRepository.java` (add query method)

**Step 1: Add `log()` method to `AuditLogService`**

The existing `AuditLogService` only has `getAllAuditLogs()`. Add a `log()` method and a `findByCase()` method:

```java
// Add to AuditLogService class:

private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
// Inject via constructor (Spring Boot auto-configures ObjectMapper as a bean)

public void log(String resource, Long resourceId, String action,
                List<String> changedFields, String username) {
    try {
        String metadata = objectMapper.writeValueAsString(
            Map.of("changedFields", changedFields)
        );
        AuditLog entry = AuditLog.builder()
            .resource(resource)
            .resourceId(resourceId.toString())
            .action(action)
            .username(username)
            .metadata(metadata)
            .build();
        auditLogRepository.save(entry);
    } catch (Exception e) {
        // Log and swallow — audit failure must not break the main operation
        log.warn("Failed to write audit log for {} {}: {}", resource, resourceId, e.getMessage());
    }
}

public List<AuditLogResponse> findByResource(String resource, Long resourceId) {
    return auditLogRepository
        .findByResourceAndResourceIdOrderByCreatedAtDesc(resource, resourceId.toString())
        .stream()
        .map(auditLogMapper::toResponse)
        .toList();
}
```

**Step 2: Add repository query method to `AuditLogRepository`**

```java
// Add to AuditLogRepository interface:
List<AuditLog> findByResourceAndResourceIdOrderByCreatedAtDesc(String resource, String resourceId);
```

**Step 3: Verify**

```bash
mvn compile
```

---

## Task 8: Update CaseService

**Files:**
- Modify: `backend/src/main/java/com/lawfirm/application/service/CaseService.java`

This is the most extensive backend change. Make changes in order.

**Step 1: Inject `LawyerRepository` and `AuditLogService`**

Add to constructor parameters (Spring will inject via constructor injection):
```java
private final LawyerRepository lawyerRepository;
private final AuditLogService auditLogService;
```

**Step 2: Update `createCase()`**

Replace single-lawyer lookup with multi-lawyer:
```java
// Remove:
Lawyer lawyer = lawyerRepository.findById(request.lawyerId())
    .orElseThrow(() -> new ResourceNotFoundException("Lawyer", request.lawyerId()));
case.setLawyer(lawyer);

// Replace with:
Set<Lawyer> lawyers = new HashSet<>(lawyerRepository.findAllById(request.lawyerIds()));
if (lawyers.size() != request.lawyerIds().size()) {
    throw new ResourceNotFoundException("One or more lawyers not found");
}
caseEntity.setLawyers(lawyers);

// Set new fields:
if (request.priority() != null) caseEntity.setPriority(request.priority());
else caseEntity.setPriority(CasePriority.NORMAL);
caseEntity.setOpposingParty(request.opposingParty());
caseEntity.setOutcome(request.outcome());
caseEntity.setOutcomeNotes(request.outcomeNotes());
caseEntity.setInitialPaymentDate(request.initialPaymentDate());
caseEntity.setFiscalYear(request.fiscalYear());

if (request.parentCaseId() != null) {
    Case parent = caseRepository.findById(request.parentCaseId())
        .orElseThrow(() -> new ResourceNotFoundException("Parent case", request.parentCaseId()));
    caseEntity.setParentCase(parent);
}
```

After saving, publish audit event:
```java
Case saved = caseRepository.save(caseEntity);
auditLogService.log("CASE", saved.getId(), "CASE_CREATED",
    List.of("fullCaseNumber", "priority", "lawyers", "status"), getCurrentUsername());
return caseMapper.toResponse(saved);
```

**Step 3: Update `updateCase()`**

Add field-diff tracking before applying changes:
```java
List<String> changedFields = new ArrayList<>();

// Example diffs (repeat for each field):
if (!Objects.equals(existing.getPriority(), request.priority()))
    changedFields.add("priority");
if (!Objects.equals(existing.getOpposingParty(), request.opposingParty()))
    changedFields.add("opposingParty");
if (!Objects.equals(existing.getOutcome(), request.outcome()))
    changedFields.add("outcome");
if (!Objects.equals(existing.getOutcomeNotes(), request.outcomeNotes()))
    changedFields.add("outcomeNotes");
if (!Objects.equals(existing.getInitialPaymentDate(), request.initialPaymentDate()))
    changedFields.add("initialPaymentDate");
if (!Objects.equals(existing.getFiscalYear(), request.fiscalYear()))
    changedFields.add("fiscalYear");

// Lawyer diff:
Set<Long> existingLawyerIds = existing.getLawyers().stream()
    .map(Lawyer::getId).collect(Collectors.toSet());
if (!existingLawyerIds.equals(new HashSet<>(request.lawyerIds())))
    changedFields.add("lawyers");

// Apply changes then save, then audit:
if (!changedFields.isEmpty()) {
    auditLogService.log("CASE", id, "CASE_UPDATED", changedFields, getCurrentUsername());
}
```

**Step 4: Update `changeStatus()`**

Add business rules BEFORE the existing validation:
```java
// Block ARCHIVED from any transition:
if (currentStatus.getCode().equals("ARCHIVED")) {
    throw new InvalidStatusTransitionException("ARCHIVED cases cannot change status.");
}
// Block CLOSED from going anywhere except ARCHIVED:
if (currentStatus.getCode().equals("CLOSED") && !newStatusCode.equals("ARCHIVED")) {
    throw new InvalidStatusTransitionException("CLOSED cases can only be archived.");
}
```

After saving, publish audit:
```java
auditLogService.log("CASE", id, "CASE_STATUS_CHANGED",
    List.of("status", "statusReason"), getCurrentUsername());
```

**Step 5: Update `deleteCase()`**

Add guard before soft-delete logic:
```java
if (caseEntity.getStatus().isTerminal()) {
    throw new BusinessRuleException(
        "Cases with status '" + caseEntity.getStatus().getCode()
        + "' cannot be deleted. Archive them instead."
    );
}
// ... existing soft delete code ...
auditLogService.log("CASE", id, "CASE_DELETED", List.of("deletedAt"), getCurrentUsername());
```

> **Note:** `BusinessRuleException` may not exist yet. If not, create it in the exception package extending `RuntimeException`, and add a `@ExceptionHandler` in `GlobalExceptionHandler` returning HTTP 409 Conflict.

**Step 6: Add `getHelper` method for current username**

```java
private String getCurrentUsername() {
    var auth = org.springframework.security.core.context.SecurityContextHolder
        .getContext().getAuthentication();
    return auth != null ? auth.getName() : "system";
}
```

**Step 7: Add `searchByParent()` method**

```java
public List<CaseSummaryResponse> findChildren(Long parentCaseId) {
    return caseRepository.findByParentCaseIdAndDeletedAtIsNull(parentCaseId)
        .stream()
        .map(c -> new CaseSummaryResponse(c.getId(), c.getFullCaseNumber()))
        .toList();
}
```

Add to `CaseRepository`:
```java
List<Case> findByParentCaseIdAndDeletedAtIsNull(Long parentCaseId);
```

**Step 8: Verify**

```bash
mvn compile
```

---

## Task 9: CaseExportService (Apache POI)

**Files:**
- Modify: `backend/pom.xml` (add POI dependency)
- Create: `backend/src/main/java/com/lawfirm/application/service/CaseExportService.java`

**Step 1: Add Apache POI dependency to `pom.xml`**

Inside the `<dependencies>` block:
```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.3.0</version>
</dependency>
```

**Step 2: Create `CaseExportService`**

```java
package com.lawfirm.application.service;

import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseExportService {

    private final CaseRepository caseRepository;

    private static final String[] HEADERS = {
        "Case Number", "Type", "Category", "Tribunal",
        "Lawyers", "Priority", "Status", "Outcome",
        "Opposing Party", "Registration Date", "Fiscal Year", "Initial Payment Date"
    };

    public byte[] exportToCases(/* same criteria used by CaseService.searchCases */ List<Case> cases) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Cases");

            // Header row
            Row header = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            for (Case c : cases) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.getFullCaseNumber());
                row.createCell(1).setCellValue(c.getCaseType() != null ? c.getCaseType().getNameFr() : "");
                row.createCell(2).setCellValue(c.getCaseCategory() != null ? c.getCaseCategory().getNameFr() : "");
                row.createCell(3).setCellValue(c.getTribunal() != null ? c.getTribunal().getNameFr() : "");
                row.createCell(4).setCellValue(
                    c.getLawyers().stream()
                        .map(l -> l.getFirstName() + " " + l.getLastName())
                        .collect(java.util.stream.Collectors.joining(", "))
                );
                row.createCell(5).setCellValue(c.getPriority() != null ? c.getPriority().name() : "");
                row.createCell(6).setCellValue(c.getStatus() != null ? c.getStatus().getCode() : "");
                row.createCell(7).setCellValue(c.getOutcome() != null ? c.getOutcome().name() : "");
                row.createCell(8).setCellValue(c.getOpposingParty() != null ? c.getOpposingParty() : "");
                row.createCell(9).setCellValue(c.getRegistrationDate() != null ? c.getRegistrationDate().toString() : "");
                row.createCell(10).setCellValue(c.getFiscalYear() != null ? c.getFiscalYear().toString() : "");
                row.createCell(11).setCellValue(c.getInitialPaymentDate() != null ? c.getInitialPaymentDate().toString() : "");
            }

            // Auto-size columns
            for (int i = 0; i < HEADERS.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export", e);
        }
    }
}
```

**Step 3: Verify**

```bash
mvn compile
```

---

## Task 10: CaseTemplateService

**Files:**
- Create: `backend/src/main/java/com/lawfirm/application/service/CaseTemplateService.java`

```java
package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.CaseTemplateRequest;
import com.lawfirm.application.dto.response.CaseTemplateResponse;
import com.lawfirm.application.mapper.CaseTemplateMapper;
import com.lawfirm.domain.repository.CaseTemplateRepository;
import com.lawfirm.infrastructure.exception.DuplicateResourceException;
import com.lawfirm.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaseTemplateService {

    private final CaseTemplateRepository templateRepository;
    private final CaseTemplateMapper templateMapper;

    public List<CaseTemplateResponse> findAll() {
        return templateRepository.findAll().stream()
            .map(templateMapper::toResponse)
            .toList();
    }

    @Transactional
    public CaseTemplateResponse create(CaseTemplateRequest request) {
        if (templateRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Template with name '" + request.name() + "' already exists");
        }
        return templateMapper.toResponse(
            templateRepository.save(templateMapper.toEntity(request))
        );
    }

    @Transactional
    public void delete(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new ResourceNotFoundException("CaseTemplate", id);
        }
        templateRepository.deleteById(id);
    }
}
```

> **Note:** Use the same exception class names that already exist in the project (`DuplicateResourceException`, `ResourceNotFoundException`). Check their package path in the infrastructure/exception directory.

**Step 1: Verify**

```bash
mvn compile
```

---

## Task 11: Update CaseController + New CaseTemplateController

**Files:**
- Modify: `backend/src/main/java/com/lawfirm/presentation/controller/CaseController.java`
- Create: `backend/src/main/java/com/lawfirm/presentation/controller/CaseTemplateController.java`

**Step 1: Add new endpoints to `CaseController`**

Inject `CaseExportService`, `AuditLogService` into the controller (constructor injection):

```java
// Add GET /api/cases/export
@GetMapping("/export")
@PreAuthorize("hasAuthority('CASE_READ')")
@Operation(summary = "Export cases to Excel")
public void exportCases(
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) String caseTypeCode,
        @RequestParam(required = false) String caseCategoryCode,
        @RequestParam(required = false) String tribunalCode,
        @RequestParam(required = false) Long lawyerId,
        @RequestParam(required = false) String statusCode,
        @RequestParam(required = false) String searchTerm,
        HttpServletResponse response) throws IOException {

    // Build same criteria as searchCases — reuse CaseSearchCriteria record
    var criteria = new CaseSearchCriteria(year, caseTypeCode, caseCategoryCode,
        tribunalCode, lawyerId, statusCode, null, null, searchTerm);

    // Fetch all (no pagination)
    List<Case> cases = caseService.findAllByCriteria(criteria);
    byte[] xlsx = caseExportService.exportToCases(cases);

    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=cases-export.xlsx");
    response.getOutputStream().write(xlsx);
}

// Add GET /api/cases/{id}/children
@GetMapping("/{id}/children")
@PreAuthorize("hasAuthority('CASE_READ')")
@Operation(summary = "Get child cases of a given case")
public ResponseEntity<List<CaseSummaryResponse>> getChildren(@PathVariable Long id) {
    return ResponseEntity.ok(caseService.findChildren(id));
}

// Add GET /api/cases/{id}/history
@GetMapping("/{id}/history")
@PreAuthorize("hasAuthority('CASE_READ')")
@Operation(summary = "Get audit history for a case")
public ResponseEntity<List<AuditLogResponse>> getHistory(@PathVariable Long id) {
    return ResponseEntity.ok(auditLogService.findByResource("CASE", id));
}
```

Also add `CaseService.findAllByCriteria()` in Task 8 if not yet added — it calls the specification with an unpaged `Pageable.unpaged()`.

**Step 2: Create `CaseTemplateController`**

```java
package com.lawfirm.presentation.controller;

import com.lawfirm.application.dto.request.CaseTemplateRequest;
import com.lawfirm.application.dto.response.CaseTemplateResponse;
import com.lawfirm.application.service.CaseTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cases/templates")
@RequiredArgsConstructor
@Tag(name = "Case Templates", description = "Manage case creation templates")
public class CaseTemplateController {

    private final CaseTemplateService templateService;

    @GetMapping
    @PreAuthorize("hasAuthority('CASE_READ')")
    @Operation(summary = "List all case templates")
    public ResponseEntity<List<CaseTemplateResponse>> findAll() {
        return ResponseEntity.ok(templateService.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CASE_CREATE')")
    @Operation(summary = "Create a case template")
    public ResponseEntity<CaseTemplateResponse> create(@Valid @RequestBody CaseTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CASE_DELETE')")
    @Operation(summary = "Delete a case template")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Step 3: Full backend verification**

```bash
cd backend
mvn clean compile
mvn spring-boot:run
# Test key endpoints via Swagger UI at http://localhost:8080/swagger-ui.html
# - POST /api/cases with lawyerIds array
# - GET /api/cases/export
# - GET /api/cases/{id}/history
# - GET /api/cases/templates
```

Expected: All endpoints respond correctly; Flyway migrations applied; no 500 errors.

---

## Task 12: Frontend — Update TypeScript Types & CaseService

**Files:**
- Modify: `frontend/src/app/services/case.service.ts`
- Modify or create: `frontend/src/app/models/case.model.ts` (or wherever Case interfaces are defined)

**Step 1: Update request/response interfaces**

Find where `CreateCaseRequest`, `UpdateCaseRequest`, `CaseResponse` are defined. Update:

```typescript
// Enums
export type CasePriority = 'URGENT' | 'HIGH' | 'NORMAL' | 'LOW';
export type CaseOutcome = 'WON' | 'LOST' | 'SETTLED' | 'DISMISSED';

// Request changes
export interface CreateCaseRequest {
  caseTypeCode: string;
  caseCategoryCode?: string;
  tribunalCode: string;
  lawyerIds: number[];          // was: lawyerId: number
  registrationDate: string;
  caseDescription: string;
  matterDescription?: string;
  initialStatusCode?: string;
  priority?: CasePriority;
  opposingParty?: string;
  outcome?: CaseOutcome;
  outcomeNotes?: string;
  initialPaymentDate?: string;
  fiscalYear?: number;
  parentCaseId?: number;
}

// Same changes for UpdateCaseRequest

// Response changes
export interface CaseSummaryResponse {
  id: number;
  fullCaseNumber: string;
}

export interface CaseResponse {
  // ... existing fields ...
  lawyers: LawyerResponse[];      // was: lawyer: LawyerResponse
  priority: CasePriority;
  opposingParty?: string;
  outcome?: CaseOutcome;
  outcomeNotes?: string;
  initialPaymentDate?: string;
  fiscalYear?: number;
  parentCase?: CaseSummaryResponse;
}

// New interfaces
export interface CaseTemplateResponse {
  id: number;
  name: string;
  caseTypeCode: string;
  caseCategoryCode: string;
}

export interface CaseTemplateRequest {
  name: string;
  caseTypeCode: string;
  caseCategoryCode: string;
}

export interface AuditLogResponse {
  id: number;
  action: string;
  resource: string;
  resourceId: string;
  username: string;
  metadata: string;   // JSON string: { changedFields: string[] }
  createdAt: string;
}
```

**Step 2: Add new methods to `CaseService`**

```typescript
// Add to CaseService class:

exportCases(params: CaseSearchParams): Observable<Blob> {
  const httpParams = this.buildHttpParams(params);
  return this.http.get('/api/cases/export', {
    params: httpParams,
    responseType: 'blob'
  });
}

getCaseChildren(id: number): Observable<CaseSummaryResponse[]> {
  return this.http.get<CaseSummaryResponse[]>(`/api/cases/${id}/children`);
}

getCaseHistory(id: number): Observable<AuditLogResponse[]> {
  return this.http.get<AuditLogResponse[]>(`/api/cases/${id}/history`);
}

// Template methods
getTemplates(): Observable<CaseTemplateResponse[]> {
  return this.http.get<CaseTemplateResponse[]>('/api/cases/templates');
}

createTemplate(request: CaseTemplateRequest): Observable<CaseTemplateResponse> {
  return this.http.post<CaseTemplateResponse>('/api/cases/templates', request);
}

deleteTemplate(id: number): Observable<void> {
  return this.http.delete<void>(`/api/cases/templates/${id}`);
}
```

**Step 3: Verify TypeScript compiles**

```bash
cd frontend
pnpm exec tsc --noEmit
```

---

## Task 13: Frontend — Update CaseListComponent

**Files:**
- Modify: `frontend/src/app/features/cases/case-list/case-list.component.ts`
- Modify: `frontend/src/app/features/cases/case-list/case-list.component.html`

**Step 1: Add `priority` filter signal and export signal**

In the `.ts` file:
```typescript
// Add to filter signals:
priority = signal<CasePriority | ''>('');
exportLoading = signal(false);

// Add priority options:
readonly priorityOptions: { value: CasePriority; label: string; cssClass: string }[] = [
  { value: 'URGENT', label: 'Urgent',  cssClass: 'bg-red-100 text-red-800' },
  { value: 'HIGH',   label: 'High',    cssClass: 'bg-orange-100 text-orange-800' },
  { value: 'NORMAL', label: 'Normal',  cssClass: 'bg-blue-100 text-blue-800' },
  { value: 'LOW',    label: 'Low',     cssClass: 'bg-gray-100 text-gray-800' },
];

getPriorityBadgeClass(priority: CasePriority): string {
  return this.priorityOptions.find(p => p.value === priority)?.cssClass ?? '';
}

exportCases(): void {
  this.exportLoading.set(true);
  this.caseService.exportCases(this.buildSearchParams()).subscribe({
    next: (blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'cases-export.xlsx';
      a.click();
      URL.revokeObjectURL(url);
      this.exportLoading.set(false);
    },
    error: () => this.exportLoading.set(false)
  });
}
```

**Step 2: Update `buildSearchParams()` to include `priority`**

```typescript
// Add to the params object:
...(this.priority() ? { priority: this.priority() } : {}),
```

**Step 3: Update HTML template**

Add priority filter dropdown (alongside existing filters):
```html
<!-- Priority filter -->
<select [(ngModel)]="priority" (change)="loadCases()" class="...">
  <option value="">All Priorities</option>
  <option *ngFor="let p of priorityOptions" [value]="p.value">{{ p.label }}</option>
</select>
```

Add Export button (top-right, next to Create button):
```html
<button (click)="exportCases()" [disabled]="exportLoading()"
        class="inline-flex items-center px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50">
  <span *ngIf="!exportLoading()">Export Excel</span>
  <span *ngIf="exportLoading()">Exporting...</span>
</button>
```

Add priority badge column in the table (after status badge column):
```html
<td class="px-4 py-3">
  <span [class]="getPriorityBadgeClass(case.priority)"
        class="px-2 py-1 rounded-full text-xs font-medium">
    {{ case.priority }}
  </span>
</td>
```

Also update the `lawyers` column — change from `case.lawyer?.fullName` to:
```html
<span *ngFor="let l of case.lawyers" class="inline-block mr-1 ...">
  {{ l.firstName }} {{ l.lastName }}
</span>
```

**Step 4: Verify**

```bash
cd frontend
pnpm exec tsc --noEmit && pnpm lint
```

---

## Task 14: Frontend — New CaseTemplatesComponent (Modal)

**Files:**
- Create: `frontend/src/app/features/cases/components/case-templates/case-templates.component.ts`
- Create: `frontend/src/app/features/cases/components/case-templates/case-templates.component.html`

**Step 1: Create the component**

```typescript
// case-templates.component.ts
import { Component, EventEmitter, OnInit, Output, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CaseService } from '../../../../services/case.service';
import { CaseTemplateResponse } from '../../../../models/case.model';

@Component({
  selector: 'app-case-templates',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './case-templates.component.html'
})
export class CaseTemplatesComponent implements OnInit {
  @Output() templateSelected = new EventEmitter<CaseTemplateResponse>();
  @Output() closed = new EventEmitter<void>();

  private caseService = inject(CaseService);

  templates = signal<CaseTemplateResponse[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    this.caseService.getTemplates().subscribe({
      next: (t) => { this.templates.set(t); this.loading.set(false); },
      error: ()  => this.loading.set(false)
    });
  }

  select(template: CaseTemplateResponse): void {
    this.templateSelected.emit(template);
    this.closed.emit();
  }
}
```

**Step 2: Create the HTML template**

```html
<!-- case-templates.component.html -->
<div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
  <div class="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-lg p-6">
    <div class="flex justify-between items-center mb-4">
      <h2 class="text-lg font-semibold text-gray-900 dark:text-white">Select a Template</h2>
      <button (click)="closed.emit()" class="text-gray-400 hover:text-gray-600">✕</button>
    </div>

    <div *ngIf="loading()" class="text-center py-8 text-gray-500">Loading templates...</div>

    <div *ngIf="!loading() && templates().length === 0" class="text-center py-8 text-gray-400">
      No templates found. Save a case as a template to get started.
    </div>

    <div *ngIf="!loading()" class="grid gap-3">
      <button *ngFor="let t of templates()"
              (click)="select(t)"
              class="flex items-center justify-between p-4 border border-gray-200 dark:border-gray-700
                     rounded-lg hover:border-indigo-500 hover:bg-indigo-50 dark:hover:bg-indigo-900/20
                     text-left transition-colors">
        <div>
          <p class="font-medium text-gray-900 dark:text-white">{{ t.name }}</p>
          <p class="text-sm text-gray-500">{{ t.caseTypeCode }} / {{ t.caseCategoryCode }}</p>
        </div>
        <span class="text-indigo-600 text-sm">Use →</span>
      </button>
    </div>
  </div>
</div>
```

---

## Task 15: Frontend — Update CaseFormComponent

**Files:**
- Modify: `frontend/src/app/features/cases/case-form/case-form.component.ts`
- Modify: `frontend/src/app/features/cases/case-form/case-form.component.html`

**Step 1: Update TypeScript**

```typescript
// Add signals:
showTemplateModal = signal(false);
templateSaveLoading = signal(false);

// Multi-lawyer: replace single lawyer signal with multi-select array
selectedLawyerIds = signal<number[]>([]);

// New field signals (or add to reactive form):
// If using reactive form, add controls:
// priority, opposingParty, outcome, outcomeNotes, initialPaymentDate, fiscalYear, parentCaseId

// Template handling:
applyTemplate(template: CaseTemplateResponse): void {
  this.form.patchValue({
    caseTypeCode: template.caseTypeCode,
    caseCategoryCode: template.caseCategoryCode
  });
}

saveAsTemplate(): void {
  const name = prompt('Template name:');
  if (!name) return;
  const req: CaseTemplateRequest = {
    name,
    caseTypeCode: this.form.value.caseTypeCode,
    caseCategoryCode: this.form.value.caseCategoryCode
  };
  this.caseService.createTemplate(req).subscribe({
    next: () => alert('Template saved!'),
    error: (e) => alert('Error: ' + e.message)
  });
}

// Update form submit to send lawyerIds array:
// request.lawyerIds = this.selectedLawyerIds();
```

**Step 2: Update HTML template**

Replace single lawyer `<select>` with a multi-select checklist:
```html
<!-- Multi-lawyer selection -->
<div class="space-y-2">
  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
    Lawyers <span class="text-red-500">*</span>
  </label>
  <div class="border border-gray-300 rounded-lg max-h-40 overflow-y-auto p-2 space-y-1">
    <label *ngFor="let lawyer of lawyers()" class="flex items-center gap-2 cursor-pointer">
      <input type="checkbox" [value]="lawyer.id"
             [checked]="selectedLawyerIds().includes(lawyer.id)"
             (change)="toggleLawyer(lawyer.id)"
             class="rounded border-gray-300">
      <span class="text-sm text-gray-700 dark:text-gray-300">
        {{ lawyer.firstName }} {{ lawyer.lastName }}
      </span>
    </label>
  </div>
</div>
```

Add new fields after existing form fields:
```html
<!-- Priority -->
<div>
  <label class="block text-sm font-medium ...">Priority</label>
  <select formControlName="priority" class="...">
    <option value="URGENT">Urgent</option>
    <option value="HIGH">High</option>
    <option value="NORMAL">Normal</option>
    <option value="LOW">Low</option>
  </select>
</div>

<!-- Opposing Party -->
<div>
  <label class="block text-sm font-medium ...">Opposing Party</label>
  <input type="text" formControlName="opposingParty" maxlength="255" class="...">
</div>

<!-- Outcome (show only when status = CLOSED or ARCHIVED) -->
<ng-container *ngIf="isTerminalStatus()">
  <div>
    <label class="block text-sm font-medium ...">Outcome</label>
    <select formControlName="outcome" class="...">
      <option value="">-- Select outcome --</option>
      <option value="WON">Won</option>
      <option value="LOST">Lost</option>
      <option value="SETTLED">Settled</option>
      <option value="DISMISSED">Dismissed</option>
    </select>
  </div>
  <div *ngIf="form.value.outcome">
    <label class="block text-sm font-medium ...">Outcome Notes</label>
    <textarea formControlName="outcomeNotes" rows="3" maxlength="1000" class="..."></textarea>
  </div>
</ng-container>

<!-- Initial Payment Date -->
<div>
  <label class="block text-sm font-medium ...">Initial Payment Date</label>
  <input type="date" formControlName="initialPaymentDate" class="...">
</div>

<!-- Fiscal Year -->
<div>
  <label class="block text-sm font-medium ...">Fiscal Year</label>
  <input type="number" formControlName="fiscalYear" min="2000" max="2099" class="...">
</div>

<!-- Parent Case (typeahead) -->
<div>
  <label class="block text-sm font-medium ...">Linked to Case (Parent)</label>
  <input type="text" [formControl]="parentCaseSearch" placeholder="Search by case number..."
         class="..." (input)="searchParentCases($event)">
  <!-- results dropdown -->
</div>

<!-- Template buttons (create mode only) -->
<div *ngIf="!isEditMode" class="flex gap-2">
  <button type="button" (click)="showTemplateModal.set(true)"
          class="text-sm text-indigo-600 hover:underline">Use Template</button>
  <button type="button" (click)="saveAsTemplate()"
          class="text-sm text-gray-500 hover:underline">Save as Template</button>
</div>

<!-- Template modal -->
<app-case-templates *ngIf="showTemplateModal()"
  (templateSelected)="applyTemplate($event)"
  (closed)="showTemplateModal.set(false)">
</app-case-templates>
```

**Step 3: Verify**

```bash
cd frontend
pnpm exec tsc --noEmit
```

---

## Task 16: Frontend — Update CaseDetailComponent

**Files:**
- Modify: `frontend/src/app/features/cases/case-detail/case-detail.component.ts`
- Modify: `frontend/src/app/features/cases/case-detail/case-detail.component.html`

**Step 1: Add new data signals**

```typescript
children = signal<CaseSummaryResponse[]>([]);
history = signal<AuditLogResponse[]>([]);
activeTab = signal<'details' | 'history' | 'children'>('details');

ngOnInit(): void {
  // ... existing case load ...
  const id = this.route.snapshot.paramMap.get('id')!;
  this.caseService.getCaseChildren(+id).subscribe(c => this.children.set(c));
  this.caseService.getCaseHistory(+id).subscribe(h => this.history.set(h));
}

parseChangedFields(metadata: string): string[] {
  try {
    return JSON.parse(metadata)?.changedFields ?? [];
  } catch { return []; }
}
```

**Step 2: Update HTML template**

Replace single lawyer display with a list of badges:
```html
<!-- Lawyers -->
<div class="flex flex-wrap gap-2">
  <span *ngFor="let l of case().lawyers"
        class="px-3 py-1 bg-indigo-100 text-indigo-800 dark:bg-indigo-900 dark:text-indigo-200
               rounded-full text-sm font-medium">
    {{ l.firstName }} {{ l.lastName }}
  </span>
</div>
```

Add new fields in the info grid:
```html
<div class="grid grid-cols-2 gap-4 mt-4">
  <!-- Priority -->
  <div>
    <p class="text-xs text-gray-500">Priority</p>
    <span [class]="getPriorityClass(case().priority)" class="px-2 py-1 rounded-full text-xs">
      {{ case().priority }}
    </span>
  </div>

  <!-- Opposing Party -->
  <div *ngIf="case().opposingParty">
    <p class="text-xs text-gray-500">Opposing Party</p>
    <p class="text-sm font-medium text-gray-900 dark:text-white">{{ case().opposingParty }}</p>
  </div>

  <!-- Outcome -->
  <div *ngIf="case().outcome">
    <p class="text-xs text-gray-500">Outcome</p>
    <p class="text-sm font-medium text-gray-900 dark:text-white">{{ case().outcome }}</p>
  </div>

  <!-- Fiscal Year -->
  <div *ngIf="case().fiscalYear">
    <p class="text-xs text-gray-500">Fiscal Year</p>
    <p class="text-sm font-medium">{{ case().fiscalYear }}</p>
  </div>

  <!-- Initial Payment Date -->
  <div *ngIf="case().initialPaymentDate">
    <p class="text-xs text-gray-500">Initial Payment</p>
    <p class="text-sm font-medium">{{ case().initialPaymentDate | date }}</p>
  </div>

  <!-- Parent Case -->
  <div *ngIf="case().parentCase">
    <p class="text-xs text-gray-500">Parent Case</p>
    <a [routerLink]="['/cases', case().parentCase!.id]"
       class="text-sm font-medium text-indigo-600 hover:underline">
      {{ case().parentCase!.fullCaseNumber }}
    </a>
  </div>
</div>
```

Add tabs for History and Related Cases:
```html
<!-- Tab navigation -->
<div class="border-b border-gray-200 dark:border-gray-700 mt-6">
  <nav class="flex gap-4">
    <button (click)="activeTab.set('details')" [class.border-indigo-500]="activeTab() === 'details'"
            class="pb-2 border-b-2 text-sm font-medium">Details</button>
    <button (click)="activeTab.set('children')" [class.border-indigo-500]="activeTab() === 'children'"
            class="pb-2 border-b-2 text-sm font-medium">
      Related Cases ({{ children().length }})
    </button>
    <button (click)="activeTab.set('history')" [class.border-indigo-500]="activeTab() === 'history'"
            class="pb-2 border-b-2 text-sm font-medium">History</button>
  </nav>
</div>

<!-- Children tab -->
<div *ngIf="activeTab() === 'children'" class="mt-4 space-y-2">
  <div *ngIf="children().length === 0" class="text-sm text-gray-400">No linked cases.</div>
  <a *ngFor="let child of children()" [routerLink]="['/cases', child.id]"
     class="block p-3 border rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 text-sm
            text-indigo-600 font-medium">
    {{ child.fullCaseNumber }}
  </a>
</div>

<!-- History tab -->
<div *ngIf="activeTab() === 'history'" class="mt-4 space-y-3">
  <div *ngIf="history().length === 0" class="text-sm text-gray-400">No history recorded.</div>
  <div *ngFor="let entry of history()"
       class="flex gap-3 p-3 border border-gray-100 dark:border-gray-700 rounded-lg">
    <div class="flex-1">
      <p class="text-sm font-medium text-gray-900 dark:text-white">{{ entry.action }}</p>
      <p class="text-xs text-gray-500">
        by {{ entry.username }} · {{ entry.createdAt | date:'medium' }}
      </p>
      <div *ngIf="parseChangedFields(entry.metadata).length > 0"
           class="mt-1 flex flex-wrap gap-1">
        <span *ngFor="let f of parseChangedFields(entry.metadata)"
              class="px-2 py-0.5 bg-gray-100 dark:bg-gray-700 text-xs rounded">
          {{ f }}
        </span>
      </div>
    </div>
  </div>
</div>
```

**Step 3: Final full-stack verification**

```bash
# Backend
cd backend
mvn clean verify
# Expected: BUILD SUCCESS, all tests pass

# Frontend
cd frontend
pnpm exec tsc --noEmit && pnpm lint
# Expected: no errors

# Manual smoke test:
# 1. Create a case with 2 lawyers — verify both appear
# 2. Change status to CLOSED, then ARCHIVED — verify ARCHIVED blocks further changes
# 3. Try deleting a CLOSED case — expect 409 error
# 4. Export cases — verify .xlsx downloads with correct data
# 5. Create a template, then use it in the create form
# 6. Check case history tab after updates
```

---

## Commit Order

Commit after each logical group passes verification:

```bash
git add backend/src/main/resources/db/migration/V3*.sql
git commit -m "feat(db): add case_lawyers, case_templates tables and PENAL type (V37-V39)"

git add backend/src/main/java/com/lawfirm/domain/
git commit -m "feat(domain): add CasePriority, CaseOutcome enums; update Case and add CaseTemplate entity"

git add backend/src/main/java/com/lawfirm/application/
git commit -m "feat(application): update case DTOs, mapper, services for enhancements"

git add backend/src/main/java/com/lawfirm/presentation/
git commit -m "feat(api): add export, children, history, template endpoints"

git add frontend/
git commit -m "feat(frontend): update case list, form, detail; add template modal and export"
```
