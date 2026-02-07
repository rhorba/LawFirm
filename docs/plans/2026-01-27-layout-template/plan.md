# Layout Template Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: use executing-plans skill to implement this plan task-by-task.

**Goal:** Introduce a shared layout shell (collapsible sidebar + header) for all authenticated routes, add Profile and Settings views, and integrate dark mode across all existing components.

**Architecture:** Pure frontend changes. A new `LayoutComponent` wraps all authenticated child routes (dashboard, users, profile, settings) with a persistent sidebar and header. `ThemeService` manages dark mode and sidebar state via Angular signals persisted to `localStorage`. Material Icons via `@angular/material`.

**Tech Stack:** Angular 18 (Standalone), Tailwind CSS 3.4, `@angular/material` (icons only), TypeScript 5.5.

---

## Batch 1: Foundation (Dependencies, Config, ThemeService)

### Task 1: Install @angular/material

**Files:**
- Modify: `frontend/package.json`

**Step 1:** Run in `frontend/` directory:
```bash
pnpm add @angular/material @angular/cdk
```

**Step 2: Verify**
```bash
pnpm list @angular/material
```
Expected: `@angular/material` and `@angular/cdk` appear in dependencies.

---

### Task 2: Add Material Icons stylesheet and configure dark mode

**Files:**
- Modify: `frontend/src/index.html`
- Modify: `frontend/tailwind.config.js`
- Modify: `frontend/src/styles.css`

**Step 1:** Add Material Icons font to `src/index.html` in `<head>`:
```html
<link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
```

**Step 2:** Update `tailwind.config.js` to enable class-based dark mode:
```js
/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: 'class',
  content: ["./src/**/*.{html,ts}"],
  theme: {
    extend: {},
  },
  plugins: [],
}
```

**Step 3:** Update `src/styles.css` to support dark mode on body:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

body {
  @apply bg-gray-50 text-gray-900 dark:bg-gray-900 dark:text-gray-100;
}
```

**Step 4: Verify**
```bash
pnpm build
```
Expected: Build succeeds with no errors.

---

### Task 3: Create ThemeService

**Files:**
- Create: `frontend/src/app/core/services/theme.service.ts`

**Step 1:** Create the service:
```typescript
import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  darkMode = signal<boolean>(false);
  sidebarCollapsed = signal<boolean>(false);
  mobileSidebarOpen = signal<boolean>(false);

  constructor() {
    const savedDark = localStorage.getItem('theme-dark-mode');
    const savedCollapsed = localStorage.getItem('theme-sidebar-collapsed');

    if (savedDark === 'true') {
      this.darkMode.set(true);
      document.documentElement.classList.add('dark');
    }

    if (savedCollapsed === 'true') {
      this.sidebarCollapsed.set(true);
    }
  }

  toggleDarkMode(): void {
    const newValue = !this.darkMode();
    this.darkMode.set(newValue);
    localStorage.setItem('theme-dark-mode', String(newValue));
    if (newValue) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  toggleSidebar(): void {
    const newValue = !this.sidebarCollapsed();
    this.sidebarCollapsed.set(newValue);
    localStorage.setItem('theme-sidebar-collapsed', String(newValue));
  }

  openMobileSidebar(): void {
    this.mobileSidebarOpen.set(true);
  }

  closeMobileSidebar(): void {
    this.mobileSidebarOpen.set(false);
  }
}
```

**Step 2: Verify**
```bash
pnpm build
```
Expected: Build succeeds.

---

## Batch 2: Layout Shell (Sidebar, Header, Layout)

### Task 4: Create SidebarComponent

**Files:**
- Create: `frontend/src/app/features/layout/sidebar/sidebar.component.ts`
- Create: `frontend/src/app/features/layout/sidebar/sidebar.component.html`

**Step 1:** Create `sidebar.component.ts`:
```typescript
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { ThemeService } from '../../../core/services/theme.service';
import { AuthService } from '../../../core/services/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  permission?: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, MatIconModule],
  templateUrl: './sidebar.component.html'
})
export class SidebarComponent {
  themeService = inject(ThemeService);
  authService = inject(AuthService);

  navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'Users', icon: 'people', route: '/users', permission: 'USER_READ' },
    { label: 'Settings', icon: 'settings', route: '/settings' }
  ];

  get userInitial(): string {
    const user = this.authService.currentUser();
    return user ? user.username.charAt(0).toUpperCase() : '?';
  }

  get username(): string {
    const user = this.authService.currentUser();
    return user ? user.username : '';
  }

  onNavClick(): void {
    // Close mobile sidebar when navigating
    this.themeService.closeMobileSidebar();
  }
}
```

**Step 2:** Create `sidebar.component.html`:
```html
<!-- Mobile Backdrop -->
@if (themeService.mobileSidebarOpen()) {
  <div
    class="fixed inset-0 bg-black/50 z-40 lg:hidden"
    (click)="themeService.closeMobileSidebar()"
  ></div>
}

<!-- Sidebar -->
<aside
  class="fixed inset-y-0 left-0 z-50 flex flex-col bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 pt-16 transition-all duration-300"
  [class.w-64]="!themeService.sidebarCollapsed()"
  [class.w-20]="themeService.sidebarCollapsed()"
  [class.-translate-x-full]="!themeService.mobileSidebarOpen()"
  [class.translate-x-0]="themeService.mobileSidebarOpen()"
  [class.lg\:translate-x-0]="true"
>
  <!-- Navigation -->
  <nav class="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
    @for (item of navItems; track item.route) {
      @if (!item.permission || authService.hasPermission(item.permission)) {
        <a
          [routerLink]="item.route"
          routerLinkActive="bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300"
          [routerLinkActiveOptions]="{ exact: item.route === '/dashboard' }"
          (click)="onNavClick()"
          class="group flex items-center gap-3 px-3 py-2.5 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          [class.justify-center]="themeService.sidebarCollapsed()"
        >
          <mat-icon class="flex-shrink-0">{{ item.icon }}</mat-icon>
          @if (!themeService.sidebarCollapsed()) {
            <span class="text-sm font-medium">{{ item.label }}</span>
          }
        </a>
      }
    }
  </nav>

  <!-- Collapse Toggle (desktop only) -->
  <div class="hidden lg:block px-3 py-2 border-t border-gray-200 dark:border-gray-700">
    <button
      (click)="themeService.toggleSidebar()"
      class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
      [class.justify-center]="themeService.sidebarCollapsed()"
    >
      <mat-icon>{{ themeService.sidebarCollapsed() ? 'chevron_right' : 'chevron_left' }}</mat-icon>
      @if (!themeService.sidebarCollapsed()) {
        <span class="text-sm">Collapse</span>
      }
    </button>
  </div>

  <!-- User Footer -->
  <div class="px-3 py-3 border-t border-gray-200 dark:border-gray-700">
    <div
      class="flex items-center gap-3 px-3 py-2"
      [class.justify-center]="themeService.sidebarCollapsed()"
    >
      <div class="flex-shrink-0 w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center text-white text-sm font-medium">
        {{ userInitial }}
      </div>
      @if (!themeService.sidebarCollapsed()) {
        <span class="text-sm font-medium text-gray-700 dark:text-gray-300 truncate">{{ username }}</span>
      }
    </div>
  </div>
</aside>
```

**Step 3: Verify**
```bash
pnpm build
```
Expected: Build succeeds (component not routed yet, just compiles).

---

### Task 5: Create HeaderComponent

**Files:**
- Create: `frontend/src/app/features/layout/header/header.component.ts`
- Create: `frontend/src/app/features/layout/header/header.component.html`

**Step 1:** Create `header.component.ts`:
```typescript
import { Component, inject, signal, HostListener, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { filter } from 'rxjs';
import { ThemeService } from '../../../core/services/theme.service';
import { AuthService } from '../../../core/services/auth.service';

interface Breadcrumb {
  label: string;
  url: string;
  isLast: boolean;
}

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule],
  templateUrl: './header.component.html'
})
export class HeaderComponent {
  themeService = inject(ThemeService);
  authService = inject(AuthService);
  private router = inject(Router);
  private elRef = inject(ElementRef);

  dropdownOpen = signal(false);
  breadcrumbs = signal<Breadcrumb[]>([]);

  private readonly labelMap: Record<string, string> = {
    dashboard: 'Dashboard',
    users: 'Users',
    profile: 'Profile',
    settings: 'Settings'
  };

