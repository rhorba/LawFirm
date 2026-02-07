# Remove Automatic Personal Group Creation - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: use executing-plans skill to implement this plan task-by-task.

**Goal:** Remove automatic personal group creation during user signup and transition to manual group assignment with a shared "Default Users" group.

**Architecture:** Current system auto-creates `personal_{username}` groups. New system assigns all new users to shared "Default Users" group. Role management moves exclusively to group assignment UI. Existing personal groups remain functional (backwards compatible).

**Tech Stack:** Spring Boot 3.4 (Java 21), Angular 18, Flyway, MapStruct, Tailwind.

---

## 🎯 Execution Progress

### ✅ Batch 1: Backend Core Changes (Database + Services) - COMPLETED
- ✅ Task 1: Create Flyway Migration for Default Users Group
- ✅ Task 2: Add findByName Method to GroupRepository
- ✅ Task 3: Remove Personal Group Creation from UserService.createUser()
- ✅ Task 4: Remove Role Update Logic from UserService.updateUser()
- **Status**: Backend service layer refactored successfully.

### ✅ Batch 2: Backend DTOs (Request Models) - COMPLETED
- ✅ Task 5: Remove roleIds from CreateUserRequest
- ✅ Task 6: Remove roleIds from UpdateUserRequest
- **Status**: Backend API contracts updated. No roleIds in user endpoints.

### ✅ Batch 3: Backend Tests - COMPLETED
- ✅ Task 7: Update UserServiceTest for Default Users group
- ✅ Additional: Update AuthServiceTest for Default Users group
- ✅ Additional: Refactor AuthService.register() to use Default Users group
- **Status**: All backend tests passing (14/14). Backend fully refactored.

### ✅ Batch 4: Frontend Models & Services - COMPLETED
- ✅ Task 8: Remove roleIds from Frontend User Models (CreateUserRequest, UpdateUserRequest)
- **Status**: TypeScript models updated. Expected compilation errors in components (fixed in Batch 5).

### ✅ Batch 5: Frontend Components (User Edit Panel) - COMPLETED
- ✅ Task 9: Remove Role Selection from User Edit Panel TypeScript
- ✅ Task 10: Remove Role Selection UI from User Edit Panel HTML (added Groups display)
- ✅ Task 11: Remove roles Input from UserEditPanelComponent
- ✅ Task 12: Remove roles Binding from UserListComponent TypeScript
- ✅ Task 13: Remove roles Binding from UserListComponent Template
- ✅ Additional: Remove role filter dropdown from user list
- **Status**: Frontend builds successfully. All components refactored.

### 🔄 Next: Batch 6 - End-to-End Verification (optional) OR Batch 7 - Documentation

---

## Batch 1: Backend Core Changes (Database + Services)

### Task 1: Create Flyway Migration for Default Users Group

**Files:**
- Create: `backend/src/main/resources/db/migration/V16__create_default_users_group.sql`

**Step 1: Create Migration Script**

Create the SQL migration file with the following content:

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

**Step 2: Verify Migration**

Run:
```bash
cd backend
mvn clean compile
```

Expected: Flyway validates migration file syntax. No compilation errors.

---

### Task 2: Add findByName Method to GroupRepository

**Files:**
- Modify: `backend/src/main/java/com/boilerplate/domain/repository/GroupRepository.java`

**Step 1: Add Repository Method**

Add the following method to `GroupRepository` interface (after line 24):

```java
Optional<Group> findByName(String name);
```

**Complete method section should look like:**
```java
@Query("SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.roles LEFT JOIN FETCH g.users")
List<Group> findAllWithRolesAndUsers();

boolean existsByName(String name);

Optional<Group> findByName(String name);
```

**Step 2: Verify Compilation**

Run:
```bash
cd backend
mvn clean compile
```

Expected: No compilation errors. Spring Data JPA generates implementation automatically.

---

### Task 3: Remove Personal Group Creation from UserService.createUser()

**Files:**
- Modify: `backend/src/main/java/com/boilerplate/application/service/UserService.java`

**Step 1: Replace Personal Group Logic**

