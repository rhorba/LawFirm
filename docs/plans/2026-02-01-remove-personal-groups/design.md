# Remove Automatic Personal Group Creation

**Date:** 2026-02-01
**Status:** Approved

## Overview

Remove automatic creation of personal groups (personal_username) when admins create new users. Transition to manual group assignment model with a shared "Default Users" group for basic permissions.

## Problem Statement

Current system automatically creates a personal group for each new user, resulting in:
- Group proliferation (one group per user)
- Confusion between personal groups and organizational groups
- Split role management (user create/update AND group assignment)
- Unnecessary database entries for single-user groups

## Solution

Transition to manual group assignment with shared default group:
- Create single "Default Users" group with USER role
- New users automatically join "Default Users" (not personal groups)
- Remove role management from user create/update operations
- All role changes happen exclusively through group assignment

## Architecture

### Current State
```
User Creation Flow:
Admin → POST /api/users (with roleIds)
  → Create User
  → Create personal_{username} group with roleIds
  → Assign user to personal group

Role Update Flow:
Admin → PUT /api/users/{id} (with roleIds)
  → Find user's personal group
  → Update personal group's roles
```

### New State
```
User Creation Flow:
Admin → POST /api/users (no roleIds)
  → Create User
  → Add to "Default Users" group
  → User inherits USER role

Role Management Flow:
Admin → Groups UI
  → POST /api/groups/{id}/users
  → Assign user to groups
  → User inherits roles from groups
```

### Data Model (No Changes)
```
User ──→ Groups ──→ Roles ──→ Permissions
  (user_groups)  (group_roles)  (role_permissions)
```

Roles are assigned to groups. Users inherit roles through group membership.

## Backend Implementation

### 1. Database Migration (V16)

**File:** `backend/src/main/resources/db/migration/V16__create_default_users_group.sql`

```sql
-- Create shared Default Users group with USER role
INSERT INTO groups (name, description, created_at, updated_at, version)
VALUES ('Default Users', 'Default group for new users', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

-- Assign USER role to Default Users group
INSERT INTO group_roles (group_id, role_id)
SELECT g.id, r.id
FROM groups g, roles r
WHERE g.name = 'Default Users' AND r.name = 'USER';
```

### 2. UserService Changes

**File:** `backend/src/main/java/com/boilerplate/application/service/UserService.java`

**createUser() method:**
```java
// REMOVE: Lines 110-129 (personal group creation logic)
// REPLACE WITH:
Group defaultGroup = groupRepository.findByName("Default Users")
    .orElseThrow(() -> new RuntimeException("Default Users group not found"));

savedUser.getGroups().add(defaultGroup);
savedUser = userRepository.save(savedUser);
```

**updateUser() method:**
```java
// REMOVE: Lines 168-181 (personal group role update logic)
// Role management now happens exclusively through GroupService
```

### 3. DTO Changes

**File:** `backend/src/main/java/com/boilerplate/application/dto/request/CreateUserRequest.java`
- **Remove:** `roleIds` field
- **Remove:** Related validation annotations

**File:** `backend/src/main/java/com/boilerplate/application/dto/request/UpdateUserRequest.java`
- **Remove:** `roleIds` field
- **Remove:** Related validation annotations

**File:** `backend/src/main/java/com/boilerplate/application/dto/response/UserResponse.java`
- **Keep:** Groups and roles display (read-only, derived from group membership)

### 4. Repository Changes

**File:** `backend/src/main/java/com/boilerplate/domain/repository/GroupRepository.java`
- **Add:** `Optional<Group> findByName(String name);`

### 5. Test Updates

**File:** `backend/src/test/java/com/boilerplate/application/service/UserServiceTest.java`
- Update createUser tests to remove roleIds assertions
- Add test to verify "Default Users" group assignment
- Update updateUser tests to remove role update scenarios
- Add mock for groupRepository.findByName()

## Frontend Implementation

### 1. User Form Components

**Files:**
- `frontend/src/app/features/users/user-form/user-form.component.ts`
- `frontend/src/app/features/users/user-form/user-form.component.html`

**Changes:**
- Remove role selection UI (dropdowns/checkboxes)
- Remove roleIds from form model
- Remove roleIds from submission payload
- Keep: username, email, password, enabled fields

