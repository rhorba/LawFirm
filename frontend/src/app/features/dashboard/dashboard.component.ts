import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <h1 class="text-2xl font-bold mb-6 text-gray-900 dark:text-white">Dashboard</h1>

    @if (authService.currentUser(); as user) {
      <div class="bg-white dark:bg-gray-800 shadow-md rounded-lg p-6 mb-6">
        <h2 class="text-xl font-semibold mb-4 text-gray-900 dark:text-white">
          Welcome, {{ user.username }}!
        </h2>
        <p class="text-gray-600 dark:text-gray-400">Email: {{ user.email }}</p>
        <div class="mt-4">
          <h3 class="font-semibold mb-2 text-gray-900 dark:text-white">Your Roles:</h3>
          <div class="flex gap-2">
            @for (role of user.roles; track role.id) {
              <span
                class="inline-flex items-center px-3 py-1 text-sm font-medium bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 rounded-full"
              >
                {{ role.name }}
              </span>
            }
          </div>
        </div>
      </div>
    }

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      @if (authService.hasPermission('USER_READ')) {
        <a
          routerLink="/users"
          class="bg-white dark:bg-gray-800 shadow-md rounded-lg p-6 hover:shadow-lg transition text-gray-900 dark:text-white"
        >
          <h3 class="text-lg font-semibold mb-2">Users</h3>
          <p class="text-gray-600 dark:text-gray-400">Manage user accounts</p>
        </a>
      }

      <a
        routerLink="/settings"
        class="bg-white dark:bg-gray-800 shadow-md rounded-lg p-6 hover:shadow-lg transition text-gray-900 dark:text-white"
      >
        <h3 class="text-lg font-semibold mb-2">Settings</h3>
        <p class="text-gray-600 dark:text-gray-400">Configure application</p>
      </a>
    </div>
  `,
})
export class DashboardComponent {
  authService = inject(AuthService);
}
