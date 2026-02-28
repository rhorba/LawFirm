# Case Management Enhancements — Test Scenarios

> **Prerequisites:** Backend running (`mvn spring-boot:run`), Frontend running (`pnpm dev`), logged in as `admin / admin123`.

---

## Feature 1: Multiple Lawyers per Case

**Backend endpoint:** `POST /api/cases` · `PUT /api/cases/{id}`

### Test 1.1 — Create case with multiple lawyers
1. Navigate to **Cases → New Case**.
2. Under **Lawyers**, check **two or more** lawyers from the checklist.
3. Fill in required fields (Case Type, Tribunal, Registration Date, Case Description).
4. Click **Create Case**.
5. **Expected:** Case detail page shows all selected lawyers as indigo badges.

### Test 1.2 — Create with zero lawyers blocked
1. Navigate to **New Case**.
2. Leave all lawyer checkboxes unchecked.
3. Click **Create Case**.
4. **Expected:** Red warning "At least one lawyer must be selected" appears; form does not submit.

### Test 1.3 — API validation
```bash
curl -X POST http://localhost:8080/api/cases \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"caseTypeCode":"CIVIL","tribunalCode":"RAJA","lawyerIds":[],"registrationDate":"2026-02-23","caseDescription":"Test"}'
# Expected: 400 Bad Request — lawyerIds must not be empty
```

---

## Feature 2: Opposing Party

**Backend fields:** `opposingParty VARCHAR(255)`

### Test 2.1 — Set opposing party on create
1. **New Case → Additional Information → Opposing Party**.
2. Enter `"Société ABC SARL"`.
3. Create the case.
4. **Expected:** Case detail shows "Opposing Party: Société ABC SARL".

### Test 2.2 — Edit opposing party
1. Open an existing case → **Edit Case**.
2. Change Opposing Party to `"Ministère des Finances"`.
3. Save.
4. **Expected:** Detail page reflects the new value. Audit history tab shows `opposingParty` in changed fields.

---

## Feature 3: Case Outcome (Enum + Notes)

**Backend:** `outcome VARCHAR(20)`, `outcome_notes TEXT`
**Enum values:** `WON, LOST, SETTLED, DISMISSED`

### Test 3.1 — Set outcome on an existing case
1. Edit a case.
2. In **Additional Information → Outcome**, select **WON**.
3. The **Outcome Notes** textarea appears automatically.
4. Enter `"Jugement définitif du 20/02/2026"`.
5. Save.
6. **Expected:** Detail shows Outcome: WON and the notes.

### Test 3.2 — Outcome clears notes textarea when blank
1. Edit the case, select `"— No outcome yet —"`.
2. Save.
3. **Expected:** Outcome Notes section no longer shown in detail view.

### Test 3.3 — API
```bash
curl -X PUT http://localhost:8080/api/cases/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"outcome":"SETTLED","outcomeNotes":"Settled for 50,000 MAD"}'
# Expected: 200 OK — CaseResponse.outcome = "SETTLED"
```

---

## Feature 4: Case Priority

**Backend:** `priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL'`
**Enum values:** `URGENT, HIGH, NORMAL, LOW`

### Test 4.1 — Default priority is NORMAL
1. Create a case without selecting priority (form defaults to NORMAL).
2. **Expected:** Case list shows "NORMAL" badge (blue) in Priority column.

### Test 4.2 — Set URGENT priority
1. Create or edit a case → set Priority to **Urgent**.
2. **Expected:** Priority badge in case list shows "URGENT" in red.

### Test 4.3 — Filter by priority
1. In the case list, open **Priority** dropdown → select **Urgent**.
2. **Expected:** Only cases with priority URGENT are shown.

---

## Feature 5: Initial Payment Date

**Backend field:** `initial_payment_date DATE`

### Test 5.1 — Set initial payment date
1. Create a new case.
2. In **Registration → Initial Payment Date**, pick `2026-03-01`.
3. Save.
4. **Expected:** Detail page shows "Initial Payment: 01/03/2026".

### Test 5.2 — Optional — leave blank
1. Create a case without setting Initial Payment Date.
2. **Expected:** Field is not shown in case detail (null value hidden by `@if`).

---

## Feature 6: Fiscal Year

**Backend field:** `fiscal_year SMALLINT`

