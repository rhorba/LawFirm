# End-to-End Testing Guide: Remove Personal Groups

**Feature**: Group-Based User Management (Migration V16)
**Testing Date**: 2026-02-02
**Tester**: _____________
**Environment**: Development / Production

---

## Prerequisites

### Environment Setup
- [ ] Java 21 installed and `JAVA_HOME` configured
- [ ] Node.js v20+ with pnpm installed globally
- [ ] PostgreSQL running (production) OR H2 (development)
- [ ] Git configured with `core.autocrlf input`

### Starting the Application

**Development Mode (H2 Database):**
```bash
# Terminal 1 - Backend
cd backend
mvn spring-boot:run

# Terminal 2 - Frontend
cd frontend
pnpm install
pnpm dev
```

**Production Mode (Docker Compose + PostgreSQL):**
```bash
docker-compose -f docker-compose.prod.yml up --build
```

### Access Points
- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console** (dev only): http://localhost:8080/h2-console

### Default Credentials
- **Username**: `admin`
- **Password**: `admin123`

---

## Test Suite

### Test 1: Verify Migration Applied Successfully

**Objective**: Confirm V16 migration created "Default Users" group

**Steps:**
1. Start backend application
2. Check backend logs for migration success:
   ```
   Expected log: "Flyway: Migrating schema ... to version 16 - create default users group"
   ```

**H2 Console Verification (Development):**
1. Navigate to http://localhost:8080/h2-console
2. **JDBC URL**: `jdbc:h2:mem:boilerplate`
3. **Username**: `sa`
4. **Password**: (leave empty)
5. Run query:
   ```sql
   SELECT g.name, g.description, r.name as role_name
   FROM groups g
   JOIN group_roles gr ON g.id = gr.group_id
   JOIN roles r ON gr.role_id = r.id
   WHERE g.name = 'Default Users';
   ```

**Expected Results:**
```
name          | description                  | role_name
------------- | ---------------------------- | ---------
Default Users | Default group for new users  | USER
```

**Status**: ✅ PASS / ❌ FAIL

**Notes:**
_____________________________________________________________

---

### Test 2: User Creation Without Role Selection

**Objective**: Verify new user creation flow no longer shows role selection

**Steps:**
1. Login as admin (username: `admin`, password: `admin123`)
2. Navigate to **Users** page from sidebar
3. Click **"Create User"** button (top-right)
4. Observe the form fields

**Expected Observations:**
- [ ] Form shows: Username, Email, Password, Enabled toggle
- [ ] **NO** role selection checkboxes visible
- [ ] **NO** "Roles" section in the form

**Create a Test User:**
1. Fill form:
   - **Username**: `testuser1`
   - **Email**: `testuser1@example.com`
   - **Password**: `password123`
   - **Enabled**: (checked)
2. Click **"Save"** button
3. Wait for success notification

**Expected Results:**
- [ ] User created successfully
- [ ] Success toast/notification appears
- [ ] User list refreshes
- [ ] New user `testuser1` appears in the list
- [ ] User has a blue badge showing **"Default Users"** group

**Verify in User List:**
- [ ] User row shows: `testuser1`, `testuser1@example.com`, Enabled badge
- [ ] **Groups column** shows blue badge: "Default Users"

**Status**: ✅ PASS / ❌ FAIL

**Screenshot**: (Optional - capture user list showing new user)

**Notes:**
_____________________________________________________________

---

### Test 3: User Edit Panel - Read-Only Groups Display

**Objective**: Verify user edit shows groups but no role selection

**Steps:**
1. From Users page, click **"Edit"** icon (pencil) on `testuser1`
2. Observe the edit panel that slides in from the right

**Expected Observations:**
- [ ] Form shows: Username, Email, Password (optional), Enabled toggle
- [ ] **NO** role selection checkboxes visible
- [ ] **"Assigned Groups"** section is present (read-only)
- [ ] "Assigned Groups" shows blue badge: **"Default Users"**
- [ ] Helper text visible: _"To change group assignments, use the Groups management page."_

**Update User Details:**
1. Change email to `testuser1-updated@example.com`
2. Click **"Save"** button

**Expected Results:**
- [ ] User updated successfully
- [ ] Success notification appears
- [ ] Edit panel closes
- [ ] User list shows updated email
- [ ] **Groups remain unchanged** (still "Default Users")

**Status**: ✅ PASS / ❌ FAIL

**Notes:**
_____________________________________________________________

---

### Test 4: API Contract Verification (Swagger)

**Objective**: Verify API no longer accepts `roleIds` parameter

**Steps:**
1. Navigate to http://localhost:8080/swagger-ui.html
2. Authenticate:
   - Click **"Authorize"** button (top-right)
   - Enter: `Bearer <your-jwt-token>` (get from browser dev tools after login)
   - Click **"Authorize"** then **"Close"**

