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
    { label: 'Users', icon: 'people', route: '/users', permission: 'USER_READ' },
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