  constructor() {
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event) => {
        const navEnd = event as NavigationEnd;
        this.buildBreadcrumbs(navEnd.urlAfterRedirects || navEnd.url);
        this.dropdownOpen.set(false);
      });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elRef.nativeElement.contains(event.target)) {
      this.dropdownOpen.set(false);
    }
  }

  toggleDropdown(): void {
    this.dropdownOpen.update(v => !v);
  }

  onMenuClick(): void {
    if (window.innerWidth < 1024) {
      this.themeService.openMobileSidebar();
    } else {
      this.themeService.toggleSidebar();
    }
  }

  logout(): void {
    this.dropdownOpen.set(false);
    this.authService.logout();
  }

  get userInitial(): string {
    const user = this.authService.currentUser();
    return user ? user.username.charAt(0).toUpperCase() : '?';
  }

  private buildBreadcrumbs(url: string): void {
    const segments = url.split('/').filter(s => s);
    const crumbs: Breadcrumb[] = [{ label: 'Home', url: '/dashboard', isLast: segments.length === 0 }];

    let currentUrl = '';
    segments.forEach((segment, index) => {
      currentUrl += `/${segment}`;
      crumbs.push({
        label: this.labelMap[segment] || segment.charAt(0).toUpperCase() + segment.slice(1),
        url: currentUrl,
        isLast: index === segments.length - 1
      });
    });

    this.breadcrumbs.set(crumbs);
  }
}
```

**Step 2:** Create `header.component.html`:
```html
<header
  class="fixed top-0 right-0 left-0 z-30 h-16 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 flex items-center px-4 transition-all duration-300"
  [class.lg\:pl-64]="!themeService.sidebarCollapsed()"
  [class.lg\:pl-20]="themeService.sidebarCollapsed()"
>
  <!-- Left: Hamburger + Breadcrumbs -->
  <div class="flex items-center gap-3">
    <button
      (click)="onMenuClick()"
      class="p-2 rounded-lg text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
    >
      <mat-icon>menu</mat-icon>
    </button>

    <!-- Breadcrumbs (hidden on mobile) -->
    <nav class="hidden md:flex items-center gap-1 text-sm">
      @for (crumb of breadcrumbs(); track crumb.url) {
        @if (!crumb.isLast) {
          <a [routerLink]="crumb.url" class="text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200">
            {{ crumb.label }}
          </a>
          <span class="text-gray-400 dark:text-gray-500">/</span>
        } @else {
          <span class="text-gray-900 dark:text-white font-medium">{{ crumb.label }}</span>
        }
      }
    </nav>
  </div>

  <!-- Center: Extensible slot -->
  <div class="flex-1"></div>

  <!-- Right: User Dropdown -->
  <div class="relative">
    <button
      (click)="toggleDropdown()"
      class="flex items-center gap-2 p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
    >
      <div class="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center text-white text-sm font-medium">
        {{ userInitial }}
      </div>
    </button>

    <!-- Dropdown Menu -->
    @if (dropdownOpen()) {
      <div class="absolute right-0 top-full mt-2 w-48 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-1 z-50">
        <a
          routerLink="/profile"
          (click)="dropdownOpen.set(false)"
          class="flex items-center gap-3 px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
        >
          <mat-icon class="text-lg">person</mat-icon>
          Profile
        </a>
        <a
          routerLink="/settings"
          (click)="dropdownOpen.set(false)"
          class="flex items-center gap-3 px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
        >
          <mat-icon class="text-lg">settings</mat-icon>
          Settings
        </a>
        <hr class="my-1 border-gray-200 dark:border-gray-700" />
        <button
          (click)="logout()"
          class="w-full flex items-center gap-3 px-4 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-gray-100 dark:hover:bg-gray-700"
        >
          <mat-icon class="text-lg">logout</mat-icon>
          Logout
        </button>
      </div>
    }
  </div>
</header>
```

**Step 3: Verify**
```bash
pnpm build
```
Expected: Build succeeds.

---

### Task 6: Create LayoutComponent and update routing

**Files:**
- Create: `frontend/src/app/features/layout/layout.component.ts`
- Create: `frontend/src/app/features/layout/layout.component.html`
- Modify: `frontend/src/app/app.routes.ts`

**Step 1:** Create `layout.component.ts`:
```typescript
import { Component, inject, HostListener } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from '../../core/services/theme.service';
import { SidebarComponent } from './sidebar/sidebar.component';
import { HeaderComponent } from './header/header.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, HeaderComponent],
  templateUrl: './layout.component.html'
})
export class LayoutComponent {
  themeService = inject(ThemeService);

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.themeService.closeMobileSidebar();
  }
}
```

**Step 2:** Create `layout.component.html`:
```html
<app-header />
<app-sidebar />

