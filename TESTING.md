# Manual Browser Test Scenarios

**Environment:** http://localhost:4200  
**Default credentials:** `admin` / `admin123`  
**Last updated:** 2026-05-16

---

## Legend
- `[PASS]` — mark when test passes
- `[FAIL]` — mark with description of failure
- `[SKIP]` — not applicable in this environment

---

## 1. Authentication

### TS-AUTH-01 — Successful login
1. Navigate to `http://localhost:4200`
2. Verify redirect to `/login`
3. Enter username `admin`, password `admin123`
4. Click **Login**
5. **Expected:** Redirect to `/dashboard`, sidebar visible, username shown in sidebar

### TS-AUTH-02 — Invalid credentials
1. Enter username `admin`, password `wrong`
2. Click **Login**
3. **Expected:** Error message appears, remain on `/login`

### TS-AUTH-03 — Remember Me
1. Login with **Remember Me** checked
2. Close the browser tab, reopen `http://localhost:4200`
3. **Expected:** Still logged in (no redirect to login)

### TS-AUTH-04 — Logout
1. Click the user avatar / logout button in sidebar
2. **Expected:** Redirect to `/login`, sidebar no longer visible

### TS-AUTH-05 — Direct URL access while unauthenticated
1. Log out
2. Navigate directly to `http://localhost:4200/cases`
3. **Expected:** Redirect to `/login` with returnUrl preserved

---

## 2. Dashboard

### TS-DASH-01 — Dashboard loads
1. Login, navigate to `/dashboard`
2. **Expected:** KPI cards visible (no blank/spinner forever), upcoming tasks widget present

### TS-DASH-02 — Upcoming deadlines widget
1. On dashboard, verify **Upcoming Deadlines** section
2. **Expected:** Shows tasks due in next 7 days (or "No upcoming deadlines" if none)

### TS-DASH-03 — Navigation from widget
1. Click a task item in the upcoming deadlines widget
2. **Expected:** Navigate to the corresponding case detail (tasks tab)

---

## 3. Cases

### TS-CASE-01 — Case list loads
1. Click **Cases** in sidebar
2. **Expected:** Table of cases loads with columns: Case Number, Type, Status, Tribunal, Priority, Date

### TS-CASE-02 — Case search/filter
1. On case list, type a search term in the search box
2. **Expected:** Results filter in real time
3. Use Type and Status dropdowns to filter
4. **Expected:** Each filter narrows results correctly

### TS-CASE-03 — Create new case
1. Click **New Case** button
2. Fill all required fields: Case Type, Tribunal, at least one Lawyer, Registration Date
3. Click **Create**
4. **Expected:** Redirect to case detail, new case number generated (e.g., `PEN-2026/TRB-001/003`)

### TS-CASE-04 — Case detail — Info tab
1. Click any case in list
2. **Expected:** Case detail opens on Info tab with: case number, type, status badge, lawyers, client, priority

### TS-CASE-05 — Change case status
1. On case detail Info tab, click **Change Status**
2. Select a valid new status, enter a reason
3. Click **Confirm**
4. **Expected:** Status badge updates, modal closes

### TS-CASE-06 — Reject invalid status transition
1. On a case with status `ARCHIVED`, try to change status
2. **Expected:** Error message "ARCHIVED cases cannot change status"

### TS-CASE-07 — Assign client to case
1. On case detail, go to **Parties** tab
2. Search for a client and assign
3. **Expected:** Client name appears in case info

### TS-CASE-08 — Case Financial tab
1. On case detail, click **Financial** tab
2. **Expected:** Summary cards (Revenue, Expenses, Balance) and transactions table

### TS-CASE-09 — Add financial transaction from case
1. On Financial tab, click **Add Transaction**
2. Fill: direction REVENUE, amount 5000, type FEES, date today
3. **Expected:** Transaction appears in list, summary totals update

### TS-CASE-10 — Case Tasks tab
1. On case detail, click **Tasks** tab
2. Click **New Task**, fill title + due date + priority
3. **Expected:** Task appears in task list

