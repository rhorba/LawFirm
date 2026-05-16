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
        path: 'cases',
        loadComponent: () =>
          import('./features/cases/case-list/case-list.component').then((m) => m.CaseListComponent),
      },
      {
        path: 'cases/new',
        loadComponent: () =>
          import('./features/cases/case-form/case-form.component').then((m) => m.CaseFormComponent),
      },
      {
        path: 'cases/:id',
        loadComponent: () =>
          import('./features/cases/case-detail/case-detail.component').then(
            (m) => m.CaseDetailComponent
          ),
      },
      {
        path: 'cases/:id/edit',
        loadComponent: () =>
          import('./features/cases/case-form/case-form.component').then((m) => m.CaseFormComponent),
      },
      {
        path: 'lawyers',
        loadComponent: () =>
          import('./features/lawyers/lawyer-list/lawyer-list.component').then(
            (m) => m.LawyerListComponent
          ),
      },
      {
        path: 'clients',
        loadComponent: () =>
          import('./features/clients/client-list/client-list.component').then(
            (m) => m.ClientListComponent
          ),
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
        path: 'financial',
        children: [
          {
            path: '',
            redirectTo: 'ledger',
            pathMatch: 'full',
          },
          {
            path: 'ledger',
            loadComponent: () =>
              import('./features/financial/ledger/financial-ledger.component').then(
                (m) => m.FinancialLedgerComponent
              ),
          },
          {
            path: 'invoices',
            loadComponent: () =>
              import('./features/financial/invoices/invoice-list/invoice-list.component').then(
                (m) => m.InvoiceListComponent
              ),
          },
          {
            path: 'invoices/new',
            loadComponent: () =>
              import('./features/financial/invoices/invoice-form/invoice-form.component').then(
                (m) => m.InvoiceFormComponent
              ),
          },
          {
            path: 'invoices/:id',
            loadComponent: () =>
              import('./features/financial/invoices/invoice-detail/invoice-detail.component').then(
                (m) => m.InvoiceDetailComponent
              ),
          },
        ],
      },
      {
        path: 'calendar',
        loadComponent: () =>
          import('./features/calendar/calendar.component').then((m) => m.CalendarComponent),
      },
      {
        path: 'conflicts',
        loadComponent: () =>
          import('./features/conflicts/conflict-check.component').then(
            (m) => m.ConflictCheckComponent
          ),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('./features/reports/reports.component').then((m) => m.ReportsComponent),
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