<!-- Main Content -->
<main
  class="pt-16 min-h-screen transition-all duration-300 bg-gray-50 dark:bg-gray-900"
  [class.lg\:ml-64]="!themeService.sidebarCollapsed()"
  [class.lg\:ml-20]="themeService.sidebarCollapsed()"
>
  <div class="p-6">
    <router-outlet />
  </div>
</main>
```

**Step 3:** Replace `frontend/src/app/app.routes.ts` entirely:
```typescript
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/layout/layout.component').then(m => m.LayoutComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'users',
        loadComponent: () => import('./features/users/user-list/user-list.component').then(m => m.UserListComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent)
      },
      {
        path: 'settings',
        loadComponent: () => import('./features/settings/settings.component').then(m => m.SettingsComponent)
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },
  {
    path: '**',
    redirectTo: '/dashboard'
  }
];
```

**Step 4: Verify**
```bash
pnpm build
```
Expected: Build will fail until Profile and Settings components exist (Task 7 & 8). That's OK — continue to next tasks.

---

## Batch 3: New Views (Profile, Settings) + Dashboard Cleanup

### Task 7: Create ProfileComponent

**Files:**
- Create: `frontend/src/app/features/profile/profile.component.ts`
- Create: `frontend/src/app/features/profile/profile.component.html`

**Step 1:** Create `profile.component.ts`:
```typescript
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.component.html'
})
export class ProfileComponent {
  authService = inject(AuthService);

  get userInitial(): string {
    const user = this.authService.currentUser();
    return user ? user.username.charAt(0).toUpperCase() : '?';
  }
}
```

**Step 2:** Create `profile.component.html`:
```html
<h1 class="text-2xl font-bold mb-6 text-gray-900 dark:text-white">Profile</h1>

@if (authService.currentUser(); as user) {
  <div class="bg-white dark:bg-gray-800 shadow-md rounded-lg p-6 mb-6">
    <!-- User Header -->
    <div class="flex items-center gap-4 mb-6">
      <div class="w-16 h-16 bg-blue-600 rounded-full flex items-center justify-center text-white text-2xl font-bold">
        {{ userInitial }}
      </div>
      <div>
        <h2 class="text-xl font-semibold text-gray-900 dark:text-white">{{ user.username }}</h2>
        <div class="flex gap-2 mt-1">
          @for (role of user.roles; track role.id) {
            <span class="inline-flex items-center px-2.5 py-0.5 text-xs font-medium bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 rounded-full">
              {{ role.name }}
            </span>
          }
        </div>
      </div>
    </div>

    <!-- User Details -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div>
        <p class="text-sm text-gray-500 dark:text-gray-400">Email</p>
        <p class="text-gray-900 dark:text-white">{{ user.email }}</p>
      </div>
      <div>
        <p class="text-sm text-gray-500 dark:text-gray-400">Status</p>
        @if (user.enabled) {
          <span class="text-green-600 dark:text-green-400 font-medium">Active</span>
        } @else {
          <span class="text-yellow-600 dark:text-yellow-400 font-medium">Inactive</span>
        }
      </div>
      <div>
        <p class="text-sm text-gray-500 dark:text-gray-400">Member Since</p>
        <p class="text-gray-900 dark:text-white">{{ user.createdAt | date:'longDate' }}</p>
      </div>
      <div>
        <p class="text-sm text-gray-500 dark:text-gray-400">Last Updated</p>
        <p class="text-gray-900 dark:text-white">{{ user.updatedAt | date:'longDate' }}</p>
      </div>
    </div>
  </div>

  <!-- Roles & Permissions -->
  <div class="bg-white dark:bg-gray-800 shadow-md rounded-lg p-6">
    <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">Roles & Permissions</h3>
    @for (role of user.roles; track role.id) {
      <div class="mb-4 last:mb-0">
        <h4 class="font-medium text-gray-900 dark:text-white mb-2">{{ role.name }}</h4>
        @if (role.description) {
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-2">{{ role.description }}</p>
        }
        <div class="flex flex-wrap gap-2">
          @for (perm of role.permissions; track perm.id) {
            <span class="inline-flex items-center px-2 py-1 text-xs font-medium bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded">
              {{ perm.name }}
            </span>
          }
        </div>
      </div>
    }
  </div>
}
```

**Step 3: Verify**
```bash
pnpm build
```
Expected: May still fail until SettingsComponent exists (Task 8).

---

### Task 8: Create SettingsComponent

**Files:**
- Create: `frontend/src/app/features/settings/settings.component.ts`
- Create: `frontend/src/app/features/settings/settings.component.html`

**Step 1:** Create `settings.component.ts`:
```typescript
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './settings.component.html'
})
export class SettingsComponent {
  themeService = inject(ThemeService);
}
```

**Step 2:** Create `settings.component.html`:
```html
<h1 class="text-2xl font-bold mb-6 text-gray-900 dark:text-white">Settings</h1>