### TS-CASE-11 — Task status update
1. Click a task to open it
2. Change status from TODO to IN_PROGRESS
3. **Expected:** Status badge updates immediately

### TS-CASE-12 — Task comment
1. Open a task, click **Add Comment**
2. Type content, submit
3. **Expected:** Comment appears in thread with author name and timestamp

### TS-CASE-13 — Case Time Tracking tab
1. Click **Time** tab on case detail
2. Click **Log Time**, fill hours, lawyer, rate, date
3. **Expected:** Entry appears in list, summary widget updates (total hours, billable amount)

### TS-CASE-14 — Mark time entry as billed
1. On a billable time entry, click **Mark as Billed**
2. **Expected:** Row shows "Billed" badge, unbilled totals decrease

### TS-CASE-15 — Case Documents tab
1. Click **Documents** tab
2. Click **Upload**, choose a PDF < 20MB, select category CONTRACT
3. **Expected:** File appears in list with name, category, size, upload date

### TS-CASE-16 — Document inline preview
1. Click on an uploaded PDF document
2. **Expected:** Preview opens inline (PDF viewer or image viewer)

### TS-CASE-17 — Document upload — reject MIME type
1. Try to upload an `.exe` file
2. **Expected:** Error "File type not allowed" before uploading

### TS-CASE-18 — Case Communications tab
1. Click **Communications** tab
2. Click **Log Communication**, select type NOTE, fill subject and body
3. **Expected:** Entry appears in timeline with type badge and author

### TS-CASE-19 — Case Parties tab — add conflict party
1. Click **Parties** tab
2. Click **Add Party**, fill name "Corp Adversaire", type OPPOSING
3. **Expected:** Party appears in parties list

### TS-CASE-20 — Delete case
1. On a non-terminal-status case, click **Delete**
2. Confirm dialog
3. **Expected:** Case removed from list; archived/closed cases should show error instead

---

## 4. Clients

### TS-CLIENT-01 — Client list loads
1. Click **Clients** in sidebar
2. **Expected:** Table with columns: Name/Company, Type, CIN/Tax No., Email, Status

### TS-CLIENT-02 — Create individual client
1. Click **New Client**
2. Select type INDIVIDUAL, fill first name, last name, CIN, date of birth (≥18), email
3. **Expected:** Client saved, appears in list

### TS-CLIENT-03 — Create corporate client
1. Select type CORPORATE, fill company name, tax number
2. **Expected:** Client saved

### TS-CLIENT-04 — Reject client under 18
1. Try to create INDIVIDUAL client with DOB = today minus 17 years
2. **Expected:** Validation error "must be between 18 and 100"

### TS-CLIENT-05 — Reject duplicate CIN
1. Try to create a client with a CIN already in the system
2. **Expected:** Error "CIN already registered"

### TS-CLIENT-06 — Deactivate client
1. Find an active client, click **Deactivate**
2. **Expected:** Status changes to inactive, action reverts to Activate

### TS-CLIENT-07 — Search clients
1. Type a name or CIN in search box
2. **Expected:** Results filter correctly

---

## 5. Lawyers

### TS-LAWYER-01 — Lawyer list loads
1. Click **Lawyers** in sidebar
2. **Expected:** Table with Name, Email, Tax ID, Bar Number, Status, Case Count

### TS-LAWYER-02 — Create lawyer
1. Click **New Lawyer**, fill first name, last name, email, unique tax ID
2. **Expected:** Lawyer created, appears in list

### TS-LAWYER-03 — Reject duplicate tax ID
1. Try to create a lawyer with a tax ID already in use
2. **Expected:** Error "tax ID already exists"

### TS-LAWYER-04 — Deactivate lawyer
1. Click **Deactivate** on an active lawyer
2. **Expected:** Status badge changes to inactive

### TS-LAWYER-05 — Activate lawyer
1. Click **Activate** on a deactivated lawyer
2. **Expected:** Status reverts to active

---

## 6. Financial (Global Ledger)