**Test POST /api/users:**
1. Expand **POST /api/users** endpoint
2. Click **"Try it out"**
3. Observe the request body schema

**Expected Schema:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string"
}
```
- [ ] **NO** `roleIds` field present in schema
- [ ] Only username, email, password fields

**Test PUT /api/users/{id}:**
1. Expand **PUT /api/users/{id}** endpoint
2. Click **"Try it out"**
3. Observe the request body schema

**Expected Schema:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "enabled": true
}
```
- [ ] **NO** `roleIds` field present in schema
- [ ] Only username, email, password, enabled fields

**Status**: ✅ PASS / ❌ FAIL

**Notes:**
_____________________________________________________________

---

### Test 5: Group-Based Role Assignment

**Objective**: Verify users can be assigned roles through groups

**Steps:**
1. Navigate to **Groups** page from sidebar
2. Verify "Default Users" group exists in the list
3. Click on "Default Users" group to view details

**Expected Observations:**
- [ ] Group details show:
  - **Name**: Default Users
  - **Description**: Default group for new users
  - **Roles**: USER badge
  - **Users**: List includes `testuser1` and `admin`

**Create a New Group:**
1. Click **"Create Group"** button
2. Fill form:
   - **Name**: `Moderators`
   - **Description**: `Moderator access group`
3. Select roles:
   - [x] MODERATOR
4. Click **"Save"**

**Expected Results:**
- [ ] "Moderators" group created successfully
- [ ] Group appears in groups list

**Assign User to New Group:**
1. Click on **"Moderators"** group
2. Click **"Manage Users"** or **"Add Users"** button
3. Select `testuser1` from the user list
4. Click **"Add"** or **"Save"**

**Expected Results:**
- [ ] `testuser1` added to "Moderators" group successfully
- [ ] Group members list shows `testuser1`

**Verify in Users Page:**
1. Navigate back to **Users** page
2. Click **"Edit"** on `testuser1`
3. Observe **"Assigned Groups"** section

**Expected Observations:**
- [ ] Two group badges visible:
  - "Default Users"
  - "Moderators"
- [ ] Both groups displayed as blue badges

**Status**: ✅ PASS / ❌ FAIL

**Notes:**
_____________________________________________________________

---

### Test 6: Backwards Compatibility - Existing Personal Groups

**Objective**: Verify existing personal groups remain functional

**Note**: This test only applies if there are users with `personal_username` groups created before the migration.

**Steps:**
1. Check if any existing users have personal groups:
   - Navigate to **Groups** page
   - Look for groups named `personal_*` (e.g., `personal_admin`)

**If Personal Groups Exist:**
1. Navigate to **Users** page
2. Find a user with a personal group (check Groups column)
3. Click **"Edit"** on that user

**Expected Observations:**
- [ ] "Assigned Groups" section shows the personal group (e.g., `personal_admin`)
- [ ] Personal group is displayed as a blue badge
- [ ] User retains all functionality

**Update User with Personal Group:**
1. Change username to `{username}-updated`
2. Click **"Save"**

**Expected Results:**
- [ ] User updated successfully
- [ ] Personal group **still visible** in "Assigned Groups"
- [ ] No errors or warnings

**If No Personal Groups Exist:**
- [ ] Mark as **N/A** - skip this test

**Status**: ✅ PASS / ❌ FAIL / N/A

**Notes:**
_____________________________________________________________

---

### Test 7: New User Registration Flow

**Objective**: Verify self-registration assigns users to "Default Users" group

**Steps:**
1. Logout from admin account (top-right menu → Logout)
2. Click **"Register"** link on login page
3. Fill registration form:
   - **Username**: `selfregister1`
   - **Email**: `selfregister1@example.com`
   - **Password**: `password123`
4. Click **"Register"** button

**Expected Results:**
- [ ] Registration successful
- [ ] Auto-login after registration
- [ ] Redirected to Dashboard
- [ ] Dashboard shows user info for `selfregister1`

**Verify Group Assignment (Admin):**
1. Logout from `selfregister1`
2. Login as admin (`admin` / `admin123`)
3. Navigate to **Users** page
4. Find `selfregister1` in the list

**Expected Observations:**
- [ ] User `selfregister1` exists in user list
- [ ] Groups column shows **"Default Users"** badge
- [ ] **NO** personal group created (e.g., no `personal_selfregister1` group)

**Status**: ✅ PASS / ❌ FAIL

**Notes:**
_____________________________________________________________

---

### Test 8: Permission Inheritance from Groups

**Objective**: Verify users inherit permissions from their assigned groups

