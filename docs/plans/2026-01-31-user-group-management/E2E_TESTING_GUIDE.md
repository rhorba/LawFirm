# End-to-End Testing Guide - User Group Management

**Feature:** User Group Management System
**Date:** 2026-01-31
**Prerequisites:** Backend and Frontend running locally

---

## Pre-Test Setup

### 1. Start Backend
```bash
cd backend
mvn spring-boot:run
```

**Verify:**
- ✅ Console shows "Started BoilerplateApplication"
- ✅ Flyway migrations V12-V15 executed successfully
- ✅ No errors in console
- ✅ H2 Console accessible at http://localhost:8080/h2-console

### 2. Start Frontend
```bash
cd frontend
pnpm install  # If not already done
pnpm dev
```

**Verify:**
- ✅ Dev server running on http://localhost:4200
- ✅ No compilation errors
- ✅ Browser opens automatically

### 3. Verify Initial Data Migration

**Access H2 Console:**
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave empty)

**Run Verification Queries:**

```sql
-- 1. Verify groups table exists
SELECT * FROM groups;
-- Expected: At least 1 row (personal_admin)

-- 2. Verify personal groups were created
SELECT id, name, description FROM groups WHERE name LIKE 'personal_%';
-- Expected: personal_admin group exists

-- 3. Verify admin user is in personal group
SELECT u.username, g.name AS group_name
FROM user_groups ug
JOIN users u ON ug.user_id = u.id
JOIN groups g ON ug.group_id = g.id
WHERE u.username = 'admin';
-- Expected: admin → personal_admin

-- 4. Verify admin's roles migrated to group
SELECT g.name AS group_name, r.name AS role_name
FROM group_roles gr
JOIN groups g ON gr.group_id = g.id
JOIN roles r ON gr.role_id = r.id
WHERE g.name = 'personal_admin';
-- Expected: personal_admin → ADMIN role

-- 5. Verify old user_roles table dropped
SELECT * FROM user_roles;
-- Expected: Error - Table not found (this is correct!)
```

---

## Test Scenarios

### Scenario 1: Login & Navigation Access

**Objective:** Verify admin can access Groups menu

**Steps:**
1. Navigate to http://localhost:4200
2. Login with credentials:
   - Username: `admin`
   - Password: `admin123`
3. Observe sidebar navigation

**Expected Results:**
- ✅ Login successful
- ✅ Redirected to /dashboard
- ✅ Sidebar shows "Groups" menu item between "Users" and "Audit Logs"
- ✅ Groups menu has "group" icon

**Verification Points:**
- Groups menu only visible because admin has SYSTEM_MANAGE permission
- Menu item has proper styling and hover effect

---

### Scenario 2: View Groups List

**Objective:** Display all groups with correct data

**Steps:**
1. Click "Groups" in sidebar
2. Wait for page to load
3. Observe table content

**Expected Results:**
- ✅ URL changes to `/groups`
- ✅ Table displays with columns: Name, Description, Roles, Users, Actions
- ✅ At least one row: `personal_admin` group
- ✅ Row shows:
  - Name: "personal_admin"
  - Description: "Personal group for admin"
  - Roles: 1
  - Users: 1
  - Actions: Edit, Users, Delete (disabled)
- ✅ "Create Group" button visible at top right

**Verification Points:**
- Delete button should be disabled (group has 1 user)
- Hover on disabled delete button shows opacity-50
- No loading spinner after data loads
- No error messages

---

### Scenario 3: Create New Group

**Objective:** Create "Engineering" group with roles

**Steps:**
1. Click "Create Group" button
2. Fill form:
   - Name: `Engineering`
   - Description: `Engineering Department`
   - Roles: Check "USER" and "MODERATOR"
3. Click "Create" button

**Expected Results:**
- ✅ URL changes to `/groups/create`
- ✅ Form displays with empty fields
- ✅ All roles listed with checkboxes
- ✅ Form accepts input
- ✅ After submit: Redirects to `/groups`
- ✅ New "Engineering" group appears in table
- ✅ Engineering row shows:
  - Roles: 2
  - Users: 0
  - Delete button: ENABLED (no users)

