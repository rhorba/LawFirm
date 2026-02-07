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