### 2. User List Component

**File:** `frontend/src/app/features/users/user-list/user-list.component.html`

**Changes:**
- Display user groups as badges/chips (instead of roles)
- Show aggregate roles from groups (tooltip/expandable)

### 3. User Detail/Edit View

**File:** `frontend/src/app/features/users/user-form/user-form.component.html`

**Add:**
- Read-only groups display section
- "Manage Groups" button linking to group assignment UI

**Remove:**
- Role editing capabilities

### 4. Navigation Flow

```
Create User:
  Form → Basic Info (username, email, password, enabled)
       → Submit → User created with "Default Users" group

Edit User:
  Form → Basic Info
       → Groups Display (read-only)
       → "Manage Groups" button → Navigate to Groups UI

Manage Roles:
  Groups → Select Group → Assign Users & Roles
```

### 5. Models

**File:** `frontend/src/app/core/models/user.model.ts`
- Remove roleIds from CreateUserRequest interface
- Remove roleIds from UpdateUserRequest interface
- Keep groups array in UserResponse interface

## Backwards Compatibility

### Existing Personal Groups
- **No automatic cleanup:** Existing personal_username groups remain in database
- **Functional:** Old users retain their personal groups and roles
- **Manual cleanup:** Admins can delete unused personal groups via Groups UI

### Migration Strategy
- **Old users:** Keep personal groups, fully functional
- **New users:** Join "Default Users" group only
- **Gradual transition:** System naturally phases out personal groups over time

## Testing Strategy

### Backend Tests
1. **UserServiceTest:**
   - Test new user gets "Default Users" group
   - Test user creation without roleIds
   - Test user update without roleIds
   - Test exception when "Default Users" group missing

2. **Integration Tests:**
   - POST /api/users without roleIds returns user with "Default Users" group
   - PUT /api/users/{id} without roleIds succeeds
   - User permissions derived from "Default Users" group

### Frontend Tests
1. **User Form Component:**
   - Role selection UI not rendered
   - Form submission excludes roleIds
   - Validation passes without roleIds

2. **User List Component:**
   - Groups displayed correctly
   - "Manage Groups" navigation works

### Manual Testing
1. Create new user → verify "Default Users" group assigned
2. Edit existing user with personal group → verify no errors
3. Assign user to additional groups → verify role inheritance
4. Remove user from groups → verify falls back to "Default Users" only

## Rollback Plan

If issues arise:
1. Revert migration V16 (removes "Default Users" group)
2. Revert UserService changes (restore personal group creation)
3. Revert DTO changes (restore roleIds fields)
4. Revert frontend form changes

## Success Criteria

- ✅ New users created without personal groups
- ✅ New users automatically have USER role via "Default Users" group
- ✅ User create/update forms no longer have role selection
- ✅ Group assignment UI is primary role management interface
- ✅ Existing users with personal groups continue working
- ✅ All tests pass
- ✅ No breaking changes to API contracts (except roleIds removal)

## Future Considerations

### Optional Enhancements (Not in Scope)
- Cleanup job to remove empty personal groups
- Migration script to consolidate personal groups
- Bulk group assignment operations
- Group templates for common role combinations

## Files to Modify

### Backend
- `backend/src/main/resources/db/migration/V16__create_default_users_group.sql` (new)
- `backend/src/main/java/com/boilerplate/application/service/UserService.java`
- `backend/src/main/java/com/boilerplate/application/dto/request/CreateUserRequest.java`
- `backend/src/main/java/com/boilerplate/application/dto/request/UpdateUserRequest.java`
- `backend/src/main/java/com/boilerplate/domain/repository/GroupRepository.java`
- `backend/src/test/java/com/boilerplate/application/service/UserServiceTest.java`

### Frontend
- `frontend/src/app/features/users/user-form/user-form.component.ts`
- `frontend/src/app/features/users/user-form/user-form.component.html`
- `frontend/src/app/features/users/user-list/user-list.component.html`
- `frontend/src/app/core/models/user.model.ts`

### Documentation
- Update README.md with new user management workflow
- Update API documentation (remove roleIds from user endpoints)