### TS-FIN-01 — Transaction ledger loads
1. Navigate to **Financial → Ledger**
2. **Expected:** Table of transactions with Amount, Direction, Type, Case, Date

### TS-FIN-02 — Filter by direction
1. Use REVENUE/EXPENSE direction filter
2. **Expected:** Only matching transactions shown

### TS-FIN-03 — Export to Excel
1. Click **Export Excel**
2. **Expected:** `.xlsx` file downloads, opens in Excel with correct columns

### TS-FIN-04 — Invoice list
1. Navigate to **Financial → Invoices**
2. **Expected:** Table of invoices with Number, Case, Status, Amount, Due Date

### TS-FIN-05 — Create invoice
1. Click **New Invoice**, select a case, add at least one line item with description, quantity, unit price
2. Click **Create**
3. **Expected:** Invoice created with DRAFT status, total calculated correctly

### TS-FIN-06 — Invoice status transitions
1. Open a DRAFT invoice, click **Send** → status becomes SENT
2. Click **Mark as Paid** → fill payment mode + date → status becomes PAID
3. **Expected:** Each transition reflected; PAID creates a transaction in the ledger

### TS-FIN-07 — Reject invalid invoice transition
1. On a DRAFT invoice, try to click **Mark as Paid** directly (if button exists)
2. **Expected:** Error "Cannot transition from DRAFT to PAID"

---

## 7. Calendar

### TS-CAL-01 — Calendar loads with current month
1. Click **Calendrier** in sidebar
2. **Expected:** Month grid visible with current month, navigation arrows present

### TS-CAL-02 — Events and tasks visible
1. **Expected:** Calendar events (HEARING/APPOINTMENT/REMINDER) and task due dates merged in same grid

### TS-CAL-03 — Navigate months
1. Click the **Next** arrow
2. **Expected:** Next month grid loads
3. Click **Previous**
4. **Expected:** Returns to previous month

### TS-CAL-04 — Create calendar event
1. Click **New Event** (or click on a day)
2. Fill title "Audience Tribunal Casablanca", type HEARING, start datetime
3. **Expected:** Event appears on that day in the grid

### TS-CAL-05 — Link event to case
1. While creating an event, select a case
2. **Expected:** Event shows the case number as a link

### TS-CAL-06 — Delete event
1. Click an event, click **Delete**
2. **Expected:** Event removed from grid

### TS-CAL-07 — Completed tasks hidden
1. Mark a task as DONE
2. Navigate to the month with that task's due date
3. **Expected:** DONE task no longer appears on calendar

---

## 8. Reporting & Analytics

### TS-REP-01 — Reports page loads
1. Click **Rapports** in sidebar
2. **Expected:** KPI cards visible (Total Cases, Open Cases, Overdue Tasks, Revenue, Pending Invoices)

### TS-REP-02 — Date preset — This Month
1. Click **This Month** preset
2. **Expected:** All charts and KPIs update to reflect current month data

### TS-REP-03 — Date preset — Last Quarter
1. Click **Last Quarter**
2. **Expected:** Data range shifts to last quarter

### TS-REP-04 — Custom date range
1. Select a custom from/to date range
2. **Expected:** KPIs and charts refresh for that range

### TS-REP-05 — Cases by status chart
1. **Expected:** Doughnut/bar chart shows cases grouped by status label

### TS-REP-06 — Cases by month chart
1. **Expected:** Line/bar chart shows case counts per month for selected range

### TS-REP-07 — Financial by month chart
1. **Expected:** Revenue vs Expenses chart per month

### TS-REP-08 — Lawyer workload table
1. **Expected:** Table with lawyer name, total hours, billable hours, billable amount

### TS-REP-09 — Unpaid invoices table
1. **Expected:** Top 10 unpaid invoices sorted by amount descending with case number and due date

---

## 9. Conflict Checking

### TS-CONF-01 — Conflict check page loads
1. Click **Conflits** in sidebar
2. **Expected:** Search box and check history visible

