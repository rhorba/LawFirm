# Testing Scenarios: Case Management Feature

**Version:** 2.0
**Last Updated:** 2026-02-09
**Status:** Production Ready with Test Data
**Coverage:** Case & Lawyer Management Full Stack

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Authentication & Permissions](#authentication--permissions)
3. [Lawyer Management Tests](#lawyer-management-tests)
4. [Case Management Tests](#case-management-tests)
5. [Integration Tests](#integration-tests)
6. [Edge Cases & Error Handling](#edge-cases--error-handling)
7. [Performance Tests](#performance-tests)

---

## Prerequisites

### Test Environment Setup

**Backend:**
```bash
cd backend
mvn spring-boot:run
```
Backend runs at: http://localhost:8080

**Frontend:**
```bash
cd frontend
pnpm install
pnpm dev
```
Frontend runs at: http://localhost:4200

### Test Users

| Username | Password | Role | Permissions |
|----------|----------|------|-------------|
| admin | admin123 | ADMIN | All permissions (CREATE, READ, UPDATE, DELETE for all modules) |
| user | admin123 | USER | Read-only permissions (CASE_READ, LAWYER_READ, TRIBUNAL_READ, CASETYPE_READ) |

> **Note:** Both test users use the same password (`admin123`) for simplicity in testing.

### Pre-Seeded Test Data

The database is pre-populated with realistic test data for immediate testing:

**Lawyers (6 total)**:
| Name | Tax ID | Email | Phone | Status |
|------|--------|-------|-------|--------|
| Ahmed BENOMAR | TAX001 | ahmed.benomar@lawfirm.ma | +212-6-12-34-56-78 | Active |
| Fatima ALAOUI | TAX002 | fatima.alaoui@lawfirm.ma | +212-6-23-45-67-89 | Active |
| Youssef IDRISSI | TAX003 | youssef.idrissi@lawfirm.ma | +212-6-34-56-78-90 | Active |
| Samira KETTANI | TAX004 | samira.kettani@lawfirm.ma | +212-6-45-67-89-01 | Active |
| Karim BENJELLOUN | TAX005 | karim.benjelloun@lawfirm.ma | +212-6-56-78-90-12 | Active |
| Nadia LAZRAK | TAX006 | nadia.lazrak@lawfirm.ma | +212-6-67-89-01-23 | **Inactive** |

**Cases (10 total)**:
- **Years**: 2023, 2024, 2025, 2026
- **Types**: Civil (4), Commercial (3), Administrative (2), Criminal (1)
- **Statuses**: Draft (1), Open (2), In Progress (3), Hearing (2), Judgment (1), Closed (1)
- **Tribunals**: Distributed across Rabat, Casablanca, Marrakech, Fes, Tanger
- **Case Numbers**: Range from `CIVIL/TR_PIN_1/2024/00001` to `COMMERC/TR_COM_PIN_8/2026/00001`

This pre-seeded data allows immediate testing without manual data entry.

### API Documentation
Swagger UI: http://localhost:8080/swagger-ui.html

---

## Authentication & Permissions

### Test Case 1.1: Login as Admin
**Objective:** Verify admin can access all features

**Steps:**
1. Navigate to http://localhost:4200
2. Enter username: `admin`
3. Enter password: `admin123`
4. Click "Login"

**Expected Result:**
- ✅ Redirected to dashboard
- ✅ User info displayed in top-right
- ✅ "Cases" menu item visible in sidebar
- ✅ "Lawyers" menu item visible in sidebar

### Test Case 1.2: Permission-Based UI
**Objective:** Verify buttons appear based on permissions

**Steps:**
1. Login as admin
2. Navigate to Cases list
3. Observe available buttons

**Expected Result:**
- ✅ "New Case" button visible (CASE_CREATE)
- ✅ Edit icons visible on rows (CASE_UPDATE)
- ✅ Delete checkboxes visible (CASE_DELETE)

### Test Case 1.3: Read-Only User Access
**Objective:** Verify restricted user cannot modify data

**Steps:**
1. Logout admin
2. Login as user (read-only)
3. Navigate to Cases list

**Expected Result:**
- ✅ Can view case list
- ✅ "New Case" button NOT visible
- ✅ Edit/Delete actions NOT visible
- ✅ Can view case details (read-only)

---

## Lawyer Management Tests

### Test Case 2.1: Create Lawyer
**Objective:** Create a new lawyer profile

> **Note:** The system already has 6 pre-seeded lawyers. Use a unique Tax ID for new test lawyers.

**Steps:**
1. Login as admin
2. Click "Lawyers" in sidebar
3. Click "New Lawyer" button
4. Fill form:
   - First Name: `Mohammed`
   - Last Name: `Benali`
   - Tax ID: `TAX007` (unique - TAX001-TAX006 already used)
   - Email: `mohammed.benali@lawfirm.ma`
   - Phone: `+212 6 12 34 56 78`
5. Click "Create Lawyer"

**Expected Result:**
- ✅ Modal closes
- ✅ Success notification (if implemented)
- ✅ Lawyer appears in list
- ✅ Status badge shows "Active"
- ✅ Full name displays as "Ahmed BENOMAR"

**Verification:**
- Check database: `SELECT * FROM lawyers WHERE tax_id = 'TAX001';`
- Verify `active = true`

### Test Case 2.2: Edit Lawyer
**Objective:** Update existing lawyer information

**Steps:**
1. Navigate to Lawyers list
2. Find lawyer "Ahmed BENOMAR"
3. Click Edit icon
4. Modify:
   - Email: `a.benali@lawfirm.ma`
   - Phone: `+212 6 99 88 77 66`
5. Click "Update Lawyer"

**Expected Result:**
- ✅ Modal closes
- ✅ Updated information reflects in list
- ✅ Email changed to `a.benali@lawfirm.ma`

### Test Case 2.3: Search Lawyers
**Objective:** Test search functionality

**Steps:**
1. Navigate to Lawyers list
2. In search box, type: `Ahmed`
3. Wait 300ms (debounce)

**Expected Result:**
- ✅ List filters to show only "Ahmed BENOMAR"
- ✅ Other lawyers hidden
- ✅ Search is case-insensitive

**Additional Tests:**
- Search by email: `benali`
- Search by tax ID: `TAX001`
- Clear search: results restore

### Test Case 2.4: Pagination
**Objective:** Test pagination controls

**Steps:**
1. Create 25 lawyers (if not already present)
2. Navigate to Lawyers list
3. Observe pagination controls

**Expected Result:**
- ✅ Shows "Showing 1 to 20 of 25 lawyers"
- ✅ "Next" button enabled
- ✅ "Previous" button disabled
- ✅ Click "Next" → shows lawyers 21-25
- ✅ Click "Previous" → returns to page 1

### Test Case 2.5: Deactivate Lawyer
**Objective:** Soft delete a lawyer

**Steps:**
1. Navigate to Lawyers list
2. Find a lawyer
3. Click Deactivate icon (❌)
4. Confirm in dialog

**Expected Result:**
- ✅ Lawyer status changes to "Inactive"
- ✅ Badge color changes from green to gray
- ✅ Lawyer remains in database (soft delete)
- ✅ Deactivate icon disappears

### Test Case 2.6: Bulk Deactivate
**Objective:** Deactivate multiple lawyers at once

**Steps:**
1. Navigate to Lawyers list
2. Check 3 lawyers
3. Click "Deactivate Selected (3)"
4. Confirm in dialog

**Expected Result:**
- ✅ All 3 lawyers deactivated
- ✅ Status badges change to gray
- ✅ Selection clears

### Test Case 2.7: Form Validation
**Objective:** Test required field validation

**Steps:**
1. Click "New Lawyer"
2. Leave all fields empty
3. Click "Create Lawyer"

**Expected Result:**
- ✅ Form does NOT submit
- ✅ Error messages appear:
  - "First name is required"
  - "Last name is required"
  - "Tax ID is required"

**Additional Tests:**
- Invalid email format: `notanemail` → "Please enter a valid email address"
- Max length exceeded: 101 character first name → Error

---

## Case Management Tests

### Test Case 3.1: View Case List
**Objective:** Load and display case list

**Steps:**
1. Login as admin
2. Click "Cases" in sidebar

**Expected Result:**
- ✅ Case list loads
- ✅ Displays table with columns:
  - Case Number
  - Description
  - Type
  - Tribunal
  - Lawyer
  - Status
  - Registration Date
  - Actions
- ✅ Pagination controls visible
- ✅ Filter panel visible

### Test Case 3.2: Create New Case
**Objective:** Create a case with auto-generated number

> **Note:** The system has 10 pre-seeded cases. New cases will increment the sequence number appropriately.

**Steps:**
1. Navigate to Cases list
2. Click "New Case" button
3. Fill form:
   - Case Type: `PENAL` (Criminal)
   - Category: Select a criminal category from dropdown
   - Tribunal: `TR_PIN_1` (Tribunal de 1ère instance de Rabat)
   - Lawyer: Select "Ahmed BENOMAR" (or any active lawyer from the dropdown)
   - Registration Date: `2026-02-09`
   - Case Description: `Vol à l'étalage - Hypermarché Marjane`
   - Matter Description: `Client accusé de vol de marchandises d'une valeur de 500 MAD`
   - Initial Status: Select `DRAFT` or leave as default
4. Click "Create Case"

**Expected Result:**
- ✅ Redirects to case detail page
- ✅ Case number generated in format: `PENAL/TR_PIN_1/2026/00XXX` (sequence increments from existing cases)
- ✅ Status shows "Draft" badge
- ✅ Financial summary shows:
  - Total Payments: 0.00 MAD
  - Total Expenses: 0.00 MAD
  - Balance: 0.00 MAD
- ✅ Audit info shows creation timestamp and version

**Verification:**
- Database check: `SELECT * FROM cases WHERE case_type_code = 'PENAL' AND year = 2026;`
- Sequence incremented: `SELECT * FROM case_sequences WHERE case_type_code = 'PENAL' AND year = 2026;`

### Test Case 3.3: Case Number Auto-Increment
**Objective:** Verify case numbering increments correctly

**Steps:**
1. Create another PENAL case for 2026
2. Use same tribunal (TA)

**Expected Result:**
- ✅ Case number: `PENAL/TA/2026/00002`
- ✅ Sequence increments by 1

**Additional Test:**
- Create case for different year (2027) → `PENAL/TA/2027/00001`
- Create case for different type (CIVIL) → `CIVIL/TA/2026/00001`

### Test Case 3.4: Cascading Category Dropdown
**Objective:** Test category filtering by case type

**Steps:**
1. Click "New Case"
2. Select Case Type: `PENAL`
3. Observe Category dropdown

**Expected Result:**
- ✅ Only PENAL categories visible:
  - PENAL_FLAGRANT_DELIT
  - PENAL_CORRECTIONNELLE
  - PENAL_CRIMINELLE
  - PENAL_APPEL

**Additional Test:**
- Change type to `CIVIL` → categories update to CIVIL options
- Change back to `PENAL` → category selection clears

### Test Case 3.5: View Case Details
**Objective:** View full case information

**Steps:**
1. Navigate to Cases list
2. Click on a case row

**Expected Result:**
- ✅ Redirects to `/cases/{id}`
- ✅ Displays case header with:
  - Full case number
  - Status badge (color-coded)
  - Registration date
- ✅ Case Details card shows:
  - Case Type, Category, Tribunal, Lawyer, Year, Sequence
- ✅ Description card shows case/matter descriptions
- ✅ Financial Summary card shows:
  - Total Payments: 0.00 MAD
  - Total Expenses: 0.00 MAD
  - Balance: 0.00 MAD (green if positive, red if negative)
- ✅ Audit Information card shows:
  - Created At, Last Updated, Version
- ✅ Quick Actions sidebar has:
  - Change Status, Edit Case, Back to List buttons

### Test Case 3.6: Edit Existing Case
**Objective:** Update case information

**Steps:**
1. View case detail
2. Click "Edit Case" button
3. Modify:
   - Case Description: Add more details
   - Matter Description: Update information
4. Click "Update Case"

**Expected Result:**
- ✅ Redirects back to case detail
- ✅ Updated information displays
- ✅ "Last Updated" timestamp changes
- ✅ Version increments (optimistic locking)

**Note:** Case number is readonly in edit mode

### Test Case 3.7: Change Case Status
**Objective:** Update case status with workflow validation

**Steps:**
1. View a DRAFT case
2. Click "Change Status" button
3. Modal opens
4. Select new status: `OUVERT` (OPEN)
5. Add reason: `Dossier validé et prêt pour instruction`
6. Click "Change Status"

**Expected Result:**
- ✅ Modal closes
- ✅ Status badge updates to blue "OUVERT"
- ✅ Reason saved in database (future: show in history)
- ✅ Only valid statuses shown in dropdown (workflow respected)

**Status Workflow Test:**
- DRAFT can transition to: OPEN, ARCHIVED
- OPEN can transition to: IN_PROGRESS, CLOSED, ARCHIVED
- Try invalid transition → should not be in dropdown

### Test Case 3.8: Advanced Filtering
**Objective:** Test all filter combinations

**Filter by Year:**
1. Select Year: `2026`
2. Click filter

**Expected Result:**
- ✅ Only 2026 cases displayed
- ✅ Pagination resets to page 1

**Filter by Case Type:**
1. Select Type: `PENAL`

**Expected Result:**
- ✅ Only criminal cases displayed
- ✅ Category dropdown updates to PENAL options

**Filter by Multiple Criteria:**
1. Year: `2026`
2. Type: `PENAL`
3. Status: `OUVERT`
4. Lawyer: `Ahmed BENOMAR`

**Expected Result:**
- ✅ Results match ALL criteria (AND logic)
- ✅ Result count accurate

**Date Range Filter:**
1. Registration Date From: `2026-01-01`
2. Registration Date To: `2026-02-28`

**Expected Result:**
- ✅ Only cases registered in date range

**Reset Filters:**
1. Click "Reset Filters" (if implemented)

**Expected Result:**
- ✅ All filters clear
- ✅ Full case list restores

### Test Case 3.9: Search Cases
**Objective:** Test case search by number/description

**Steps:**
1. Navigate to Cases list
2. In search box, type: `PENAL`
3. Wait 300ms (debounce)

**Expected Result:**
- ✅ Cases with "PENAL" in number or description appear
- ✅ Search is case-insensitive

**Additional Searches:**
- Search by case number: `00001`
- Search by description keyword: `vol`
- Empty search → all cases restore

### Test Case 3.10: Bulk Delete Cases
**Objective:** Soft delete multiple cases

**Steps:**
1. Navigate to Cases list
2. Check 2-3 cases
3. Click "Delete Selected" button (if implemented)
4. Confirm deletion

**Expected Result:**
- ✅ Cases removed from list
- ✅ Actually soft-deleted (check `active = false` in database)
- ✅ Selection clears

### Test Case 3.11: Sort Cases
**Objective:** Test column sorting

**Steps:**
1. Click "Registration Date" column header

**Expected Result:**
- ✅ Cases sort by date (ascending)
- ✅ Click again → sort descending
- ✅ Sort indicator appears (arrow icon)

**Additional Sorts:**
- Sort by Case Number
- Sort by Status
- Sort by Lawyer Name

---

## Integration Tests

### Test Case 4.1: Create Case with New Lawyer
**Objective:** Test lawyer dropdown after creating new lawyer

**Steps:**
1. Create a new lawyer: "Fatima ALAOUI"
2. Navigate to Cases → New Case
3. Open Lawyer dropdown

**Expected Result:**
- ✅ "Fatima ALAOUI" appears in dropdown
- ✅ Can select newly created lawyer

### Test Case 4.2: Deactivated Lawyer Not Shown
**Objective:** Verify inactive lawyers excluded from dropdowns

**Steps:**
1. Deactivate lawyer "Ahmed BENOMAR"
2. Navigate to Cases → New Case
3. Open Lawyer dropdown

**Expected Result:**
- ✅ "Ahmed BENOMAR" NOT in dropdown
- ✅ Only active lawyers shown

### Test Case 4.3: Case Count Per Lawyer
**Objective:** Verify case count tracking

**Steps:**
1. Create 3 cases for lawyer "Fatima ALAOUI"
2. Call API: `GET /api/lawyers/{id}/cases/count`

**Expected Result:**
- ✅ Returns count: `3`

### Test Case 4.4: Reference Data Caching
**Objective:** Verify global reference data loads on startup

**Steps:**
1. Open browser DevTools → Network tab
2. Login to application
3. Navigate to Cases list

**Expected Result:**
- ✅ Tribunals/Types/Categories loaded ONCE on app init
- ✅ No repeat API calls when navigating
- ✅ Dropdowns populate instantly

---

## Edge Cases & Error Handling

### Test Case 5.1: Duplicate Tax ID (Lawyer)
**Objective:** Test unique constraint enforcement

**Steps:**
1. Create lawyer with Tax ID: `TAX001`
2. Try to create another lawyer with same Tax ID

**Expected Result:**
- ✅ Backend returns 409 Conflict
- ✅ Error message displayed: "Tax ID already exists"
- ✅ Form does not submit

### Test Case 5.2: Invalid Case Number Format
**Objective:** Test case number validation

**Steps:**
1. Try to manually set invalid case number via API
2. POST with malformed number format

**Expected Result:**
- ✅ Backend returns 400 Bad Request
- ✅ Error: "Invalid case number format"

### Test Case 5.3: Concurrent Updates (Optimistic Locking)
**Objective:** Test version conflict handling

**Steps:**
1. User A opens case for editing (version = 1)
2. User B opens same case and updates it (version = 2)
3. User A tries to save

**Expected Result:**
- ✅ Backend returns 409 Conflict
- ✅ Error: "Case has been modified by another user"
- ✅ User A must refresh and retry

### Test Case 5.4: Missing Required Fields
**Objective:** Test backend validation

**Steps:**
1. Submit case creation with missing required field (via API)
2. POST without `caseTypeCode`

**Expected Result:**
- ✅ Backend returns 400 Bad Request
- ✅ Validation errors returned:
  ```json
  {
    "validationErrors": {
      "caseTypeCode": "Case type is required"
    }
  }
  ```

### Test Case 5.5: Max Length Validation
**Objective:** Test character limit enforcement

**Steps:**
1. Create case with 501-character description
2. Submit form

**Expected Result:**
- ✅ Frontend shows error: "Must be less than 500 characters"
- ✅ Backend validates and rejects if bypassed

### Test Case 5.6: Invalid Status Transition
**Objective:** Test workflow enforcement

**Steps:**
1. Try to change status from CLOSED to DRAFT (invalid)
2. Call API: PATCH /api/cases/{id}/status

**Expected Result:**
- ✅ Backend returns 400 Bad Request
- ✅ Error: "Invalid status transition"

### Test Case 5.7: Date Validation
**Objective:** Test date format and logic

**Steps:**
1. Set Registration Date to future date
2. Submit case

**Expected Result:**
- ✅ Backend should validate (if business rule exists)
- ✅ Or accept future dates if allowed

### Test Case 5.8: Empty Search Results
**Objective:** Test empty state display

**Steps:**
1. Search for non-existent case: `ZZZZ9999`

**Expected Result:**
- ✅ Shows "No cases found" message
- ✅ Suggests adjusting search terms
- ✅ No error thrown

---

## Performance Tests

### Test Case 6.1: Large Dataset Pagination
**Objective:** Test performance with 1000+ cases

**Steps:**
1. Create 1000 cases via seed script
2. Navigate to Cases list
3. Measure load time

**Expected Result:**
- ✅ Initial page loads in < 2 seconds
- ✅ Pagination smooth (no lag)
- ✅ Backend uses LIMIT/OFFSET efficiently

### Test Case 6.2: Filter Performance
**Objective:** Test filter query optimization

**Steps:**
1. Apply multiple filters on 1000 cases
2. Measure response time

**Expected Result:**
- ✅ Filtered results return in < 1 second
- ✅ Database uses indexes (check EXPLAIN ANALYZE)

### Test Case 6.3: Lazy Loading
**Objective:** Verify route-based code splitting

**Steps:**
1. Open browser DevTools → Network tab
2. Navigate to Cases list

**Expected Result:**
- ✅ case-list component chunk loads on-demand (~20 kB)
- ✅ Not loaded until route accessed

### Test Case 6.4: Reference Data Caching
**Objective:** Verify no repeated API calls

**Steps:**
1. Navigate between routes 10 times
2. Monitor network requests

**Expected Result:**
- ✅ Tribunals/Types/Statuses fetched ONCE
- ✅ No repeat calls on navigation

---

## Test Execution Checklist

### Pre-Testing
- [ ] Backend running on port 8080
- [ ] Frontend running on port 4200
- [ ] Database seeded with reference data
- [ ] Test users created (admin, user)
- [ ] Browser DevTools open for debugging

### Core Functionality
- [ ] Authentication & Permissions (1.1 - 1.3)
- [ ] Lawyer CRUD Operations (2.1 - 2.6)
- [ ] Lawyer Search & Pagination (2.3 - 2.4)
- [ ] Case CRUD Operations (3.1 - 3.6)
- [ ] Case Status Workflow (3.7)
- [ ] Advanced Filtering (3.8)
- [ ] Search & Sort (3.9 - 3.11)

### Integration
- [ ] Lawyer-Case Integration (4.1 - 4.3)
- [ ] Reference Data Caching (4.4)

### Error Handling
- [ ] Validation Errors (5.1 - 5.5)
- [ ] Workflow Violations (5.6)
- [ ] Empty States (5.8)

### Performance
- [ ] Large Dataset Handling (6.1 - 6.2)
- [ ] Code Splitting (6.3)
- [ ] Caching (6.4)

---

## Bug Reporting Template

**Title:** [Component] Brief description

**Severity:** Critical | High | Medium | Low

**Steps to Reproduce:**
1.
2.
3.

**Expected Result:**
[What should happen]

**Actual Result:**
[What actually happened]

**Environment:**
- Browser:
- OS:
- Backend Version:
- Frontend Version:

**Screenshots/Logs:**
[Attach if applicable]

---

## Test Results Log

| Test ID | Description | Status | Date | Tester | Notes |
|---------|-------------|--------|------|--------|-------|
| 1.1 | Login as Admin | ⏳ | | | |
| 2.1 | Create Lawyer | ⏳ | | | |
| 3.2 | Create Case | ⏳ | | | |
| ... | ... | ... | ... | ... | ... |

**Status Legend:**
- ⏳ Not Tested
- ✅ Pass
- ❌ Fail
- ⚠️ Partial

---

## Next Testing Phase

**Client Management Testing:**
- Client CRUD operations
- Client-Case relationships
- Conflict checking
- Client search and filtering

**Financial Transaction Testing:**
- Record payments
- Record expenses
- Financial reports
- Transaction history

---

**Document Maintained By:** Development Team
**Review Schedule:** After each feature release
**Last Review:** 2026-02-08