**Prerequisites:**
- Complete Test 5 (testuser1 assigned to Moderators group)

**Steps:**
1. Logout from admin
2. Login as `testuser1` / `password123`
3. Navigate to **Users** page

**Expected Behavior:**
- [ ] `testuser1` can view users (inherited from "Default Users" → USER role → USER_READ permission)
- [ ] `testuser1` can create/edit users (inherited from "Moderators" → MODERATOR role → USER_CREATE, USER_UPDATE permissions)

**Test User Creation (as testuser1):**
1. Click **"Create User"** button
2. Verify form is accessible (no permission denied error)
3. Create a test user:
   - **Username**: `createdbymod`
   - **Email**: `createdbymod@example.com`
   - **Password**: `password123`
4. Click **"Save"**

**Expected Results:**
- [ ] User created successfully by `testuser1`
- [ ] No permission errors
- [ ] New user appears in list

**Verify Group Management Access:**
1. Navigate to **Groups** page
2. Verify `testuser1` **cannot** create groups (no CREATE permission)

**Expected Behavior:**
- [ ] Groups page visible (read-only)
- [ ] **"Create Group"** button **NOT visible** or disabled
- [ ] Clicking on a group shows details but no edit button

**Status**: ✅ PASS / ❌ FAIL

**Notes:**
_____________________________________________________________

---

### Test 9: Database Consistency Check

**Objective**: Verify database reflects new group assignment model

**H2 Console Verification (Development):**
1. Navigate to http://localhost:8080/h2-console
2. Run query to check all users have groups:
   ```sql
   SELECT u.username, g.name as group_name, r.name as role_name
   FROM users u
   JOIN user_groups ug ON u.id = ug.user_id
   JOIN groups g ON ug.group_id = g.id
   JOIN group_roles gr ON g.id = gr.group_id
   JOIN roles r ON gr.role_id = r.id
   ORDER BY u.username, g.name;
   ```

**Expected Results:**
- [ ] All users have at least one group assignment
- [ ] New users (testuser1, selfregister1) show "Default Users" group
- [ ] testuser1 shows both "Default Users" and "Moderators"
- [ ] No users have `NULL` group assignments

**Check for Orphaned Personal Groups:**
```sql
SELECT name, description
FROM groups
WHERE name LIKE 'personal_%'
AND id NOT IN (SELECT group_id FROM user_groups);
```

**Expected Results:**
- [ ] Query returns empty (no orphaned personal groups)
- [ ] OR returns only old personal groups created before migration (acceptable)

**Status**: ✅ PASS / ❌ FAIL

**Notes:**
_____________________________________________________________

---

### Test 10: Frontend Console Error Check

**Objective**: Verify no JavaScript errors in browser console

**Steps:**
1. Open browser Developer Tools (F12)
2. Navigate to **Console** tab
3. Clear console
4. Perform the following actions:
   - Login as admin
   - Navigate to Users page
   - Create a new user
   - Edit an existing user
   - Navigate to Groups page
   - View group details

**Expected Results:**
- [ ] **NO** JavaScript errors in console
- [ ] **NO** 404 errors for API calls
- [ ] **NO** TypeScript compilation errors
- [ ] Only expected API calls visible in Network tab

**Common Issues to Watch For:**
- ❌ `Cannot read property 'roleIds' of undefined`
- ❌ `roles is not defined`
- ❌ `isRoleSelected is not a function`
- ❌ `toggleRole is not a function`

**Status**: ✅ PASS / ❌ FAIL

**Screenshot**: (Optional - capture console with no errors)

**Notes:**
_____________________________________________________________

---

## Test Summary

| Test # | Test Name                              | Status | Notes |
|--------|----------------------------------------|--------|-------|
| 1      | Verify Migration Applied               | [ ]    |       |
| 2      | User Creation Without Role Selection   | [ ]    |       |
| 3      | User Edit - Read-Only Groups Display   | [ ]    |       |
| 4      | API Contract Verification (Swagger)    | [ ]    |       |
| 5      | Group-Based Role Assignment            | [ ]    |       |
| 6      | Backwards Compatibility - Personal     | [ ]    |       |
| 7      | New User Registration Flow             | [ ]    |       |
| 8      | Permission Inheritance from Groups     | [ ]    |       |
| 9      | Database Consistency Check             | [ ]    |       |
| 10     | Frontend Console Error Check           | [ ]    |       |

**Overall Status**: ✅ ALL PASS / ⚠️ PARTIAL / ❌ FAIL

---

## Regression Testing

### Test 11: Existing Functionality Still Works

**Objective**: Verify other features unaffected by changes

**Quick Smoke Tests:**
1. **Authentication**:
   - [ ] Login works
   - [ ] Logout works
   - [ ] Remember me checkbox works
   - [ ] Invalid credentials show error