**REMOVE lines 110-129** (entire personal group creation block):
```java
// Create personal group for the new user with requested roles
Set<Role> roles;
if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
    roles = roleRepository.findAllByIdWithPermissions(request.getRoleIds());
} else {
    Role userRole = roleRepository.findByName("USER")
        .orElseThrow(() -> new RuntimeException("Default USER role not found"));
    roles = Set.of(userRole);
}

Group personalGroup = Group.builder()
    .name("personal_" + savedUser.getUsername())
    .description("Personal group for " + savedUser.getUsername())
    .roles(roles)
    .build();
Group savedGroup = groupRepository.save(personalGroup);

// Assign user to their personal group
savedUser.getGroups().add(savedGroup);
savedUser = userRepository.save(savedUser);
```

**REPLACE WITH** (at line 110):
```java
// Assign user to Default Users group
Group defaultGroup = groupRepository.findByName("Default Users")
    .orElseThrow(() -> new RuntimeException("Default Users group not found"));

savedUser.getGroups().add(defaultGroup);
savedUser = userRepository.save(savedUser);
```

**Step 2: Verify Compilation**

Run:
```bash
cd backend
mvn clean compile
```

Expected: No compilation errors. `createUser()` now assigns users to "Default Users" instead of creating personal groups.

---

### Task 4: Remove Role Update Logic from UserService.updateUser()

**Files:**
- Modify: `backend/src/main/java/com/boilerplate/application/service/UserService.java`

**Step 1: Remove Personal Group Role Update**

**REMOVE lines 168-181** (personal group role update block):
```java
// Update roles in user's personal group if provided
if (request.getRoleIds() != null) {
    Set<Role> roles = roleRepository.findAllByIdWithPermissions(request.getRoleIds());
    // Find user's personal group and update its roles
    Group personalGroup = user.getGroups().stream()
        .filter(g -> g.getName().equals("personal_" + user.getUsername()))
        .findFirst()
        .orElse(null);

    if (personalGroup != null) {
        personalGroup.setRoles(roles);
        groupRepository.save(personalGroup);
    }
}
```

**DO NOT replace with anything.** Just delete this entire block. The update method should now only handle username, email, password, and enabled status.

**Step 2: Verify Compilation**

Run:
```bash
cd backend
mvn clean compile
```

Expected: No compilation errors. `updateUser()` no longer manages roles.

---

## Batch 2: Backend DTOs (Request Models)

### Task 5: Remove roleIds from CreateUserRequest

**Files:**
- Modify: `backend/src/main/java/com/boilerplate/application/dto/request/CreateUserRequest.java`

**Step 1: Remove roleIds Field**

**REMOVE lines 31-32**:
```java
private Set<Long> roleIds;
```

Also **REMOVE the import** at line 11:
```java
import java.util.Set;
```

**Final file should look like:**
```java
package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
```

**Step 2: Verify Compilation**

Run:
```bash
cd backend
mvn clean compile
```

Expected: No compilation errors.

---

### Task 6: Remove roleIds from UpdateUserRequest

**Files:**
- Modify: `backend/src/main/java/com/boilerplate/application/dto/request/UpdateUserRequest.java`

**Step 1: Remove roleIds Field**

**REMOVE lines 29-30**:
```java
private Set<Long> roleIds;
```

Also **REMOVE the import** at line 10:
```java
import java.util.Set;
```

**Final file should look like:**
```java
package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Email(message = "Email must be valid")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private Boolean enabled;
}
```

**Step 2: Verify Full Backend Compilation**

Run:
```bash
cd backend
mvn clean compile
```

Expected: No compilation errors. All DTO changes applied successfully.

---

## Batch 3: Backend Tests

### Task 7: Update UserServiceTest

**Files:**
- Modify: `backend/src/test/java/com/boilerplate/application/service/UserServiceTest.java`

**Step 1: Add GroupRepository Mock**

Find the field declarations section (around line 30-40) and add:
```java
@Mock
private GroupRepository groupRepository;
```

**Step 2: Update createUser Test to Verify Default Group Assignment**

Find the test method `shouldCreateUser` (or similar create user test). Replace role-related assertions with group assertions.

**Before:**
```java
// Verify roleRepository interactions for personal group creation
verify(roleRepository).findAllByIdWithPermissions(any());
```

**After:**
```java
// Verify Default Users group assignment
verify(groupRepository).findByName("Default Users");
```