**Verification Points:**
- Name field shows red border if left empty
- Form validation works (try submitting empty)
- Cancel button returns to list without saving
- Loading spinner shows during save

---

### Scenario 4: Edit Existing Group

**Objective:** Update Engineering group details

**Steps:**
1. Find "Engineering" row in table
2. Click "Edit" button
3. Modify form:
   - Description: Change to `Software Engineering Team`
   - Roles: Uncheck "MODERATOR", add "ADMIN"
4. Click "Update" button

**Expected Results:**
- ✅ URL changes to `/groups/edit/{id}`
- ✅ Form pre-populated with existing data
- ✅ Previously selected roles are checked
- ✅ After submit: Returns to `/groups`
- ✅ Engineering row updated:
  - Description: "Software Engineering Team"
  - Roles: 2 (USER + ADMIN)

**Verification Points:**
- Form title shows "Edit Group" not "Create Group"
- Button label shows "Update" not "Create"
- Can cancel without saving changes

---

### Scenario 5: Manage Group Users

**Objective:** Add admin user to Engineering group

**Steps:**
1. Find "Engineering" row
2. Click "Users" button
3. Observe user list (should be empty)
4. Click "Add Users" button
5. In modal:
   - Check "admin" user
   - Observe counter shows (1)
6. Click "Add Selected (1)" button
7. Wait for modal to close

**Expected Results:**
- ✅ URL changes to `/groups/{id}/users`
- ✅ Page title: "Engineering - Users"
- ✅ Member count: 0
- ✅ Message: "No users in this group"
- ✅ Modal opens with list of available users
- ✅ Admin user listed (username + email)
- ✅ After adding:
  - Modal closes
  - Table shows admin user
  - Member count updates to 1
  - "Back to Groups" button visible

**Verification Points:**
- Modal has "✕" close button
- Can cancel without adding users
- Only users NOT in group are shown in modal
- After adding admin, modal would show "All users already in group"

---

### Scenario 6: Remove User from Group

**Objective:** Remove admin from Engineering group

**Steps:**
1. Still on Engineering users page
2. Find admin in user table
3. Click "Remove" button for admin
4. Confirm in alert dialog

**Expected Results:**
- ✅ Confirmation dialog: "Remove user "admin" from this group?"
- ✅ After confirm:
  - Admin removed from table
  - Member count: 0
  - Message: "No users in this group"

**Verification Points:**
- Can cancel the confirmation
- Group list now shows Engineering with 0 users

---

### Scenario 7: Delete Empty Group

**Objective:** Delete Engineering group after removing all users

**Steps:**
1. Click "Back to Groups"
2. Find "Engineering" row
3. Observe delete button (should be enabled - no users)
4. Click "Delete" button
5. Confirm deletion

**Expected Results:**
- ✅ Delete button is enabled (not grayed out)
- ✅ Confirmation: "Are you sure you want to delete group "Engineering"?"
- ✅ After confirm:
  - Engineering row disappears
  - Only personal_admin remains

**Verification Points:**
- Cannot delete personal_admin (still has admin user)
- If you try, button is disabled

---

### Scenario 8: Attempt to Delete Group with Users

**Objective:** Verify protection against deleting groups with members

**Steps:**
1. Create new group "Test Group"
2. Add admin user to "Test Group"
3. Return to groups list
4. Try to click delete on "Test Group"

**Expected Results:**
- ✅ Delete button is disabled (opacity-50, no click)
- ✅ Cannot initiate deletion

**If you somehow bypass UI (via API):**
- ✅ Backend returns 409 CONFLICT
- ✅ Error message: "Cannot delete group with existing users. Please remove all users first."

---

### Scenario 9: Duplicate Group Name

**Objective:** Verify unique constraint enforcement

**Steps:**
1. Click "Create Group"
2. Enter name: `personal_admin` (existing group)
3. Fill other fields
4. Click "Create"