### Test 6.1 — Set fiscal year
1. Create or edit a case → enter **Fiscal Year: 2026**.
2. Save.
3. **Expected:** Case detail shows "Fiscal Year: 2026".

### Test 6.2 — Invalid year
1. Enter `1999` (below min=2000) or `2100` (above max=2099).
2. **Expected:** Browser HTML5 validation prevents submission. (No server-side constraint is enforced beyond the SMALLINT range.)

---

## Feature 7: Linked Cases (Parent Case)

**Backend field:** `parent_case_id BIGINT REFERENCES cases(id)`

### Test 7.1 — Create sub-case with parent
```bash
# Get an existing case ID first (e.g. id=1)
curl -X POST http://localhost:8080/api/cases \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "caseTypeCode":"CIVIL","tribunalCode":"RAJA","lawyerIds":[1],
    "registrationDate":"2026-02-23","caseDescription":"Sub-case test",
    "parentCaseId":1
  }'
# Expected: 201 Created — response.parentCase = {id:1, fullCaseNumber:"CIVIL/RAJA/2026/0001"}
```

### Test 7.2 — Detail shows parent link
1. Open the sub-case.
2. **Expected:** "Parent Case" field shows a clickable link to the parent's full case number.

### Test 7.3 — Parent shows related children
1. Open the parent case → click the **Related Cases** tab.
2. **Expected:** Sub-case appears as a clickable link.

### Test 7.4 — API: Get children
```bash
curl http://localhost:8080/api/cases/1/children \
  -H "Authorization: Bearer <token>"
# Expected: JSON array with sub-case(s) [{id, fullCaseNumber}]
```

---

## Feature 8: Excel Export

**Backend endpoint:** `GET /api/cases/export` (streams `.xlsx`)

### Test 8.1 — Export from UI
1. Apply any filter (e.g., Case Type = CIVIL).
2. Click **Export Excel** button.
3. **Expected:** File `cases-export.xlsx` downloads. Open it — header row is bold, 12 columns, data matches the filtered cases.

### Test 8.2 — Export all (no filters)
1. Reset all filters.
2. Click **Export Excel**.
3. **Expected:** Excel file contains all non-deleted cases.

### Test 8.3 — API
```bash
curl -o cases.xlsx \
  "http://localhost:8080/api/cases/export?caseTypeCode=CIVIL" \
  -H "Authorization: Bearer <token>"
# Expected: Binary .xlsx file downloaded. Content-Disposition: attachment; filename=cases-export.xlsx
```

---

## Feature 9: Case Templates

**Backend:** `GET/POST/DELETE /api/cases/templates`

### Test 9.1 — Create a template (API)
```bash
curl -X POST http://localhost:8080/api/cases/templates \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Standard Civil Contract","caseTypeCode":"CIVIL","caseCategoryCode":"1101"}'
# Expected: 201 Created — {id, name, caseTypeCode, caseCategoryCode}
```

### Test 9.2 — Use template from form
1. Navigate to **New Case**.
2. Click **Use Template** (top-right of Case Identification section).
3. **Expected:** Modal opens with the template from 9.1 listed.
4. Click **Use →** on the template.
5. **Expected:** Modal closes; Case Type and Category fields pre-filled.

### Test 9.3 — Save as template from form
1. Fill in Case Type and Category in the form.
2. Click **Save as Template**, enter a name.
3. **Expected:** Alert "Template saved successfully!" appears.
4. Click **Use Template** again — new template appears in the list.

### Test 9.4 — Duplicate template blocked
```bash
curl -X POST http://localhost:8080/api/cases/templates \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Standard Civil Contract","caseTypeCode":"CIVIL","caseCategoryCode":"1101"}'
# Expected: 409 Conflict — "Template with name 'Standard Civil Contract' already exists"
```

### Test 9.5 — Delete template
```bash
curl -X DELETE http://localhost:8080/api/cases/templates/1 \
  -H "Authorization: Bearer <token>"
# Expected: 204 No Content
```

---

## Feature 10: Audit Trail (History)

**Backend endpoint:** `GET /api/cases/{id}/history`

### Test 10.1 — History recorded on create
1. Create a new case.
2. Open the case detail → **History** tab.
3. **Expected:** Entry `CASE_CREATED` with changed fields `[fullCaseNumber, priority, lawyers, status]`.

### Test 10.2 — History recorded on update
1. Edit the case (change priority, opposingParty).
2. Save.
3. **Expected:** New entry `CASE_UPDATED` with `changedFields: ["priority", "opposingParty"]`.