**Step 3: Add Mock Behavior in @BeforeEach or Test Setup**

Add this mock setup:
```java
Group defaultGroup = Group.builder()
    .id(1L)
    .name("Default Users")
    .description("Default group for new users")
    .build();

when(groupRepository.findByName("Default Users")).thenReturn(Optional.of(defaultGroup));
```

**Step 4: Remove Role Update Test**

Find and **DELETE** any test methods that verify role updates during user update (e.g., `shouldUpdateUserRoles()`). Role management is no longer part of UserService.

**Step 5: Run Backend Tests**

Run:
```bash
cd backend
mvn clean test
```

Expected: All tests pass. UserService tests verify "Default Users" group assignment.

---

## Batch 4: Frontend Models & Services

### Task 8: Remove roleIds from Frontend User Models

**Files:**
- Modify: `frontend/src/app/core/models/user.model.ts`

**Step 1: Update CreateUserRequest Interface**

**REMOVE line 42** from `CreateUserRequest`:
```typescript
roleIds?: number[];
```

**Updated interface:**
```typescript
export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
}
```

**Step 2: Update UpdateUserRequest Interface**

**REMOVE line 50** from `UpdateUserRequest`:
```typescript
roleIds?: number[];
```

**Updated interface:**
```typescript
export interface UpdateUserRequest {
  username?: string;
  email?: string;
  password?: string;
  enabled?: boolean;
}
```

**Step 3: Verify TypeScript Compilation**

Run:
```bash
cd frontend
pnpm run build --mode development
```

Expected: No TypeScript errors. Models updated successfully.

---

## Batch 5: Frontend Components (User Edit Panel)

### Task 9: Remove Role Selection from User Edit Panel TypeScript

**Files:**
- Modify: `frontend/src/app/features/users/user-edit-panel/user-edit-panel.component.ts`

**Step 1: Remove roleIds from Form Model**

**REMOVE line 47** from `editForm`:
```typescript
roleIds: [[] as number[]],
```

**Updated form group (lines 42-48):**
```typescript
editForm = this.fb.nonNullable.group({
  username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
  email: ['', [Validators.required, Validators.email]],
  password: [''],
  enabled: [true],
});
```

**Step 2: Remove roleIds from ngOnInit Patch**

**REMOVE line 56** from `ngOnInit`:
```typescript
roleIds: this.user.roles.map((r) => r.id),
```

**Updated ngOnInit (lines 50-63):**
```typescript
ngOnInit(): void {
  if (this.user) {
    this.editForm.patchValue({
      username: this.user.username,
      email: this.user.email,
      enabled: this.user.enabled,
    });
  } else {
    const passwordCtrl = this.editForm.get('password')!;
    passwordCtrl.setValidators([Validators.required, Validators.minLength(8)]);
    passwordCtrl.updateValueAndValidity();
  }
}
```

**Step 3: Remove Role Toggle Methods**

**DELETE lines 70-82** (toggleRole, isRoleSelected methods):
```typescript
toggleRole(roleId: number): void {
  const current = this.editForm.get('roleIds')!.value;
  const index = current.indexOf(roleId);
  if (index === -1) {
    this.editForm.get('roleIds')!.setValue([...current, roleId]);
  } else {
    this.editForm.get('roleIds')!.setValue(current.filter((id: number) => id !== roleId));
  }
}

isRoleSelected(roleId: number): boolean {
  return this.editForm.get('roleIds')!.value.includes(roleId);
}
```

**Step 4: Remove roleIds from onSubmit Create Logic**

**In onSubmit method, REMOVE lines 98-101**:
```typescript
const roleIds = formValue.roleIds;
if (roleIds.length > 0) {
  request.roleIds = roleIds;
}
```

**Updated create block (lines 92-102):**
```typescript
if (this.isCreateMode()) {
  const request: CreateUserRequest = {
    username: formValue.username,
    email: formValue.email,
    password: formValue.password,
  };

  this.userService.createUser(request).subscribe({
    next: () => {
      this.loading.set(false);
      this.saved.emit();
    },
```

**Step 5: Remove roleIds from onSubmit Update Logic**

