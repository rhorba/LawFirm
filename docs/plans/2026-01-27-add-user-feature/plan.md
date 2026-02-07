# Add User Feature - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: use executing-plans skill to implement this plan task-by-task.

**Goal:** Wire the existing "Add User" button to open the edit panel in create mode, allowing admins to create new users from the user management view.

**Architecture:** Frontend-only change across 4 files. The `UserEditPanelComponent` gains a create mode (detected when `user` input is `null`). The `UserListComponent` gets an `openCreatePanel()` method. No backend, migration, or new service changes required -- all APIs and DTOs already exist.

**Tech Stack:** Angular 18 (Standalone Components), Tailwind CSS, TypeScript 5.5.

---

## Task 1: Update UserEditPanelComponent TypeScript to support create mode

**Files:**

* Modify: `frontend/src/app/features/users/user-edit-panel/user-edit-panel.component.ts`

**Step 1: Change user input to accept null**

At line 14, add `CreateUserRequest` to the import:

```typescript
import { UserResponse, RoleResponse, UpdateUserRequest, CreateUserRequest } from '../../../core/models/user.model';
```

At line 23, change the `@Input` from required to optional and allow null:

```typescript
// BEFORE:
@Input({ required: true }) user!: UserResponse;

// AFTER:
@Input() user: UserResponse | null = null;
```

**Step 2: Add computed create mode flag**

After the `showPassword` signal (line 33), add:

```typescript
isCreateMode = computed(() => this.user === null);
```

Also add `computed` to the `@angular/core` import at line 1:

```typescript
import {
  Component,
  EventEmitter,
  HostListener,
  Input,
  OnInit,
  Output,
  computed,
  inject,
  signal,
} from '@angular/core';
```

**Step 3: Update ngOnInit to conditionally patch form**

Replace the `ngOnInit` body (lines 43-50) with:

```typescript
ngOnInit(): void {
  if (this.user) {
    this.editForm.patchValue({
      username: this.user.username,
      email: this.user.email,
      enabled: this.user.enabled,
      roleIds: this.user.roles.map((r) => r.id),
    });
  }
}
```

**Step 4: Update onSubmit to fork between create and update**

Replace the `onSubmit` method (lines 71-107) with:

```typescript
onSubmit(): void {
  if (this.editForm.invalid) return;

  // In create mode, validate password is provided
  const formValue = this.editForm.getRawValue();
  if (this.isCreateMode() && !formValue.password) {
    this.error.set('Password is required');
    return;
  }

  this.loading.set(true);
  this.error.set(null);

  if (this.isCreateMode()) {
    const request: CreateUserRequest = {
      username: formValue.username,
      email: formValue.email,
      password: formValue.password,
    };
    const roleIds = formValue.roleIds;
    if (roleIds.length > 0) {
      request.roleIds = roleIds;
    }

    this.userService.createUser(request).subscribe({
      next: () => {
        this.loading.set(false);
        this.saved.emit();
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Failed to create user');
        this.loading.set(false);
      },
    });
  } else {
    const request: UpdateUserRequest = {};

    if (formValue.username !== this.user!.username) request.username = formValue.username;
    if (formValue.email !== this.user!.email) request.email = formValue.email;
    if (formValue.password) request.password = formValue.password;
    if (formValue.enabled !== this.user!.enabled) request.enabled = formValue.enabled;

    const originalRoleIds = this.user!.roles.map((r) => r.id).sort();
    const newRoleIds = formValue.roleIds.sort();
    if (JSON.stringify(originalRoleIds) !== JSON.stringify(newRoleIds)) {
      request.roleIds = formValue.roleIds;
    }

    if (Object.keys(request).length === 0) {
      this.close.emit();
      return;
    }

    this.userService.updateUser(this.user!.id, request).subscribe({
      next: () => {
        this.loading.set(false);
        this.saved.emit();
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Failed to update user');
        this.loading.set(false);
      },
    });
  }
}
```

**Expected result:** Component compiles with no errors. Create mode is detected when `user` is `null`. Submit calls `createUser()` in create mode, `updateUser()` in edit mode.

---

## Task 2: Update UserEditPanelComponent template for create mode

**Files:**

* Modify: `frontend/src/app/features/users/user-edit-panel/user-edit-panel.component.html`

**Step 1: Conditional panel header (line 10)**

```html
<!-- BEFORE: -->
<h2 class="text-lg font-semibold dark:text-white">Edit User</h2>

<!-- AFTER: -->
<h2 class="text-lg font-semibold dark:text-white">{{ isCreateMode() ? 'Create User' : 'Edit User' }}</h2>
```

**Step 2: Conditionally hide user metadata (lines 26-31)**

Wrap the existing `<div class="mb-4 text-sm ...">` block:

```html
<!-- BEFORE: -->
<div class="mb-4 text-sm text-gray-500 dark:text-gray-400 space-y-1">
  <p>ID: {{ user.id }}</p>
  <p>Created: {{ user.createdAt | date:'medium' }}</p>
  <p>Updated: {{ user.updatedAt | date:'medium' }}</p>
</div>

<!-- AFTER: -->
@if (!isCreateMode()) {
  <div class="mb-4 text-sm text-gray-500 dark:text-gray-400 space-y-1">
    <p>ID: {{ user!.id }}</p>
    <p>Created: {{ user!.createdAt | date:'medium' }}</p>
    <p>Updated: {{ user!.updatedAt | date:'medium' }}</p>
  </div>
}
```

