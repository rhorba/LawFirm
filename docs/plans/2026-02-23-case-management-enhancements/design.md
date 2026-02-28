# Case/Dossier Management — Enhancement Design

**Date:** 2026-02-23
**Status:** Validated
**Scope:** 9 missing features + 3 partial fixes

---

## What We're Building

Filling the gaps in the existing Case/Dossier Management feature. The current implementation covers ~60% of requirements. This design covers the remaining 40%.

---

## Design Decisions (Q&A Summary)

| Feature | Decision |
|---|---|
| Multiple lawyers | ManyToMany, no roles — simple join table |
| Opposing party | Single text field `opposingParty` on case |
| Case outcome | Enum (WON/LOST/SETTLED/DISMISSED) + `outcomeNotes` text |
| Priority | Simple enum label (URGENT/HIGH/NORMAL/LOW), no workflow impact |
| Payment date | `initialPaymentDate` DATE field on the case |
| Fiscal year | Single `fiscalYear` SMALLINT field |
| Linked cases | `parentCaseId` self-referential FK (one parent only) |
| Excel export | Export current filtered results as .xlsx |
| Case templates | Pre-fill type + category only; stored in DB |
| Audit trail | Actions + list of changed field names (no values) |
| PENAL type | Code `PENAL`, FR "Pénal", AR "جنائي" |
| Closed case protection | CLOSED → archive only; ARCHIVED → no delete |

---

## Section 1 — Database Schema

### Migration V37 — Alter `cases` table

```sql
-- Migrate existing single lawyer before dropping column
INSERT INTO case_lawyers (case_id, lawyer_id)
SELECT id, lawyer_id FROM cases WHERE lawyer_id IS NOT NULL;

ALTER TABLE cases DROP COLUMN lawyer_id;

ALTER TABLE cases
  ADD COLUMN opposing_party      VARCHAR(255),
  ADD COLUMN outcome             VARCHAR(20),
  ADD COLUMN outcome_notes       TEXT,
  ADD COLUMN priority            VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  ADD COLUMN initial_payment_date DATE,
  ADD COLUMN fiscal_year         SMALLINT,
  ADD COLUMN parent_case_id      BIGINT REFERENCES cases(id);

CREATE INDEX idx_cases_priority ON cases(priority);
CREATE INDEX idx_cases_parent   ON cases(parent_case_id);
```

### Migration V38 — New tables

```sql
-- Many-to-many: lawyers assigned to a case
CREATE TABLE case_lawyers (
  case_id   BIGINT NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
  lawyer_id BIGINT NOT NULL REFERENCES lawyers(id),
  PRIMARY KEY (case_id, lawyer_id)
);

CREATE INDEX idx_case_lawyers_lawyer ON case_lawyers(lawyer_id);

-- Case templates (type + category preset)
CREATE TABLE case_templates (
  id                 BIGSERIAL    PRIMARY KEY,
  name               VARCHAR(100) NOT NULL UNIQUE,
  case_type_code     VARCHAR(20)  NOT NULL,
  case_category_code VARCHAR(20)  NOT NULL,
  created_at         TIMESTAMP    NOT NULL DEFAULT now(),
  updated_at         TIMESTAMP    NOT NULL DEFAULT now()
);
```

### Migration V39 — Seed PENAL case type

```sql
INSERT INTO case_types (code, name_fr, name_ar, number_format_template)
VALUES ('PENAL', 'Pénal', 'جنائي', 'PENAL/{TRIBUNAL}/{YEAR}/{SEQUENCE}');

-- Add PENAL → category mappings following existing V22 pattern
-- Add PENAL → status mappings following existing V26 pattern
```

---

## Section 2 — Backend: Entities, DTOs & Services

### New Enums

```java
public enum CasePriority { URGENT, HIGH, NORMAL, LOW }
public enum CaseOutcome  { WON, LOST, SETTLED, DISMISSED }
```

### `Case.java` entity changes

```java
// Replace single @ManyToOne lawyer with:
@ManyToMany
@JoinTable(
  name = "case_lawyers",
  joinColumns = @JoinColumn(name = "case_id"),
  inverseJoinColumns = @JoinColumn(name = "lawyer_id")
)
private Set<Lawyer> lawyers = new HashSet<>();

// New fields:
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private CasePriority priority = CasePriority.NORMAL;

private String opposingParty;

@Enumerated(EnumType.STRING)
private CaseOutcome outcome;

@Column(columnDefinition = "TEXT")
private String outcomeNotes;

private LocalDate initialPaymentDate;

private Short fiscalYear;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_case_id")
private Case parentCase;
```

