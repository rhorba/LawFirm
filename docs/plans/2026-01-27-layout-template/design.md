# Layout Template Design

## Summary

Introduce a shared layout shell (sidebar + header) for all authenticated routes. Add Profile (read-only) and Settings (theme toggle) views. Pure frontend changes — no backend modifications.

## Decisions

| Decision              | Choice                                                     |
|-----------------------|------------------------------------------------------------|
| Sidebar style         | Collapsible (icons + labels, collapses to icons-only)      |
| Collapse behavior     | Responsive + overlay on mobile                             |
| Header content        | Toggle + breadcrumbs (left), extensible slot (center), user dropdown (right) |
| Profile view          | Read-only (display current user info from memory)          |
| Settings view         | Theme only (dark/light mode toggle, sidebar preference)    |
| Icon library          | Material Icons (`@angular/material`)                       |
| State management      | `ThemeService` with Angular signals + `localStorage`       |
| Dark mode strategy    | Tailwind `darkMode: 'class'` on `<html>`                   |

## Architecture

### Routing Change

Authenticated routes become children of a `LayoutComponent` wrapper. Auth pages remain full-page.

```
AppComponent (<router-outlet>)
+-- /login          -> LoginComponent (full page)
+-- /register       -> RegisterComponent (full page)
+-- LayoutComponent (sidebar + header + <router-outlet>)
    +-- /dashboard  -> DashboardComponent
    +-- /users      -> UserListComponent
    +-- /profile    -> ProfileComponent (NEW)
    +-- /settings   -> SettingsComponent (NEW)
```

`authGuard` moves to the parent `LayoutComponent` route so all children inherit protection.

### New Files

```
features/layout/
  layout.component.ts         # Shell: sidebar + header + router-outlet
  layout.component.html
  sidebar/
    sidebar.component.ts      # Collapsible nav + overlay on mobile
    sidebar.component.html
  header/
    header.component.ts       # Toggle, breadcrumbs, center slot, user dropdown
    header.component.html
features/profile/
  profile.component.ts        # Read-only user info card
  profile.component.html
features/settings/
  settings.component.ts       # Theme toggle, sidebar pref
  settings.component.html
core/services/
  theme.service.ts            # Dark/light mode + sidebar state (localStorage)
```

### Modified Files

- `app.routes.ts` — restructure to nested layout route
- `tailwind.config.js` — add `darkMode: 'class'`
- `login.component.html` — add `dark:` Tailwind classes
- `register.component.html` — add `dark:` Tailwind classes
- `dashboard.component.ts` — add `dark:` Tailwind classes (inline template)
- `user-list.component.html` — add `dark:` Tailwind classes
- `user-edit-panel.component.html` — add `dark:` Tailwind classes

---

## Component Specifications

### 1. Layout Component

CSS Grid shell with fixed header and sidebar.

```
+----------------------------------------------+
| Header (fixed top, full width)         h-16  |
+------------+---------------------------------+
|            |                                 |
|  Sidebar   |       Main Content              |
|  w-64      |       <router-outlet>           |
|  (or w-20  |       p-6, scrollable           |
|  collapsed)|                                 |
|            |                                 |
|  fixed     |                                 |
|  left      |                                 |
+------------+---------------------------------+
```

**Desktop:**
- Sidebar: `w-64` expanded, `w-20` collapsed. Fixed left, full height below header.
- Content: `ml-64` / `ml-20` with `pt-16`. Transitions with `transition-all duration-300`.
- Header: Fixed top, full width. Left padding matches sidebar width.

**Mobile (below `lg`):**
- Sidebar hidden by default. Hamburger opens overlay with `bg-black/50` backdrop.
- Content: No left margin, full width, `pt-16`.
- Tapping backdrop or nav link closes overlay.

### 2. Sidebar Component

**Navigation items:**

| Label     | Icon        | Route      | Permission |
|-----------|-------------|------------|------------|
| Dashboard | `dashboard` | /dashboard | none       |
| Users     | `people`    | /users     | USER_READ  |
| Settings  | `settings`  | /settings  | none       |