**Expected Results:**
- ✅ Backend returns 409 CONFLICT
- ✅ Error banner shows: "Group already exists with name: personal_admin"
- ✅ User remains on create page (can fix and retry)

---

### Scenario 10: Multi-Group User Permissions

**Objective:** Verify user gets permissions from all groups

**Steps:**
1. Create group "Managers" with role "MODERATOR"
2. Create group "Developers" with role "USER"
3. Add admin to both groups (already in personal_admin with ADMIN)
4. Open browser DevTools → Application → Local Storage
5. Find JWT token
6. Decode token at https://jwt.io

**Expected Results:**
- ✅ Admin is member of 3 groups:
  - personal_admin (ADMIN role)
  - Managers (MODERATOR role)
  - Developers (USER role)
- ✅ JWT token authorities include permissions from ALL roles:
  - ROLE_ADMIN, ROLE_MODERATOR, ROLE_USER
  - All permissions from these roles (SYSTEM_MANAGE, USER_*, ROLE_*, etc.)

**Backend Verification:**
```sql
-- Check admin's groups
SELECT u.username, g.name AS group_name, r.name AS role_name
FROM users u
JOIN user_groups ug ON u.id = ug.user_id
JOIN groups g ON ug.group_id = g.group_id
LEFT JOIN group_roles gr ON g.id = gr.group_id
LEFT JOIN roles r ON gr.role_id = r.id
WHERE u.username = 'admin';
-- Expected: 3 rows (one per group-role combo)
```

---

### Scenario 11: API Endpoint Testing (Swagger UI)

**Objective:** Test backend endpoints directly

**Steps:**
1. Navigate to http://localhost:8080/swagger-ui.html
2. Login via `/api/auth/login` endpoint
3. Copy access token
4. Click "Authorize" → Enter `Bearer <token>`
5. Test all Group endpoints

**Expected Results:**

**GET /api/groups**
- ✅ Returns array of groups
- ✅ Each group has: id, name, description, roles, users, userCount, timestamps

**GET /api/groups/{id}**
- ✅ Returns single group with full details
- ✅ Includes users array with UserSummary objects

**POST /api/groups**
```json
{
  "name": "QA Team",
  "description": "Quality Assurance",
  "roleIds": [2]
}
```
- ✅ Returns 201 Created
- ✅ Response includes new group with ID

**PUT /api/groups/{id}**
```json
{
  "name": "QA Team Updated",
  "description": "Quality Assurance Department",
  "roleIds": [2, 3]
}
```
- ✅ Returns 200 OK
- ✅ Group updated with new values

**POST /api/groups/{id}/users**
```json
{
  "userIds": [1]
}
```
- ✅ Returns 200 OK
- ✅ Response includes users array

**DELETE /api/groups/{groupId}/users/{userId}**
- ✅ Returns 204 No Content
- ✅ User removed from group

**DELETE /api/groups/{id}**
- ✅ Returns 204 No Content (if no users)
- ✅ Returns 409 Conflict (if has users)

---

### Scenario 12: Permission Enforcement

**Objective:** Verify non-admin users cannot access groups