### New `CaseTemplate.java` entity

```java
@Entity
@Table(name = "case_templates")
public class CaseTemplate extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String caseTypeCode;

    @Column(nullable = false, length = 20)
    private String caseCategoryCode;
}
```

### DTO changes

**`CreateCaseRequest` / `UpdateCaseRequest`:**
- Replace `Long lawyerId` → `@NotEmpty Set<Long> lawyerIds`
- Add: `CasePriority priority` (default NORMAL)
- Add: `String opposingParty` (max 255)
- Add: `CaseOutcome outcome`
- Add: `String outcomeNotes` (max 1000)
- Add: `LocalDate initialPaymentDate`
- Add: `Short fiscalYear`
- Add: `Long parentCaseId`

**`CaseResponse`:**
- Replace `LawyerSummaryResponse lawyer` → `List<LawyerSummaryResponse> lawyers`
- Add all new fields
- Add: `CaseSummaryResponse parentCase` (id + fullCaseNumber only — no recursion)

**New DTOs:**
- `CaseTemplateRequest` (name, caseTypeCode, caseCategoryCode)
- `CaseTemplateResponse` (id, name, caseTypeCode, caseCategoryCode)

### `CaseService` rule changes

```java
// deleteCase() — block terminal statuses
if (case.getStatus().isTerminal()) {
    throw new BusinessRuleException(
        "Cases with status " + case.getStatus().getCode() + " cannot be deleted. Archive them instead."
    );
}

// changeStatus() — CLOSED can only go to ARCHIVED
if (current.getCode().equals("CLOSED") && !next.getCode().equals("ARCHIVED")) {
    throw new InvalidStatusTransitionException("CLOSED cases can only be archived.");
}
// ARCHIVED is final
if (current.getCode().equals("ARCHIVED")) {
    throw new InvalidStatusTransitionException("ARCHIVED cases cannot change status.");
}

// createCase() / updateCase() — resolve lawyer IDs
Set<Lawyer> lawyers = new HashSet<>(lawyerRepository.findAllById(request.getLawyerIds()));
if (lawyers.size() != request.getLawyerIds().size()) {
    throw new ResourceNotFoundException("One or more lawyers not found");
}
case.setLawyers(lawyers);
```

### New `CaseTemplateService`

Simple CRUD: `findAll()`, `create()`, `delete()`. No business logic beyond uniqueness check on name.

---

## Section 3 — API Endpoints

### Modified endpoints (existing `CaseController`)

```
POST   /api/cases              – lawyerIds replaces lawyerId; all new fields
PUT    /api/cases/{id}         – same
DELETE /api/cases/{id}         – 409 Conflict if status = CLOSED or ARCHIVED
PATCH  /api/cases/{id}/status  – enforces CLOSED→ARCHIVED only; ARCHIVED is terminal
```

### New endpoints (same `CaseController`)

```
GET /api/cases/export        – Excel download (same filter params as list, no pagination)
GET /api/cases/{id}/children – List<CaseSummaryResponse> where parent_case_id = {id}
GET /api/cases/{id}/history  – List<AuditLogResponse> (CASE_READ permission)
```

### New `CaseTemplateController` (`/api/cases/templates`)

```
GET    /api/cases/templates        – list all (CASE_READ)
POST   /api/cases/templates        – create (CASE_CREATE)
DELETE /api/cases/templates/{id}   – delete (CASE_DELETE)
```

### Excel export

**Dependency** (`pom.xml`):
```xml
<dependency>
  <groupId>org.apache.poi</groupId>
  <artifactId>poi-ooxml</artifactId>
  <version>5.3.0</version>
</dependency>
```

**`CaseExportService`:** Takes same `CaseSearchCriteria`, fetches all matching rows (no pagination limit), writes `.xlsx`.

**Columns:**
Case Number | Type | Category | Tribunal | Lawyers | Priority | Status | Outcome | Opposing Party | Registration Date | Fiscal Year | Initial Payment Date

**Response headers:**
```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename=cases-export.xlsx
```

No new permission — reuses `CASE_READ`.

---