### Test 10.3 — History recorded on status change
1. Change status (e.g., DRAFT → OPEN).
2. **Expected:** Entry `CASE_STATUS_CHANGED` with `changedFields: ["status", "statusReason"]`.

### Test 10.4 — History recorded on delete
1. Delete a case.
2. **Expected:** Entry `CASE_DELETED` with `changedFields: ["deletedAt"]`.

### Test 10.5 — API
```bash
curl http://localhost:8080/api/cases/1/history \
  -H "Authorization: Bearer <token>"
# Expected: JSON array of audit entries, newest first
# Each entry: {id, action, resource, resourceId, username, metadata, createdAt}
# metadata: {"changedFields":["..."]}
```

---

## Feature 11: PENAL Case Type

**Backend:** V39 migration seeds PENAL case type with bilingual names.

### Test 11.1 — PENAL type available
1. **New Case → Case Type** dropdown.
2. **Expected:** "Pénal" appears as an option.

### Test 11.2 — PENAL case number format
1. Create a case with Case Type = PENAL, Tribunal = RAJA.
2. **Expected:** Case number starts with `PENAL/RAJA/2026/...`

### Test 11.3 — API
```bash
curl http://localhost:8080/api/reference/case-types \
  -H "Authorization: Bearer <token>"
# Expected: response includes {code:"PENAL", nameFr:"Pénal", nameAr:"جنائي"}
```

---

## Feature 12: Closed/Archived Case Protection

**Business rules:**
- `CLOSED` cases → can only transition to `ARCHIVED`, nothing else.
- `ARCHIVED` cases → cannot change status at all.
- Cases with terminal status (`isTerminal=true`) → cannot be soft-deleted.

### Test 12.1 — CLOSED → OPEN blocked
```bash
curl -X PATCH http://localhost:8080/api/cases/1/status \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"statusCode":"OPEN"}'
# (Case 1 must already be CLOSED)
# Expected: 422 Unprocessable Entity — "CLOSED cases can only be archived."
```

### Test 12.2 — CLOSED → ARCHIVED allowed
```bash
curl -X PATCH http://localhost:8080/api/cases/1/status \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"statusCode":"ARCHIVED"}'
# Expected: 200 OK — status changes to ARCHIVED
```

### Test 12.3 — ARCHIVED → any transition blocked
```bash
curl -X PATCH http://localhost:8080/api/cases/1/status \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"statusCode":"OPEN"}'
# (Case 1 must already be ARCHIVED)
# Expected: 422 Unprocessable Entity — "ARCHIVED cases cannot change status."
```

### Test 12.4 — Delete terminal case blocked
```bash
curl -X DELETE http://localhost:8080/api/cases/1 \
  -H "Authorization: Bearer <token>"
# (Case 1 must have status CLOSED or ARCHIVED)
# Expected: 409 Conflict — "Cases with status 'CLOSED' cannot be deleted. Archive them instead."
```

### Test 12.5 — Delete non-terminal case allowed
```bash
curl -X DELETE http://localhost:8080/api/cases/2 \
  -H "Authorization: Bearer <token>"
# (Case 2 must have status DRAFT or OPEN)
# Expected: 204 No Content — soft delete succeeds
```

---

## Regression Tests

Run these to confirm existing functionality still works after the changes:

| # | Test | Expected |
|---|------|----------|
| R1 | Create case with 1 lawyer | Works — `lawyerIds: [X]` accepted |
| R2 | Case list pagination | 20 rows per page, prev/next work |
| R3 | Filter by status, tribunal, year | Filtered results returned |
| R4 | Change status DRAFT → OPEN | 200 OK, status updated |
| R5 | Financial summary on detail page | Payments/Expenses/Balance calculated |
| R6 | Lawyer list page still works | No regression from ManyToMany change |
| R7 | Export with no cases matching filter | Empty Excel file with only header row |
| R8 | Template modal closes on backdrop click | `showTemplateModal.set(false)` triggers |

---

## Backend Compilation Check

```bash
cd backend
mvn clean compile
# Expected: BUILD SUCCESS — no errors
```

## Frontend Build Check

```bash
cd frontend
pnpm exec tsc --noEmit && pnpm build
# Expected: TypeScript: 0 errors. Angular build: 0 errors.
```
