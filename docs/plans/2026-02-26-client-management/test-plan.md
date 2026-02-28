# Client Management — Manual Test Plan

> **Scope:** End-to-end manual verification of the Client Management module.
> **Pre-requisites:** Backend running on `http://localhost:8080`, Frontend on `http://localhost:4200`.
> **Login:** `admin` / `admin123` (has all CLIENT_* permissions).

---

## Setup

```bash
# Terminal 1 — Backend
cd backend && mvn spring-boot:run

# Terminal 2 — Frontend
cd frontend && pnpm dev
```

Navigate to `http://localhost:4200`, log in as `admin`.

---

## TC-01: Sidebar Navigation

| Step | Action | Expected |
|------|--------|----------|
| 1 | Look at the left sidebar | "Clients" item with `people` icon appears between Lawyers and Users |
| 2 | Click "Clients" | Navigates to `/clients`, page loads with empty table ("No clients found") |

---

## TC-02: Create Individual Client

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click "New Client" button | Modal opens with "New Client" title |
| 2 | Verify default type | "Individual" is pre-selected |
| 3 | Leave firstName blank, click "Create Client" | Backend returns 400: "Individual clients require first and last name" (error banner shown) |
| 4 | Fill: firstName=`Ahmed`, lastName=`Benali`, CIN=`AB123456`, DOB=`1990-05-15`, phone=`+212600000001`, email=`ahmed@example.com` | Fields accept input |
| 5 | Click "Create Client" | Modal closes, client appears in table with badge `INDIVIDUAL` |
| 6 | Verify table row | Shows "Ahmed Benali", INDIVIDUAL badge (blue), CIN=AB123456, phone and email, Cases=0, Active badge (green) |

---

## TC-03: Create Corporate Client

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click "New Client" | Modal opens |
| 2 | Change type to "Corporate" | INDIVIDUAL fields (First/Last/CIN/DOB) disappear, Company Name / Tax Number appear |
| 3 | Leave Company Name blank, click "Create Client" | Error: "Corporate/Government clients require a company name" |
| 4 | Fill: companyName=`Acme Maroc SARL`, taxNumber=`ICE123456789` | Fields accept input |
| 5 | Click "Create Client" | Modal closes, "Acme Maroc SARL" appears with CORPORATE badge (purple) |

---

## TC-04: Create Government Client

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click "New Client" | Modal opens |
| 2 | Select type "Government" | Company Name field shown |
| 3 | Fill: companyName=`Ministère de la Justice` | — |
| 4 | Click "Create Client" | Row appears with GOVERNMENT badge (green) |

---

## TC-05: Age Validation

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click "New Client", type=Individual | — |
| 2 | Fill firstName, lastName, CIN, set DOB=`2015-01-01` (under 18) | — |
| 3 | Click "Create Client" | Error: "Client must be between 18 and 100 years old" |
| 4 | Set DOB=`1910-01-01` (over 100) | — |
| 5 | Click "Create Client" | Error: "Client must be between 18 and 100 years old" |
| 6 | Set DOB=`1985-06-15` (valid) | — |
| 7 | Click "Create Client" | Client created successfully |

---

## TC-06: Uniqueness Validation

| Step | Action | Expected |
|------|--------|----------|
| 1 | Try to create a second Individual with CIN=`AB123456` (already used in TC-02) | Error: "CIN already registered: AB123456" |
| 2 | Try to create another client with email=`ahmed@example.com` | Error: "Email already registered: ahmed@example.com" |

---

## TC-07: Edit Client

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click the edit (pencil) icon on "Ahmed Benali" | Modal opens with "Edit Client" title, all fields pre-populated |
| 2 | Verify clientType field is hidden (cannot change type on edit) | No "Client Type" dropdown in modal |
| 3 | Change phone to `+212666999111` | — |
| 4 | Click "Save Changes" | Modal closes, phone updated in the table row |

---

## TC-08: Search