<div class="space-y-4">
  <!-- Dark Mode Toggle -->
  <div class="bg-white dark:bg-gray-800 shadow-md rounded-lg p-6 flex items-center justify-between">
    <div>
      <h3 class="text-lg font-medium text-gray-900 dark:text-white">Dark Mode</h3>
      <p class="text-sm text-gray-500 dark:text-gray-400">Toggle between light and dark theme</p>
    </div>
    <button
      (click)="themeService.toggleDarkMode()"
      class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-800"
      [class.bg-blue-600]="themeService.darkMode()"
      [class.bg-gray-200]="!themeService.darkMode()"
      role="switch"
      [attr.aria-checked]="themeService.darkMode()"
    >
      <span
        class="inline-block h-4 w-4 transform rounded-full bg-white transition-transform"
        [class.translate-x-6]="themeService.darkMode()"
        [class.translate-x-1]="!themeService.darkMode()"
      ></span>
    </button>
  </div>

  <!-- Sidebar Collapsed Toggle -->
  <div class="bg-white dark:bg-gray-800 shadow-md rounded-lg p-6 flex items-center justify-between">
    <div>
      <h3 class="text-lg font-medium text-gray-900 dark:text-white">Compact Sidebar</h3>
      <p class="text-sm text-gray-500 dark:text-gray-400">Collapse sidebar to icons only</p>
    </div>
    <button
      (click)="themeService.toggleSidebar()"
      class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-800"
      [class.bg-blue-600]="themeService.sidebarCollapsed()"
      [class.bg-gray-200]="!themeService.sidebarCollapsed()"
      role="switch"
      [attr.aria-checked]="themeService.sidebarCollapsed()"
    >
      <span
        class="inline-block h-4 w-4 transform rounded-full bg-white transition-transform"
        [class.translate-x-6]="themeService.sidebarCollapsed()"
        [class.translate-x-1]="!themeService.sidebarCollapsed()"
      ></span>
    </button>
  </div>
</div>
```

**Step 3: Verify full build**
```bash
pnpm build
```
Expected: Build succeeds. All routes now resolve. App should load with sidebar + header.

---

### Task 9: Clean up DashboardComponent

**Files:**
- Modify: `frontend/src/app/features/dashboard/dashboard.component.ts`

**Step 1:** The dashboard currently has its own container padding, navigation cards (Users, Settings, Logout) that are now redundant since the sidebar handles navigation and the header handles logout. Update the inline template to remove the logout button, make the Settings card link to `/settings`, and remove the outer `container` padding (the layout already provides `p-6`):

Replace the entire inline template with:
```typescript
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <h1 class="text-2xl font-bold mb-6 text-gray-900 dark:text-white">Dashboard</h1>

    @if (authService.currentUser(); as user) {
      <div class="bg-white dark:bg-gray-800 shadow-md rounded-lg p-6 mb-6">
        <h2 class="text-xl font-semibold mb-4 text-gray-900 dark:text-white">Welcome, {{ user.username }}!</h2>
        <p class="text-gray-600 dark:text-gray-400">Email: {{ user.email }}</p>
        <div class="mt-4">
          <h3 class="font-semibold mb-2 text-gray-900 dark:text-white">Your Roles:</h3>
          <div class="flex gap-2">
            @for (role of user.roles; track role.id) {
              <span class="inline-flex items-center px-3 py-1 text-sm font-medium bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 rounded-full">
                {{ role.name }}
              </span>
            }
          </div>
        </div>
      </div>
    }

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      @if (authService.hasPermission('USER_READ')) {
        <a routerLink="/users" class="bg-white dark:bg-gray-800 shadow-md rounded-lg p-6 hover:shadow-lg transition text-gray-900 dark:text-white">
          <h3 class="text-lg font-semibold mb-2">Users</h3>
          <p class="text-gray-600 dark:text-gray-400">Manage user accounts</p>
        </a>
      }

      <a routerLink="/settings" class="bg-white dark:bg-gray-800 shadow-md rounded-lg p-6 hover:shadow-lg transition text-gray-900 dark:text-white">
        <h3 class="text-lg font-semibold mb-2">Settings</h3>
        <p class="text-gray-600 dark:text-gray-400">Configure application</p>
      </a>
    </div>
  `
})
export class DashboardComponent {
  authService = inject(AuthService);
}
```