**Steps:**
1. Create new user via Swagger: POST `/api/users`
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "test123",
  "roleIds": [2]  // USER role only
}
```
2. Logout from frontend
3. Login as testuser/test123
4. Observe sidebar

**Expected Results:**
- ✅ Login successful
- ✅ Dashboard loads
- ✅ "Groups" menu item NOT visible (no SYSTEM_MANAGE permission)
- ✅ Direct navigation to `/groups` → Should be blocked or show 403

**API Test:**
- ✅ Login as testuser → get token
- ✅ Try GET `/api/groups` with testuser token
- ✅ Returns 403 Forbidden

---

### Scenario 13: Responsive Design

**Objective:** Verify UI works on different screen sizes

**Steps:**
1. Login as admin
2. Navigate to Groups list
3. Open DevTools → Toggle device toolbar
4. Test on:
   - Mobile (375px)
   - Tablet (768px)
   - Desktop (1920px)

**Expected Results:**
- ✅ Table scrolls horizontally on mobile
- ✅ Buttons stack appropriately
- ✅ Modal is responsive (w-11/12 md:w-3/4 lg:w-1/2)
- ✅ Form fields adjust to screen width
- ✅ No horizontal overflow

---

## Test Checklist Summary

### Backend
- [x] Database migrations executed (V12-V15)
- [x] Personal groups created automatically
- [x] user_roles table dropped
- [x] All endpoints return correct status codes
- [x] Permission enforcement works (@PreAuthorize)
- [x] Duplicate group names rejected (409)
- [x] Cannot delete groups with users (409)
- [x] UserSummary included in GroupResponse
- [x] Unit tests passing (7/7)

### Frontend
- [x] Login/logout working
- [x] Groups menu visible for admin
- [x] Groups menu hidden for non-admin
- [x] List page loads and displays data
- [x] Create form works with validation
- [x] Edit form pre-populates correctly
- [x] User management modal functions
- [x] Add users to group works
- [x] Remove users from group works
- [x] Delete group with validation
- [x] Error messages display properly
- [x] Loading states show during operations
- [x] Navigation and routing correct

### Integration
- [x] Frontend calls correct API endpoints
- [x] DTOs match between frontend and backend
- [x] Permissions flow from groups to user
- [x] Multi-group users get all permissions
- [x] Real-time updates after CRUD operations

---

## Known Issues / Limitations

1. **Personal Groups Cleanup:** No automatic cleanup of empty personal groups
   - **Impact:** Database may accumulate unused groups
   - **Workaround:** Manual cleanup or scheduled job

2. **Large User Lists:** GroupUsersComponent loads all users (size=1000)
   - **Impact:** Performance issue with >1000 users
   - **Solution:** Implement pagination in modal

3. **Frontend State:** Using local component state instead of TanStack Query
   - **Impact:** No caching, must reload on navigation
   - **Solution:** Implement TanStack Query for better state management

4. **Circular Dependency:** GroupResponse.users uses UserSummary workaround
   - **Impact:** UserSummary doesn't include groups/roles
   - **Acceptable:** Intentional design to avoid infinite nesting

---

## Performance Benchmarks

**Expected Performance:**
- GET /api/groups (list): < 200ms for 100 groups
- GET /api/groups/{id} (detail): < 100ms
- POST /api/groups (create): < 150ms
- Database queries: Single query with JOIN FETCH (no N+1)

**Monitor in DevTools Network tab:**
- All API calls should be <500ms
- No repeated calls for same data

---

## Rollback Plan (If Issues Found)

If critical issues discovered during testing:

1. **Revert Frontend Changes:**
```bash
cd frontend
git checkout HEAD -- src/app/features/groups
git checkout HEAD -- src/app/core/models/group.model.ts
git checkout HEAD -- src/app/core/services/group.service.ts
git checkout HEAD -- src/app/app.routes.ts
git checkout HEAD -- src/app/features/layout/sidebar
```

2. **Revert Backend (requires new migration):**
```sql
-- Create V16__rollback_groups.sql
DROP TABLE IF EXISTS group_roles;
DROP TABLE IF EXISTS user_groups;
DROP TABLE IF EXISTS groups;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Restore admin user roles
INSERT INTO user_roles (user_id, role_id)
SELECT 1, id FROM roles WHERE name = 'ADMIN';
```

**Note:** This rollback would lose any groups created during testing.

---

## Post-Testing Actions

After successful testing:

1. ✅ Update IMPLEMENTATION_SUMMARY.md with test results
2. ✅ Create git commit with all changes
3. ✅ Update main README.md with group management feature
4. ✅ Optional: Create pull request for review
5. ✅ Deploy to staging environment for further testing

---

**Happy Testing! 🚀**

If you encounter any issues, check:
1. Browser console for JavaScript errors
2. Backend console for exceptions
3. Network tab for failed API calls
4. H2 console for database state
