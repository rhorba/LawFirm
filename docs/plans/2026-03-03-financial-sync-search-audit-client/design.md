# Design: Financial Sync, Searchable Dropdowns, Audit Snapshots, Case-Client Link

**Date:** 2026-03-03
**Status:** Approved — ready for implementation planning
**Scope:** 4 bugs + 2 implementations

---

## Items Covered

| ID | Type | Title |
|----|------|-------|
| impl1 | Implementation | Seed full tribunal and case type/category lists from Excel |
| impl2 | Implementation | Seed financial transactions from real payment data |
| bug1 | Bug | Searchable dropdowns for all large reference lists |
| bug2 | Bug | Case audit shows before/after full snapshots, not just field names |
| bug3 | Bug | Cases have no client assignment in UI (DB link exists, frontend missing) |
| bug4 | Bug | No sync between invoice (facture) and financial transaction |

---

## Section 1 — Database Layer (Flyway Migrations)

**7 new migrations (V56–V62):**

| Migration | Purpose |
|-----------|---------|
| `V56__add_invoice_id_to_financial_transactions` | Add `invoice_id` FK + UNIQUE constraint on `financial_transactions` — dedup guarantee, prevents double-transaction on PAID |
| `V57__replace_tribunal_seeds` | TRUNCATE + re-seed all 125 Moroccan tribunals from Excel. Split bilingual combined string into `name_fr` + `name_ar`. Codes: `TR_APPL_*`, `TR_PIN_*`, `TR_COM_PIN_*`, `TR_ADM_PIN_*`, `TR_ADM_APPL_*` |
| `V58__replace_case_type_seeds` | Replace current 3 types with 4 from Excel: CIVIL, PENAL, COMMERCIAL, ADMINISTRATIVE |
| `V59__replace_case_category_seeds` | Replace current categories with all 426 from Excel. Numeric codes. Arabic names. Linked to case type by numeric prefix: 1xxx→CIVIL, 2xxx→PENAL, 7xxx→ADM, 8xxx→COMMERCIAL |
| `V60__seed_financial_transactions` | 11 transactions from Excel VAA sheet (real data: year, payment date, case reference, tribunal). Serves as impl2 |
| `V61__add_audit_before_after_snapshot` | Add `old_values TEXT` + `new_values TEXT` columns to `audit_logs` table (JSON payloads) |
| `V62__update_case_type_statuses_for_new_types` | Wire allowed statuses (DRAFT, ACTIVE, CLOSED, ARCHIVED, ADJOURNED) to all 4 new case types |

### V56 Schema Detail

```sql
ALTER TABLE financial_transactions
    ADD COLUMN invoice_id BIGINT,
    ADD CONSTRAINT fk_financial_transactions_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE SET NULL,
    ADD CONSTRAINT uq_financial_transactions_invoice
        UNIQUE (invoice_id);
```

### V61 Schema Detail

```sql
ALTER TABLE audit_logs
    ADD COLUMN old_values TEXT,
    ADD COLUMN new_values TEXT;
```

---

## Section 2 — Backend API Changes

### 2a. Invoice Payment Sync (bug4)

**Goal:** Marking an invoice PAID atomically creates a `FinancialTransaction`.

**New flow:**
1. User clicks "Marquer comme Payée" → frontend shows payment modal
2. Frontend sends `PATCH /api/invoices/{id}/status` with extended body:
   ```json
   {
     "status": "PAID",
     "paymentMode": "TRANSFER",
     "paymentDate": "2026-03-03",
     "paymentReference": "VIR-2026-001"
   }
   ```
3. `InvoiceService.updateStatus()` runs in single `@Transactional`:
   - Validates `status = PAID` → requires `paymentMode` + `paymentDate`
   - Updates `invoice.status = PAID`
   - Creates `FinancialTransaction`:
     - `direction = REVENUE`
     - `amount = invoice.totalAmount`
     - `caseEntity = invoice.caseEntity`
     - `invoice_id = invoice.id`
     - `paymentMode = request.paymentMode`
     - `paymentDate = request.paymentDate`
     - `paymentReference = request.paymentReference`
     - `description = "Paiement facture #" + invoice.invoiceNumber`