**Step 3: Update password field label (lines 70-71)**

```html
<!-- BEFORE: -->
<label for="edit-password" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
  New Password <span class="text-gray-400 dark:text-gray-500">(leave blank to keep current)</span>
</label>

<!-- AFTER: -->
<label for="edit-password" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
  @if (isCreateMode()) {
    Password <span class="text-red-500">*</span>
  } @else {
    New Password <span class="text-gray-400 dark:text-gray-500">(leave blank to keep current)</span>
  }
</label>
```

**Step 4: Conditionally hide the enabled toggle (lines 91-102)**

Wrap the existing enabled toggle `<div>`:

```html
<!-- BEFORE: -->
<div class="flex items-center gap-3">
  <label class="relative inline-flex items-center cursor-pointer">
    ...
  </label>
  <span ...>Account Enabled</span>
</div>

<!-- AFTER: -->
@if (!isCreateMode()) {
  <div class="flex items-center gap-3">
    <label class="relative inline-flex items-center cursor-pointer">
      ...
    </label>
    <span ...>Account Enabled</span>
  </div>
}
```

**Step 5: Conditional submit button text (lines 134-138)**

```html
<!-- BEFORE: -->
@if (loading()) {
  Saving...
} @else {
  Save Changes
}

<!-- AFTER: -->
@if (loading()) {
  {{ isCreateMode() ? 'Creating...' : 'Saving...' }}
} @else {
  {{ isCreateMode() ? 'Create' : 'Save Changes' }}
}
```

**Expected result:** Panel shows "Create User" header, required password asterisk, no metadata section, no enabled toggle, and "Create" button when in create mode.

---

## Task 3: Wire the "Add User" button in UserListComponent

**Files:**

* Modify: `frontend/src/app/features/users/user-list/user-list.component.ts`
* Modify: `frontend/src/app/features/users/user-list/user-list.component.html`

**Step 1: Add openCreatePanel() method to the TypeScript file**

After the `openEditPanel` method (line 277), add:

```typescript
openCreatePanel(): void {
  this.editingUser.set(null);
  this.panelOpen.set(true);
}
```

**Step 2: Wire the "Add User" button click in the template (line 6)**

```html
<!-- BEFORE: -->
<button class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
  Add User
</button>

<!-- AFTER: -->
<button (click)="openCreatePanel()" class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
  Add User
</button>
```

**Step 3: Update the panel binding to remove the non-null assertion (line 229)**

The current template passes `editingUser()!` which would crash when `null`. Change:

```html
<!-- BEFORE: -->
@if (panelOpen()) {
  <app-user-edit-panel
    [user]="editingUser()!"
    [roles]="roles()"
    (close)="closeEditPanel()"
    (saved)="onUserUpdated()"
  />
}

<!-- AFTER: -->
@if (panelOpen()) {
  <app-user-edit-panel
    [user]="editingUser()"
    [roles]="roles()"
    (close)="closeEditPanel()"
    (saved)="onUserUpdated()"
  />
}
```

**Expected result:** Clicking "Add User" opens the panel in create mode (null user). Clicking "Edit" on a row still opens the panel in edit mode (existing user passed).

---

## Task 4: Verify full-stack flow

**Step 1: Compile check**

Run:

```bash
# Frontend type check and lint
cd frontend
pnpm lint
```

Expected: No TypeScript or linting errors.

**Step 2: Manual smoke test**

1. Start backend: `cd backend && mvn spring-boot:run`
2. Start frontend: `cd frontend && pnpm dev`
3. Log in as `admin` / `admin123`
4. Navigate to Users page
5. Click **"Add User"** -- panel should open with:
   - Header: "Create User"
   - Empty username, email fields
   - Password field with red asterisk, no "(leave blank)" hint
   - No enabled toggle
   - No user metadata (ID, dates)
   - Role checkboxes (none selected)
   - "Create" button
6. Fill in username, email, password, optionally select roles
7. Click **"Create"** -- panel closes, user list refreshes with the new user
8. Click **"Edit"** on any user -- panel should still work as before (edit mode)
9. Test error cases:
   - Submit without password in create mode -- error banner: "Password is required"
   - Submit duplicate username -- error banner from 409
   - Submit duplicate email -- error banner from 409

---

## Summary

| Task | Files | Description |
|------|-------|-------------|
| 1 | `user-edit-panel.component.ts` | Add create mode logic: null user, computed flag, forked submit |
| 2 | `user-edit-panel.component.html` | Conditional header, metadata, password label, enabled toggle, button text |
| 3 | `user-list.component.ts` + `.html` | Add `openCreatePanel()`, wire button, fix panel binding |
| 4 | -- | Compile check + manual smoke test |

**Total files modified:** 4 (all frontend, all existing).
**New files:** 0.
**Backend changes:** 0.
