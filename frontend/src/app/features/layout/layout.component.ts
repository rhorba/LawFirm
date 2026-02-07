import { Component, inject, HostListener } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from '../../core/services/theme.service';
import { SidebarComponent } from './sidebar/sidebar.component';
import { HeaderComponent } from './header/header.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, HeaderComponent],
  templateUrl: './layout.component.html',
})
export class LayoutComponent {
  themeService = inject(ThemeService);

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.themeService.closeMobileSidebar();
  }
}
