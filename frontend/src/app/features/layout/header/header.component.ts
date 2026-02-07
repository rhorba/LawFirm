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
  templateUrl: './header.component.html',
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
    settings: 'Settings',
  };

  constructor() {
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
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
    this.dropdownOpen.update((v) => !v);
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
    const segments = url.split('/').filter((s) => s);
    const crumbs: Breadcrumb[] = [
      { label: 'Home', url: '/dashboard', isLast: segments.length === 0 },
    ];

    let currentUrl = '';
    segments.forEach((segment, index) => {
      currentUrl += `/${segment}`;
      crumbs.push({
        label: this.labelMap[segment] || segment.charAt(0).toUpperCase() + segment.slice(1),
        url: currentUrl,
        isLast: index === segments.length - 1,
      });
    });

    this.breadcrumbs.set(crumbs);
  }
}