## Section 4 — Audit Trail

### Wiring into `CaseService`

Four actions to publish after each operation:

| Action | Trigger | Changed Fields |
|---|---|---|
| `CASE_CREATED` | after `createCase()` | all non-null fields |
| `CASE_UPDATED` | after `updateCase()` | only fields that actually changed |
| `CASE_STATUS_CHANGED` | after `changeStatus()` | `["status", "statusReason"]` |
| `CASE_DELETED` | after `deleteCase()` | `["deletedAt"]` |

### Field diff pattern

```java
List<String> changedFields = new ArrayList<>();
if (!Objects.equals(existing.getPriority(), request.getPriority()))
    changedFields.add("priority");
if (!Objects.equals(existing.getOpposingParty(), request.getOpposingParty()))
    changedFields.add("opposingParty");
if (!existing.getLawyers().equals(resolvedLawyers))
    changedFields.add("lawyers");
// ... repeat for each field

auditLogService.log("CASE", caseId, "CASE_UPDATED", changedFields, currentUser);
```

`changedFields` stored as JSON array string in existing `AuditLog.details` column — **no schema change needed**.

---

## Section 5 — Frontend (Angular)

### Modified `CaseFormComponent`

New form controls:

| Field | Control | Visibility |
|---|---|---|
| `priority` | `<select>` URGENT/HIGH/NORMAL/LOW | Always, default NORMAL |
| `opposingParty` | `<input>` max 255 | Always |
| `outcome` | `<select>` | Only when status = CLOSED or ARCHIVED |
| `outcomeNotes` | `<textarea>` | Only when outcome is set |
| `initialPaymentDate` | `<input type="date">` | Always |
| `fiscalYear` | `<input type="number">` 4-digit | Always |
| `parentCaseId` | typeahead `<select>` | Always |
| `lawyerIds` | multi-select checklist | Always, replaces single lawyer |

"Save as template" button (create mode only) → name input dialog → `POST /api/cases/templates`.

### Modified `CaseListComponent`

- Add `priority` filter dropdown
- Add priority badge column (URGENT=red, HIGH=orange, NORMAL=blue, LOW=gray)
- Add **Export** button → calls `GET /api/cases/export` with current active filters → triggers browser file download

### New `CaseTemplatesComponent` (modal)

- Triggered by "Use Template" button on create form
- Shows saved templates as clickable cards (name, type, category)
- On select: pre-fills `caseTypeCode` + `caseCategoryCode`, closes modal

### Modified `CaseDetailComponent`

- Lawyers: list of badges (replacing single lawyer)
- New fields: priority badge, opposing party, outcome + notes, fiscal year, initial payment date
- Parent case: clickable link → navigates to parent case detail
- Related cases section: loads `GET /api/cases/{id}/children`
- History tab: audit log timeline from `GET /api/cases/{id}/history`

**No new routes needed.**

---

## Implementation Checklist

### Backend
- [ ] V37 migration — alter `cases` table + data migration for existing lawyer_id
- [ ] V38 migration — `case_lawyers` + `case_templates` tables
- [ ] V39 migration — seed PENAL case type + mappings
- [ ] `CasePriority` + `CaseOutcome` enums
- [ ] Update `Case` entity (new fields, ManyToMany lawyers)
- [ ] `CaseTemplate` entity + repository
- [ ] Update `CreateCaseRequest` / `UpdateCaseRequest` DTOs
- [ ] Update `CaseResponse` DTO
- [ ] `CaseTemplateRequest` / `CaseTemplateResponse` DTOs
- [ ] Update `CaseMapper` (MapStruct)
- [ ] Update `CaseService` (lawyer resolution, delete guard, status guard)
- [ ] `CaseTemplateService` (CRUD)
- [ ] `CaseExportService` (POI xlsx)
- [ ] Wire audit events in `CaseService`
- [ ] Update `CaseController` (new endpoints + modified existing)
- [ ] `CaseTemplateController`
- [ ] Add POI dependency to `pom.xml`

### Frontend
- [ ] Update `CaseService` (new fields in request/response types)
- [ ] Update `CaseFormComponent` (new controls, multi-lawyer, template modal)
- [ ] Update `CaseListComponent` (priority filter, export button)
- [ ] Update `CaseDetailComponent` (new fields, children, history tab)
- [ ] New `CaseTemplatesComponent` (modal)