**Step 2: Verify**
```bash
pnpm build
```
Expected: Build succeeds.

---

## Batch 4: Dark Mode for Existing Components

### Task 10: Add dark mode to LoginComponent

**Files:**
- Modify: `frontend/src/app/features/auth/login/login.component.html`

**Step 1:** Add `dark:` class variants throughout the template. Key changes:

| Element | Add classes |
|---------|------------|
| Outer `div` (`bg-gray-100`) | `dark:bg-gray-900` |
| Card (`bg-white`) | `dark:bg-gray-800` |
| `h2` | `dark:text-white` |
| Error banner (`bg-red-100 border-red-400 text-red-700`) | `dark:bg-red-900/30 dark:border-red-600 dark:text-red-400` |
| Labels (`text-gray-700`) | `dark:text-gray-300` |
| Inputs (`border-gray-300`) | `dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400` |
| Checkbox label (`text-gray-700`) | `dark:text-gray-300` |
| Footer text (`text-gray-600`) | `dark:text-gray-400` |
| Link (`text-blue-600`) | `dark:text-blue-400` |

**Step 2: Verify**
```bash
pnpm build
```

---

### Task 11: Add dark mode to RegisterComponent

**Files:**
- Modify: `frontend/src/app/features/auth/register/register.component.html`

**Step 1:** Same pattern as login — add `dark:` variants:

| Element | Add classes |
|---------|------------|
| Outer `div` (`bg-gray-100`) | `dark:bg-gray-900` |
| Card (`bg-white`) | `dark:bg-gray-800` |
| `h2` | `dark:text-white` |
| Error banner | `dark:bg-red-900/30 dark:border-red-600 dark:text-red-400` |
| Labels (`text-gray-700`) | `dark:text-gray-300` |
| All inputs | `dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400` |
| Validation errors (`text-red-600`) | `dark:text-red-400` |
| Password toggle buttons (`text-gray-500 hover:text-gray-700`) | `dark:text-gray-400 dark:hover:text-gray-200` |
| Footer text (`text-gray-600`) | `dark:text-gray-400` |
| Link | `dark:text-blue-400` |

**Step 2: Verify**
```bash
pnpm build
```

---

### Task 12: Add dark mode to UserListComponent

**Files:**
- Modify: `frontend/src/app/features/users/user-list/user-list.component.html`

**Step 1:** Add `dark:` variants throughout:

| Element | Add classes |
|---------|------------|
| `h1` | `dark:text-white` |
| Search/filter bar (`bg-white`) | `dark:bg-gray-800` |
| Search input | `dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400` |
| Selects | `dark:bg-gray-700 dark:border-gray-600 dark:text-white` |
| Show deleted label (`text-gray-700`) | `dark:text-gray-300` |
| Bulk action bar (`bg-blue-50 border-blue-200`) | `dark:bg-blue-900/20 dark:border-blue-800` |
| Bulk text (`text-blue-800`) | `dark:text-blue-300` |
| Table wrapper (`bg-white`) | `dark:bg-gray-800` |
| `thead` (`bg-gray-50`) | `dark:bg-gray-700` |
| `th` text (`text-gray-500`) | `dark:text-gray-400` |
| `tbody` (`bg-white divide-gray-200`) | `dark:bg-gray-800 dark:divide-gray-700` |
| `td` text | `dark:text-white` or `dark:text-gray-300` |
| Role badges | `dark:bg-blue-900 dark:text-blue-200` |
| Empty state text (`text-gray-500`) | `dark:text-gray-400` |
| Pagination bar (`bg-gray-50`) | `dark:bg-gray-700` |
| Pagination text (`text-gray-700`) | `dark:text-gray-300` |
| Pagination buttons (`border hover:bg-gray-100`) | `dark:border-gray-600 dark:hover:bg-gray-600 dark:text-gray-300` |
| Deleted row (`bg-red-50`) | `dark:bg-red-900/20` |
| Error banner | `dark:bg-red-900/30 dark:border-red-600 dark:text-red-400` |