| Step | Action | Expected |
|------|--------|----------|
| 1 | Type `ahmed` in the search box | After 300ms debounce, list filters to show "Ahmed Benali" only |
| 2 | Clear search, type `acme` | Shows "Acme Maroc SARL" only |
| 3 | Type `XYZ999` (no match) | Table shows "No clients found" |
| 4 | Click "Clear" button | All clients reappear |

---

## TC-09: Type Filter

| Step | Action | Expected |
|------|--------|----------|
| 1 | Change type dropdown to "Corporate" | Only corporate clients shown |
| 2 | Change to "Government" | Only government clients shown |
| 3 | Change to "Individual" | Only individual clients shown |
| 4 | Change to "All Types" | All clients shown |

---

## TC-10: Deactivate Client

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click the deactivate (person_off) icon on "Ahmed Benali" | Browser confirm dialog appears |
| 2 | Click "Cancel" | Client remains in list (unchanged) |
| 3 | Click deactivate again, click "OK" | Client disappears from active list (search only shows active clients) |
| 4 | Verify "Acme Maroc SARL" still has deactivate icon | ✓ (still active) |

---

## TC-11: Excel Export

| Step | Action | Expected |
|------|--------|----------|
| 1 | Click "Export" button (top right) | Browser downloads `clients-export.xlsx` |
| 2 | Open the file | Has header row: Full Name, Type, CIN, Company Name, Tax Number, Phone, Email, Address, Active, Case Count, Date of Birth, Registered At |
| 3 | Verify data rows match visible clients | ✓ |
| 4 | Apply type filter "Corporate", click Export | Downloaded file contains only corporate clients |

---

## TC-12: Permission-Based UI (if a non-admin user is available)

| Step | Action | Expected |
|------|--------|----------|
| 1 | Log in as a user with only `CLIENT_READ` | Navigate to `/clients` |
| 2 | Verify "New Client" button is hidden | ✓ |
| 3 | Verify Edit and Deactivate icons are hidden | ✓ |
| 4 | Verify Export button is visible | ✓ (CLIENT_READ is sufficient) |

---

## TC-13: Backend API Direct Tests (Swagger UI)

Navigate to `http://localhost:8080/swagger-ui.html`, authenticate with Bearer token.

| Endpoint | Test | Expected |
|----------|------|----------|
| `GET /api/clients` | No params | 200, page with all active clients |
| `GET /api/clients?search=ahmed` | Search filter | 200, filtered results |
| `GET /api/clients?type=CORPORATE` | Type filter | 200, only corporate clients |
| `GET /api/clients/{id}` | Valid ID | 200, full ClientResponse with `age` calculated |
| `GET /api/clients/999` | Invalid ID | 404, "Client not found: 999" |
| `POST /api/clients` | Missing clientType | 400, validation error |
| `POST /api/clients` | Individual with no name | 422, "Individual clients require first and last name" |
| `DELETE /api/clients/{id}` | Valid ID | 204, client deactivated |
| `GET /api/clients/export` | No params | 200, binary `.xlsx` stream |

---

## TC-14: Regression — Existing Features Unaffected

| Test | Expected |
|------|----------|
| Navigate to `/cases` | Case list loads normally |
| Navigate to `/lawyers` | Lawyer list loads normally |
| Create a new case | Case form works, no errors |
| Backend starts cleanly | 43 migrations applied (V1–V43), no errors |

---

## Sign-Off Checklist

- [ ] TC-01: Sidebar navigation works
- [ ] TC-02: Individual client creation + validation
- [ ] TC-03: Corporate client creation + validation
- [ ] TC-04: Government client creation
- [ ] TC-05: Age validation (under 18, over 100)
- [ ] TC-06: Uniqueness validation (CIN, email)
- [ ] TC-07: Edit client (type locked, fields pre-populated)
- [ ] TC-08: Search debounced correctly
- [ ] TC-09: Type filter works
- [ ] TC-10: Deactivate with confirm dialog
- [ ] TC-11: Excel export downloads correctly
- [ ] TC-12: Permission-based UI
- [ ] TC-13: API endpoints via Swagger
- [ ] TC-14: Regression — existing features unaffected
