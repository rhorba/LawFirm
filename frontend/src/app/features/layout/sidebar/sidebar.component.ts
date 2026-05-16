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
  templateUrl: './sidebar.component.html',
})
export class SidebarComponent {
  themeService = inject(ThemeService);
  authService = inject(AuthService);

  navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'Cases', icon: 'description', route: '/cases', permission: 'CASE_READ' },
    { label: 'Lawyers', icon: 'gavel', route: '/lawyers', permission: 'LAWYER_READ' },
    { label: 'Clients', icon: 'people', route: '/clients', permission: 'CLIENT_READ' },
    {
      label: 'Financial',
      icon: 'account_balance_wallet',
      route: '/financial',
      permission: 'FINANCIAL_READ',
    },
    {
      label: 'Calendrier',
      icon: 'calendar_month',
      route: '/calendar',
      permission: 'CALENDAR_READ',
    },
    {
      label: 'Conflits',
      icon: 'gavel',
      route: '/conflicts',
      permission: 'CONFLICT_READ',
    },
    {
      label: 'Rapports',
      icon: 'bar_chart',
      route: '/reports',
      permission: 'REPORT_READ',
    },
    { label: 'Users', icon: 'manage_accounts', route: '/users', permission: 'USER_READ' },
    { label: 'Groups', icon: 'group', route: '/groups', permission: 'SYSTEM_MANAGE' },
    { label: 'Audit Logs', icon: 'history', route: '/audit-logs', permission: 'SYSTEM_MANAGE' },
    { label: 'Settings', icon: 'settings', route: '/settings' },
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
    this.themeService.closeMobileSidebar();
  }
}