- `routerLinkActive` for current route highlighting.
- Permission-guarded items hidden with `@if (authService.hasPermission(...))`.
- Collapsed state: icons only, Tailwind tooltip on hover shows label.
- Footer: user initials circle + username (hidden when collapsed).
- Collapse toggle: chevron button at bottom (`chevron_left` / `chevron_right`).

**Mobile overlay:**
- `fixed inset-y-0 left-0 z-50` with backdrop at `z-40`.
- Slide-in via `translate-x` transition (`-translate-x-full` -> `translate-x-0`).
- Closes on: backdrop click, nav link click, Escape key.

### 3. Header Component

Three-zone horizontal bar:

```
+-------------------+----------------------+-------------------+
|  Left             |  Center (slot)       |  Right            |
|  hamburger +      |  empty <div> for     |  user dropdown    |
|  breadcrumbs      |  future search       |                   |
+-------------------+----------------------+-------------------+
```

**Left zone:**
- Hamburger (`menu` icon): toggles sidebar on desktop, opens overlay on mobile.
- Breadcrumbs: Built from Router `NavigationEnd` events. Splits URL segments into labels. Last segment bold/non-clickable.

**Center zone:**
- Empty `<div class="flex-1">` — extensible slot for future search bar.

**Right zone:**
- User initials circle button. Click toggles dropdown.
- Dropdown (absolute positioned, closes on outside click):
  - Profile (`person` icon, `routerLink="/profile"`)
  - Settings (`settings` icon, `routerLink="/settings"`)
  - Divider
  - Logout (`logout` icon, calls `authService.logout()`)

### 4. Profile Component

Read-only card displaying `authService.currentUser()` signal data. No API call.

```
+-------------------------------------+
|  Profile                            |
+-------------------------------------+
|  [Initials]  username               |
|              role badges            |
|                                     |
|  Email         user@example.com     |
|  Status        Active / Inactive    |
|  Member since  Jan 15, 2025         |
|  Last updated  Jan 27, 2026         |
|                                     |
|  Roles & Permissions                |
|  +-----------------------------+    |
|  | ADMIN                       |    |
|  |  USER_READ, USER_CREATE ... |    |
|  +-----------------------------+    |
+-------------------------------------+
```

### 5. Settings Component

Two toggle cards, both backed by `ThemeService`:

| Setting           | Control       | localStorage key           |
|-------------------|---------------|----------------------------|
| Dark Mode         | Toggle switch | `theme-dark-mode`          |
| Sidebar Collapsed | Toggle switch | `theme-sidebar-collapsed`  |

Toggle switches: Tailwind-styled pill button with sliding circle. No Angular Material form controls.

Changes apply immediately via signals. No save button.

### 6. ThemeService

```typescript
class ThemeService {
  darkMode = signal<boolean>(false);
  sidebarCollapsed = signal<boolean>(false);
  mobileSidebarOpen = signal<boolean>(false);

  toggleDarkMode()       // flip + save + toggle 'dark' class on <html>
  toggleSidebar()        // flip + save to localStorage
  openMobileSidebar()    // set true
  closeMobileSidebar()   // set false
}
```

Constructor reads `localStorage` on init. Applies `dark` class to `document.documentElement` if saved.

---

## Dark Mode Integration

**Tailwind config:** Set `darkMode: 'class'`.

**Existing components affected** (add `dark:` class variants only, no logic changes):
- `LoginComponent` — card bg, inputs, text
- `RegisterComponent` — card bg, inputs, text
- `DashboardComponent` — cards, text, role badges
- `UserListComponent` — table, filters, bulk bar, pagination
- `UserEditPanelComponent` — overlay, panel bg, form fields

---

## Dependencies

New package: `@angular/material` (for `MatIconModule` / `<mat-icon>`).

No backend changes. No new API endpoints. No Flyway migrations.