**In onSubmit method, REMOVE lines 121-125**:
```typescript
const originalRoleIds = this.user!.roles.map((r) => r.id).sort();
const newRoleIds = formValue.roleIds.sort();
if (JSON.stringify(originalRoleIds) !== JSON.stringify(newRoleIds)) {
  request.roleIds = formValue.roleIds;
}
```

**Updated update block (lines 113-130):**
```typescript
} else {
  const request: UpdateUserRequest = {};

  if (formValue.username !== this.user!.username) request.username = formValue.username;
  if (formValue.email !== this.user!.email) request.email = formValue.email;
  if (formValue.password) request.password = formValue.password;
  if (formValue.enabled !== this.user!.enabled) request.enabled = formValue.enabled;

  if (Object.keys(request).length === 0) {
    this.close.emit();
    return;
  }

  this.userService.updateUser(this.user!.id, request).subscribe({
    next: () => {
      this.loading.set(false);
      this.saved.emit();
    },
```

**Step 6: Verify TypeScript Compilation**

Run:
```bash
cd frontend
pnpm run build --mode development
```

Expected: No TypeScript errors.

---

### Task 10: Remove Role Selection UI from User Edit Panel HTML

**Files:**
- Modify: `frontend/src/app/features/users/user-edit-panel/user-edit-panel.component.html`

**Step 1: Remove Roles Section**

**DELETE lines 155-178** (entire Roles section):
```html
<!-- Roles -->
<div>
  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
    >Roles</label
  >
  <div class="space-y-2">
    @for (role of roles; track role.id) {
      <label class="flex items-center gap-2 cursor-pointer">
        <input
          type="checkbox"
          [checked]="isRoleSelected(role.id)"
          (change)="toggleRole(role.id)"
          class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
        />
        <span class="text-sm dark:text-gray-300">
          <span class="font-medium">{{ role.name }}</span>
          @if (role.description) {
            <span class="text-gray-500 dark:text-gray-400"> - {{ role.description }}</span>
          }
        </span>
      </label>
    }
  </div>
</div>
```

**Step 2: Add Groups Display Section (Read-Only)**

**ADD after the Enabled Toggle section** (after line 153, before Actions):
```html
<!-- Groups (Read-Only) -->
@if (!isCreateMode() && user?.groups && user.groups.length > 0) {
  <div>
    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
      >Assigned Groups</label
    >
    <div class="flex flex-wrap gap-2">
      @for (group of user.groups; track group.id) {
        <span
          class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-300"
        >
          {{ group.name }}
        </span>
      }
    </div>
    <p class="mt-2 text-xs text-gray-500 dark:text-gray-400">
      To change group assignments, use the Groups management page.
    </p>
  </div>
}
```

**Step 3: Verify Frontend Linting**

Run:
```bash
cd frontend
pnpm lint
```

Expected: No linting errors. HTML template valid.

---

### Task 11: Remove roles Input from UserEditPanelComponent

**Files:**
- Modify: `frontend/src/app/features/users/user-edit-panel/user-edit-panel.component.ts`

**Step 1: Remove roles @Input**

**DELETE line 30**:
```typescript
@Input() roles: RoleResponse[] = [];
```

**Step 2: Remove RoleResponse Import (if unused)**

Check if `RoleResponse` is still used elsewhere in the file. If not, **REMOVE from imports at line 17**:
```typescript
RoleResponse,
```

**Updated imports:**
```typescript
import {
  UserResponse,
  UpdateUserRequest,
  CreateUserRequest,
} from '../../../core/models/user.model';
```

**Step 3: Verify TypeScript Compilation**

Run:
```bash
cd frontend
pnpm run build --mode development
```

Expected: No TypeScript errors.

---

### Task 12: Remove roles Binding from UserListComponent

**Files:**
- Modify: `frontend/src/app/features/users/user-list/user-list.component.ts`

**Step 1: Remove loadRoles Method Call**

**DELETE line 55** from `ngOnInit`:
```typescript
this.loadRoles();
```

**Updated ngOnInit:**
```typescript
ngOnInit(): void {
  this.loadUsers();

  this.searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe((term) => {
    this.searchTerm.set(term);
    this.page.set(0);
    this.loadUsers();
  });
}
```

**Step 2: Remove loadRoles Method**

