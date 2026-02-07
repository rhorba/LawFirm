# Add User Feature - Design

## Summary

Extend the existing `UserEditPanelComponent` slide-out panel to support a **create mode** alongside its current edit mode. No backend changes needed -- all endpoints, DTOs, and services are already implemented.

## Files Changed

| File | Change |
|------|--------|
| `user-edit-panel.component.ts` | Add create mode logic: null user detection, password required toggle, `createUser()` call |
| `user-edit-panel.component.html` | Conditional header/button text, hide enabled toggle and metadata in create mode |
| `user-list.component.ts` | Add `openCreatePanel()` method |
| `user-list.component.html` | Wire "Add User" button click handler |

## Architecture & Data Flow

**Mode detection:** The `user` input becomes `UserResponse | null`. When `null`, the panel operates in create mode.

**Create flow:**
```
1. Admin clicks "Add User" button
2. UserListComponent sets editingUser = null, showEditPanel = true
3. UserEditPanelComponent detects null user -> create mode
4. Admin fills: username, email, password (all required), roles (optional)
5. Submit -> userService.createUser({ username, email, password, roleIds })
6. POST /api/users -> 201 Created + UserResponse
7. Panel emits "saved", parent closes panel and reloads list
```

No new endpoints, DTOs, migrations, or services required.

## Form Behavior by Mode

| Field | Create Mode | Edit Mode (unchanged) |
|-------|-------------|----------------------|
| Username | Required, 3-50 chars | Required, 3-50 chars |
| Email | Required, valid email | Required, valid email |
| Password | **Required**, min 8 chars | Optional, min 8 chars |
| Enabled | Hidden (defaults to true) | Toggle switch |
| Role checkboxes | Shown, none pre-selected | Shown, current roles pre-selected |

**UI differences in create mode:**
- Header: "Create User" instead of "Edit User"
- Password: required (shown with asterisk)
- Enabled toggle: hidden (backend defaults to `true`)
- User metadata (ID, dates): hidden
- Submit button: "Create" instead of "Save Changes"

## Submit Logic

```typescript
if (this.isCreateMode) {
  const request: CreateUserRequest = {
    username: form.username,
    email: form.email,
    password: form.password,
    roleIds: this.selectedRoleIds().length > 0 ? this.selectedRoleIds() : undefined
  };
  this.userService.createUser(request).subscribe({
    next: () => this.saved.emit(),
    error: (err) => this.error.set(err.error?.message || 'Failed to create user')
  });
} else {
  // Existing update logic (unchanged)
}
```

## Error Handling

| Scenario | HTTP | Exception | Display |
|----------|------|-----------|---------|
| Duplicate username | 409 | `DuplicateResourceException` | Error banner: "Username already exists" |
| Duplicate email | 409 | `DuplicateResourceException` | Error banner: "Email already exists" |
| Validation failure | 400 | `MethodArgumentNotValidException` | Field-level errors |
| Unauthorized | 403 | Spring Security | Handled by `errorInterceptor` |

**Double-submit prevention:** Disable "Create" button during request (reuse existing `saving` signal).

## Edge Cases

- Escape key / overlay click closes panel (existing behavior, no change).
- No dirty form warning (YAGNI -- edit panel doesn't have it either).
- No backend changes or new tests needed -- `UserService.createUser()` already has unit tests.

## Decisions Made

| Decision | Choice | Rationale |
|----------|--------|-----------|
| UI pattern | Reuse edit panel | Less code, consistent UX |
| Role assignment | Show checkboxes on create | Backend supports optional roleIds, defaults to USER |
| Post-create behavior | Close panel + refresh list | Matches existing edit save behavior |
