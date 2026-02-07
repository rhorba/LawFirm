import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/layout/layout.component').then((m) => m.LayoutComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./features/users/user-list/user-list.component').then((m) => m.UserListComponent),
      },
      {
        path: 'groups',
        loadComponent: () =>
          import('./features/groups/group-list/group-list.component').then(
            (m) => m.GroupListComponent
          ),
      },
      {
        path: 'groups/create',
        loadComponent: () =>
          import('./features/groups/group-form/group-form.component').then(
            (m) => m.GroupFormComponent
          ),
      },
      {
        path: 'groups/edit/:id',
        loadComponent: () =>
          import('./features/groups/group-form/group-form.component').then(
            (m) => m.GroupFormComponent
          ),
      },
      {
        path: 'groups/:id/users',
        loadComponent: () =>
          import('./features/groups/group-users/group-users.component').then(
            (m) => m.GroupUsersComponent
          ),
      },
      {
        path: 'audit-logs',
        loadComponent: () =>
          import('./features/audit-logs/audit-log-list/audit-log-list.component').then(
            (m) => m.AuditLogListComponent
          ),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.component').then((m) => m.ProfileComponent),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings.component').then((m) => m.SettingsComponent),
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      },
    ],
  },
  {
    path: '**',
    redirectTo: '/dashboard',
  },
];