### TS-CONF-02 — Check with no conflicts
1. Type a unique nonsense string in the search box
2. Click **Check**
3. **Expected:** "No conflicts found" / CLEAR badge with 0 matches

### TS-CONF-03 — Check with conflicts — existing client name
1. Type the last name of an existing client
2. Click **Check**
3. **Expected:** Results show the client match(es) with entity type CLIENT

### TS-CONF-04 — Clear a conflict
1. After a check that returned matches, click **Clear**
2. Enter a resolution note "Verified: different individual"
3. **Expected:** Check marked as cleared with note and cleared-by username

### TS-CONF-05 — View check history
1. **Expected:** Previous checks listed with date, search term, result badge, and cleared status

---

## 10. User Management

### TS-USR-01 — User list loads
1. Navigate to **Users**
2. **Expected:** Table with Username, Email, Roles, Status, Last Login

### TS-USR-02 — Create user
1. Click **New User**, fill username, email, password
2. **Expected:** User created and appears in list

### TS-USR-03 — Edit user
1. Click edit on a user, change email, save
2. **Expected:** Email updates in list

### TS-USR-04 — Deactivate user
1. Toggle a user's enabled status
2. **Expected:** Status badge changes to inactive

### TS-USR-05 — Duplicate username rejected
1. Try to create user with username `admin`
2. **Expected:** Error "Username already exists"

---

## 11. Group Management

### TS-GRP-01 — Group list loads
1. Navigate to **Groups**
2. **Expected:** Table with group name, user count, roles/permissions

### TS-GRP-02 — Create group
1. Click **New Group**, fill name and description, assign roles
2. **Expected:** Group created and visible in list

### TS-GRP-03 — Add users to group
1. Click **Manage Users** on a group
2. Search for a user, click **Add**
3. **Expected:** User appears in group member list

---

## 12. Audit Logs

### TS-AUDIT-01 — Audit log list loads
1. Navigate to **Audit Logs**
2. **Expected:** Table with timestamp, user, action, entity type

### TS-AUDIT-02 — Filter by entity type
1. Use the entity type dropdown filter
2. **Expected:** Only matching log entries shown

### TS-AUDIT-03 — Actions logged after operations
1. Create a case, then navigate to Audit Logs
2. **Expected:** `CASE_CREATED` entry visible with correct user and timestamp

---

## 13. Settings

### TS-SETTINGS-01 — Dark mode toggle
1. Navigate to **Settings**
2. Toggle dark/light mode
3. **Expected:** UI theme switches immediately and persists on reload

---

## 14. Cross-cutting Concerns

### TS-SEC-01 — Unauthorized access to protected route
1. Log in as `test_viewer` / `viewer123`
2. Navigate to `/groups` (requires SYSTEM_MANAGE)
3. **Expected:** 403 or redirect, group management not accessible

### TS-SEC-02 — Token expiry (simulated)
1. Login, then manually delete `access_token` from localStorage
2. Trigger any API call (e.g., navigate to Cases)
3. **Expected:** Auto-refresh attempted; if refresh fails → redirect to login

### TS-RESP-01 — Responsive layout (mobile)
1. Open browser DevTools, set viewport to 375×812 (iPhone)
2. Navigate through all main sections
3. **Expected:** Sidebar collapses to hamburger menu, tables scroll horizontally without overflow

### TS-PERF-01 — Large dataset pagination
1. On case list with 50+ cases, go to page 2
2. **Expected:** Page loads in < 2s, correct cases shown

---

## Regression Checklist (run after each deployment)

| # | Scenario | Result |
|---|----------|--------|
| 1 | Admin login + dashboard loads | |
| 2 | Create case → status change → delete | |
| 3 | Create client → assign to case | |
| 4 | Log time entry → mark as billed | |
| 5 | Upload document → preview | |
| 6 | Create calendar event → verify on grid | |
| 7 | Run conflict check (CLEAR result) | |
| 8 | Generate report (current month KPIs) | |
| 9 | Create invoice → send → mark paid → verify transaction | |
| 10 | Logout → verify unauthenticated redirect | |