4. UNIQUE constraint on `invoice_id` guarantees idempotency at DB level

**Files changed:**
- `InvoiceStatusRequest.java` — add `paymentMode`, `paymentDate`, `paymentReference` fields
- `InvoiceService.java` — extend `updateStatus()` method
- `FinancialTransaction.java` — add `invoice` ManyToOne relationship
- `FinancialTransactionMapper.java` — include `invoiceId` in response

### 2b. Searchable Reference Endpoints (bug1)

All reference data endpoints gain `search`, `page`, `size` query params:

```
GET /api/tribunals?search=rabat&page=0&size=20
GET /api/case-types?search=civil
GET /api/case-categories?search=&caseTypeCode=CIVIL&page=0&size=20
GET /api/lawyers?search=benomar&page=0&size=20
GET /api/clients?search=hassan&page=0&size=20
```

**Backend implementation:**
- Use Spring Data `JpaSpecificationExecutor` + `Specification<T>` for each entity
- Search filter: `ILIKE '%search%'` on name_fr, name_ar (tribunals/types/categories) or firstName+lastName+email (clients/lawyers)
- Returns `Page<T>` wrapped in existing pagination response structure

**Files changed (per entity):**
- Repository: extend `JpaSpecificationExecutor<T>`
- Service: add `search(String query, Pageable pageable)` method
- Controller: add `@RequestParam` to existing list endpoints

### 2c. Full Audit Snapshots (bug2)

**Goal:** Audit log stores complete case state before and after each update.

**`AuditLogService.log()` signature change:**
```java
void log(String resource, Long resourceId, String action,
         Object oldValues, Object newValues, String username);
```

**`CaseService.updateCase()` change:**
```java
// Before update:
CaseResponse before = caseMapper.toResponse(existingCase);

// ... apply updates ...

// After update:
CaseResponse after = caseMapper.toResponse(savedCase);

auditLogService.log("CASE", id, "CASE_UPDATED", before, after, getCurrentUsername());
```

- `oldValues` and `newValues` serialized to JSON via `ObjectMapper`
- Stored in `audit_logs.old_values` and `audit_logs.new_values` columns
- `AuditLogResponse` DTO gains `oldValues` (Map<String,Object>) and `newValues` (Map<String,Object>)

**Files changed:**
- `AuditLog.java` — add `oldValues`, `newValues` fields
- `AuditLogService.java` — update `log()` signature + serialization
- `CaseService.java` — capture before/after, pass to audit
- `AuditLogResponse.java` — expose `oldValues`, `newValues`
- `AuditLogMapper.java` — map new fields

### 2d. Case ↔ Client Assignment (bug3)

**`CaseRequest.java`:** add `clientId` (nullable Long)

**`CaseResponse.java`:** add `clientId` (Long), `clientName` (String)

**`CaseMapper.java`:** map `client.id` → `clientId`, `client.getFullName()` → `clientName`

**`CaseService.updateCase()`:** if `clientId` provided, load and assign `Client` entity

**New dedicated endpoint:**
```
PATCH /api/cases/{id}/client
Body: { "clientId": 42 }        // assign
Body: { "clientId": null }      // unassign
```

---

## Section 3 — Frontend Components

### 3a. `SearchableSelectComponent` (bug1)

Reusable Angular standalone component replacing all static `<select>` dropdowns.

**Interface:**
```typescript
@Input() endpoint: string;           // e.g., '/api/tribunals'
@Input() searchParam: string;        // e.g., 'search'
@Input() displayField: string;       // e.g., 'nameFr'
@Input() valueField: string;         // e.g., 'code'
@Input() placeholder: string;
@Input() multiple: boolean = false;
@Input() additionalParams?: Record<string, string>; // e.g., { caseTypeCode: 'CIVIL' }
@Output() selectionChange = new EventEmitter<any>();
```