2. **User Profile**:
   - [ ] Navigate to Profile page
   - [ ] Update profile (bio, phone)
   - [ ] Changes saved successfully

3. **Audit Logs**:
   - [ ] Navigate to Audit Logs page
   - [ ] Recent user creation events visible
   - [ ] Filtering and pagination work

4. **Dashboard**:
   - [ ] Dashboard shows user info
   - [ ] Role badges display correctly
   - [ ] Statistics/widgets display

**Status**: ✅ PASS / ❌ FAIL

**Notes:**
_____________________________________________________________

---

## Performance Testing

### Test 12: Load Time and Responsiveness

**Objective**: Verify no performance degradation

**Steps:**
1. Open browser Developer Tools → Network tab
2. Clear cache (Ctrl+Shift+Delete)
3. Reload application (Ctrl+R)

**Measure:**
- [ ] Initial page load: _______ ms (Expected: < 2000ms)
- [ ] API response time (GET /api/users): _______ ms (Expected: < 500ms)
- [ ] User creation time: _______ ms (Expected: < 1000ms)

**Expected Results:**
- [ ] No significant performance degradation
- [ ] Page loads feel responsive
- [ ] No laggy UI interactions

**Status**: ✅ PASS / ❌ FAIL

**Notes:**
_____________________________________________________________

---

## Security Testing

### Test 13: Authorization Rules Still Enforced

**Objective**: Verify permission-based access control works

**Steps:**
1. Create a user with only USER role (default):
   - Username: `basicuser`
   - Assign only to "Default Users" group

2. Login as `basicuser`
3. Try to access restricted features

**Expected Behavior:**
- [ ] **Can** view Users page (USER_READ permission)
- [ ] **Cannot** create users (no USER_CREATE permission)
- [ ] **Cannot** delete users (no USER_DELETE permission)
- [ ] **Cannot** access Groups page (no ROLE_READ permission)

**Verify Error Handling:**
- [ ] Attempting restricted action shows proper error message
- [ ] No 500 errors (should be 403 Forbidden)

**Status**: ✅ PASS / ❌ FAIL

**Notes:**
_____________________________________________________________

---

## Cleanup

After testing, optionally clean up test data:

```sql
-- Remove test users
DELETE FROM user_groups WHERE user_id IN (SELECT id FROM users WHERE username IN ('testuser1', 'selfregister1', 'createdbymod', 'basicuser'));
DELETE FROM users WHERE username IN ('testuser1', 'selfregister1', 'createdbymod', 'basicuser');

-- Remove test group
DELETE FROM group_roles WHERE group_id IN (SELECT id FROM groups WHERE name = 'Moderators');
DELETE FROM user_groups WHERE group_id IN (SELECT id FROM groups WHERE name = 'Moderators');
DELETE FROM groups WHERE name = 'Moderators';
```

---

## Sign-Off

**Tester Name**: _______________________
**Date Completed**: _______________________
**Signature**: _______________________

**Overall Test Result**: ✅ APPROVED FOR PRODUCTION / ❌ REQUIRES FIXES

**Critical Issues Found**:
_____________________________________________________________
_____________________________________________________________

**Recommendations**:
_____________________________________________________________
_____________________________________________________________

---

## Appendix: Troubleshooting

### Issue: "Default Users group not found" error

**Symptoms**: Backend logs show `RuntimeException: Default Users group not found`

**Solution**:
1. Verify migration V16 applied:
   ```sql
   SELECT * FROM flyway_schema_history WHERE version = '16';
   ```
2. If missing, manually run migration or restart backend
3. Check `groups` table for "Default Users" entry

---

### Issue: Role selection UI still visible

**Symptoms**: User create/edit form shows role checkboxes

**Solution**:
1. Clear browser cache (Ctrl+Shift+Delete)
2. Hard refresh (Ctrl+F5)
3. Verify frontend build is latest:
   ```bash
   cd frontend
   pnpm run build
   ```

---

### Issue: Users cannot access any features

**Symptoms**: New users get 403 Forbidden on all pages

**Solution**:
1. Verify "Default Users" group has USER role assigned:
   ```sql
   SELECT * FROM group_roles WHERE group_id IN (SELECT id FROM groups WHERE name = 'Default Users');
   ```
2. If missing, manually assign USER role to "Default Users" group

---

## Test Automation Notes

**For CI/CD Integration:**
- All backend tests automated: `mvn clean verify`
- Frontend linting automated: `pnpm lint`
- Manual E2E tests can be automated with:
  - Selenium/Playwright for UI testing
  - RestAssured for API contract testing
  - Testcontainers for integration tests

**Recommended E2E Test Framework**: Playwright with TypeScript