**Step 2: Verify**
```bash
pnpm build
```

---

### Task 13: Add dark mode to UserEditPanelComponent

**Files:**
- Modify: `frontend/src/app/features/users/user-edit-panel/user-edit-panel.component.html`

**Step 1:** Add `dark:` variants:

| Element | Add classes |
|---------|------------|
| Panel (`bg-white shadow-xl`) | `dark:bg-gray-800` |
| Header border (`border-b`) | `dark:border-gray-700` |
| Header title | `dark:text-white` |
| Close button (`text-gray-400 hover:text-gray-600`) | `dark:text-gray-500 dark:hover:text-gray-300` |
| Error banner | `dark:bg-red-900/30 dark:border-red-600 dark:text-red-400` |
| Meta text (`text-gray-500`) | `dark:text-gray-400` |
| Labels (`text-gray-700`) | `dark:text-gray-300` |
| All inputs | `dark:bg-gray-700 dark:border-gray-600 dark:text-white` |
| Enabled toggle label | `dark:text-gray-300` |
| Role labels | `dark:text-gray-300` |
| Role descriptions (`text-gray-500`) | `dark:text-gray-400` |
| Cancel button (`border-gray-300 hover:bg-gray-50`) | `dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700` |
| Action border (`border-t`) | `dark:border-gray-700` |

**Step 2: Verify full build**
```bash
pnpm build
```
Expected: Full build succeeds with zero errors.

---

## Batch 5: Final Verification

### Task 14: End-to-end verification

**Step 1:** Start the backend:
```bash
cd backend && mvn spring-boot:run
```

**Step 2:** Start the frontend (separate terminal):
```bash
cd frontend && pnpm dev
```

**Step 3:** Manual test checklist:
- [ ] Login page loads at `/login` (no sidebar/header)
- [ ] Register page loads at `/register` (no sidebar/header)
- [ ] After login, redirects to `/dashboard` with sidebar + header visible
- [ ] Sidebar shows Dashboard, Users (if admin), Settings
- [ ] Sidebar collapse toggle works (desktop)
- [ ] Sidebar hamburger opens overlay on mobile viewport
- [ ] Escape key closes mobile sidebar
- [ ] Clicking backdrop closes mobile sidebar
- [ ] Header breadcrumbs update when navigating
- [ ] User dropdown shows Profile, Settings, Logout
- [ ] Profile link opens `/profile` with user info
- [ ] Settings link opens `/settings` with toggle switches
- [ ] Dark mode toggle applies immediately (body, sidebar, header, content)
- [ ] Sidebar collapsed toggle works from settings page
- [ ] Logout from dropdown clears session and returns to `/login`
- [ ] Users page loads with full table (if admin)
- [ ] Edit panel opens correctly within layout
- [ ] Refresh browser — dark mode and sidebar state persist from localStorage
- [ ] Navigate directly to `/users` when logged out — redirects to `/login`

**Step 4:** Run lint:
```bash
cd frontend && pnpm lint
```
Expected: No errors.

---

## Summary

| Batch | Tasks | Description |
|-------|-------|-------------|
| 1 | 1-3 | Dependencies, config, ThemeService |
| 2 | 4-6 | Sidebar, Header, Layout shell + routing |
| 3 | 7-9 | Profile, Settings, Dashboard cleanup |
| 4 | 10-13 | Dark mode for all existing components |
| 5 | 14 | End-to-end verification |

**Total new files:** 10
**Total modified files:** 7
**Backend changes:** None
**New dependency:** `@angular/material`, `@angular/cdk`