**Behavior:**
- Text input with dropdown panel (Tailwind styled)
- Debounce 300ms on keyup → `GET {endpoint}?{searchParam}={query}&page=0&size=20`
- Shows loading spinner during fetch
- Renders paginated results; "Load more" button if `totalPages > 1`
- Keyboard navigation (arrow keys, Enter to select, Escape to close)

**Replaces dropdowns in:**
- `CaseFormComponent`: tribunal, caseType, caseCategory, lawyers (multi), client
- `CaseListComponent`: filter dropdowns
- `ClientListComponent`: search bar

### 3b. Payment Modal (bug4)

Triggered by "Marquer comme Payée" button on invoice detail/list.

**Component:** `PaymentModalComponent`

**Form fields:**
| Field | Type | Validation |
|-------|------|------------|
| `paymentMode` | select | required |
| `paymentDate` | date | required, defaults to today |
| `paymentReference` | text | optional |

**On submit:**
- `PATCH /api/invoices/{id}/status` with full payload
- On success: invoice status badge updates reactively, case financial tab refreshes to show new transaction
- On error: inline error message (e.g., "Facture déjà payée")

### 3c. Case Form — Client Field (bug3)

**Case create/edit form:**
- Add optional `SearchableSelectComponent` for client below existing fields
- Label: "Client assigné (optionnel)"
- Sends `clientId` in `CaseRequest`

**Case detail page:**
- New "Client" card section showing: client name, CIN/tax number, phone
- "Modifier" button → inline `SearchableSelectComponent` → `PATCH /api/cases/{id}/client`
- "Retirer" button → `PATCH /api/cases/{id}/client` with `clientId: null`

### 3d. Audit History Tab — Before/After Diff (bug2)

**Enhanced history tab in case detail:**

For each audit event with `oldValues`/`newValues`:
- Render a field-by-field diff table:

```
| Champ            | Avant                    | Après                   |
|------------------|--------------------------|-------------------------|
| tribunal         | TGI Rabat                | TGI Casablanca          |
| status           | ACTIVE                   | CLOSED                  |
```

- Only show rows where `before[field] !== after[field]`
- Human-readable field labels (French)
- For `CASE_CREATED` events: show "Après" column only (no "Avant")

---

## Data Source Summary (impl1 + impl2)

### Tribunals (125 courts from Excel)
Codes follow pattern: `TR_{type}_{level}_{city_index}`
- `TR_APPL_*` — Cours d'appel (22 courts)
- `TR_PIN_*` — Tribunaux de 1ère instance (70+ courts)
- `TR_COM_PIN_*` — Tribunaux commerciaux (8 courts)
- `TR_COM_APPL_*` — Cours d'appel commerciales (3 courts)
- `TR_ADM_PIN_*` — Tribunaux administratifs (7 courts)
- `TR_ADM_APPL_*` — Cours d'appel administratives (2 courts)

### Case Types (4 types from Excel)
| Code | FR | AR |
|------|----|----|
| CIVIL | Civile | مدني |
| PENAL | Pénale | جنائي |
| COMMERCIAL | Commerciale | تجاري |
| ADMINISTRATIVE | Administrative | إداري |

### Case Categories (426 items from Excel)
Numeric codes with Arabic descriptions. Linked to case type by prefix:
- `1xxx` → CIVIL
- `2xxx` → PENAL
- `7xxx` → ADMINISTRATIVE
- `8xxx` → COMMERCIAL

### Transaction Seeds (11 records from Excel VAA sheet)
Real 2024 payment data for lawyer Abdelhadi Benomar. Fields: year, case number, payment date, tribunal, case nature.

---

## Implementation Order (suggested)

1. **V56–V62 Flyway migrations** (foundation for everything)
2. **Backend: bug4** (invoice-transaction sync) — highest business value
3. **Backend: bug1** (search endpoints) — needed before frontend
4. **Backend: bug2** (audit snapshots)
5. **Backend: bug3** (case-client assignment endpoint)
6. **Frontend: SearchableSelectComponent** (shared, needed by all features)
7. **Frontend: bug4** (payment modal)
8. **Frontend: bug3** (case-client UI)
9. **Frontend: bug2** (audit diff display)
10. **Frontend: impl1** (no frontend work — data only)