**DELETE lines 126-131**:
```typescript
loadRoles(): void {
  this.userService.getRoles().subscribe({
    next: (roles) => this.roles.set(roles),
    error: (err) => console.error('Failed to load roles', err),
  });
}
```

**Step 3: Remove roles Signal**

**DELETE line 21**:
```typescript
roles = signal<RoleResponse[]>([]);
```

**Step 4: Remove RoleResponse Import**

**REMOVE from imports at line 6**:
```typescript
RoleResponse,
```

**Updated imports:**
```typescript
import { PageResponse, UserSearchParams } from '../../../core/models/user.model';
```

**Step 5: Verify TypeScript Compilation**

Run:
```bash
cd frontend
pnpm run build --mode development
```

Expected: No TypeScript errors.

---

### Task 13: Update UserListComponent Template to Remove roles Binding

**Files:**
- Modify: `frontend/src/app/features/users/user-list/user-list.component.html`

**Step 1: Find UserEditPanelComponent Binding**

Search for `<app-user-edit-panel` in the file. It should have a `[roles]` binding.

**REMOVE the [roles] binding**:
```html
[roles]="roles()"
```

**Updated component tag:**
```html
<app-user-edit-panel
  [user]="editingUser()"
  (close)="closeEditPanel()"
  (saved)="onUserUpdated()"
/>
```

**Step 2: Verify Frontend Linting and Build**

Run:
```bash
cd frontend
pnpm lint
pnpm run build --mode development
```

Expected: No errors. Frontend builds successfully without role selection UI.

---

## Batch 6: End-to-End Verification

### Task 14: Run Full Backend Test Suite

**Step 1: Clean Build and Test**

Run:
```bash
cd backend
mvn clean verify
```

Expected: All tests pass, including:
- UserService creates users with "Default Users" group
- UserService updates users without role management
- Flyway migration V16 applies successfully

---

### Task 15: Manual Integration Testing

**Step 1: Start Backend**

Run:
```bash
cd backend
mvn spring-boot:run
```

Expected: Backend starts successfully. Flyway applies V16 migration creating "Default Users" group.

**Step 2: Start Frontend**

In a separate terminal:
```bash
cd frontend
pnpm dev
```

Expected: Frontend starts without compilation errors.

**Step 3: Test User Creation Flow**

1. Login as admin (username: `admin`, password: `admin123`)
2. Navigate to Users page
3. Click "Create User"
4. Fill form: username, email, password (NO role selection visible)
5. Submit form
6. Verify new user appears in list with "Default Users" group badge

Expected: User created successfully with "Default Users" group assignment.

**Step 4: Test User Edit Flow**

1. Click edit on an existing user
2. Verify NO role selection UI visible
3. Verify "Assigned Groups" section displays user's groups (read-only)
4. Update username/email
5. Submit form
6. Verify update succeeds

Expected: User updated without role changes. Groups remain unchanged.

**Step 5: Test Existing Personal Group Users**

1. Find a user with `personal_username` group (created before migration)
2. Edit that user
3. Verify personal group still visible in "Assigned Groups"
4. Update user details
5. Verify personal group remains functional

Expected: Backwards compatibility maintained. Old personal groups work correctly.

**Step 6: Test Group Assignment for Role Management**

1. Navigate to Groups page
2. Create a new group "Moderators"
3. Assign MODERATOR role to "Moderators" group
4. Add a user to "Moderators" group
5. Verify user inherits MODERATOR role permissions

Expected: Group-based role assignment works correctly.

---

## Batch 7: Documentation Updates

### Task 16: Update README with New User Management Workflow

**Files:**
- Modify: `README.md`

**Step 1: Update User Management Section**

Find the user management documentation section (search for "User Management" or similar). Update to reflect:

```markdown
### User Management

**Creating Users:**
- Admin creates users with username, email, and password
- New users automatically join "Default Users" group with USER role
- Role assignment happens exclusively through group membership

**Managing Roles:**
- Navigate to Groups page
- Create groups and assign roles to groups
- Add users to groups to grant permissions
- Users inherit all permissions from their assigned groups

**Default Groups:**
- `Default Users`: All new users automatically join this group with USER role
- Custom groups: Create additional groups for specific role combinations
```

**Step 2: Update API Documentation Notes**

Add a note about the API change:

```markdown
### API Changes

**Breaking Change (v16):**
- `POST /api/users` and `PUT /api/users/{id}` no longer accept `roleIds` parameter
- Role management moved exclusively to group assignment endpoints
- Use `POST /api/groups/{id}/users` to assign users to groups for role management
```

**Step 3: Verify Documentation**

Run:
```bash
cd frontend
pnpm lint
```

Expected: No markdown linting errors (if configured).

---

## Commit Strategy

**Commit 1: Backend Database and Repository**
```bash
git add backend/src/main/resources/db/migration/V16__create_default_users_group.sql
git add backend/src/main/java/com/boilerplate/domain/repository/GroupRepository.java
git commit -m "feat(users): add Default Users group migration and repository method"
```

**Commit 2: Backend Service Layer**
```bash
git add backend/src/main/java/com/boilerplate/application/service/UserService.java
git add backend/src/main/java/com/boilerplate/application/dto/request/CreateUserRequest.java
git add backend/src/main/java/com/boilerplate/application/dto/request/UpdateUserRequest.java
git commit -m "refactor(users): remove personal group creation and role management from UserService"
```

**Commit 3: Backend Tests**
```bash
git add backend/src/test/java/com/boilerplate/application/service/UserServiceTest.java
git commit -m "test(users): update tests for Default Users group assignment"
```

**Commit 4: Frontend Models and Services**
```bash
git add frontend/src/app/core/models/user.model.ts
git commit -m "refactor(users): remove roleIds from user request DTOs"
```

**Commit 5: Frontend Components**
```bash
git add frontend/src/app/features/users/user-edit-panel/
git add frontend/src/app/features/users/user-list/
git commit -m "refactor(users): remove role selection UI and add group display"
```

**Commit 6: Documentation**
```bash
git add README.md
git add docs/plans/2026-02-01-remove-personal-groups/
git commit -m "docs(users): update user management workflow documentation"
```

---

## Rollback Instructions

If issues arise after deployment:

**Step 1: Identify the Issue**
- Check backend logs for "Default Users group not found" errors
- Check frontend console for TypeScript/API errors
- Verify user creation still works

**Step 2: Rollback Database Migration**

If "Default Users" group causes issues:
```bash
# Create rollback migration
# V17__rollback_default_users_group.sql
DELETE FROM group_roles WHERE group_id IN (SELECT id FROM groups WHERE name = 'Default Users');
DELETE FROM user_groups WHERE group_id IN (SELECT id FROM groups WHERE name = 'Default Users');
DELETE FROM groups WHERE name = 'Default Users';
```

**Step 3: Revert Code Changes**

```bash
git revert <commit-hash> --no-edit
# Revert in reverse order: commit 6 → 5 → 4 → 3 → 2 → 1
```

**Step 4: Restore Personal Group Creation**

If complete rollback needed, restore original `UserService.createUser()` logic with personal group creation.

---

## Success Metrics

After implementation, verify:

✅ New users created without personal groups
✅ New users automatically have "Default Users" group
✅ User create/update forms exclude role selection
✅ Groups display (read-only) in user edit panel
✅ Group assignment UI is primary role management interface
✅ Existing personal groups remain functional
✅ All backend tests pass (mvn verify)
✅ All frontend tests pass (pnpm test)
✅ No TypeScript compilation errors
✅ No runtime errors in browser console
✅ API endpoints work correctly (manual testing)

---

## Notes for Engineer

**Key Architectural Decision:**
- Groups are now the **single source of truth** for roles
- Users inherit permissions through group membership
- No direct user-to-role relationship exists
- This simplifies RBAC and scales better for enterprise use

**Backwards Compatibility:**
- Existing personal groups are NOT deleted
- Old users retain their personal groups and roles
- System naturally transitions as new users are created
- Manual cleanup of unused personal groups can happen later

**Testing Focus:**
- Verify "Default Users" group assignment on user creation
- Verify role management no longer in user update flow
- Verify existing personal group users still functional
- Verify group assignment UI works for role changes

**Common Pitfalls:**
- Forgetting to mock `groupRepository.findByName()` in tests
- Not removing `roleIds` from ALL places (DTO, form, submit logic)
- Leaving role-related imports that cause unused import warnings
- Not testing backwards compatibility with existing personal groups
