import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../services/user.service';
import { AuthService, UserResponse } from '../../../core/services/auth.service';
import { PageResponse, UserSearchParams } from '../../../core/models/user.model';
import { UserEditPanelComponent } from '../user-edit-panel/user-edit-panel.component';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule, UserEditPanelComponent],
  templateUrl: './user-list.component.html',
})
export class UserListComponent implements OnInit {
  private userService = inject(UserService);
  authService = inject(AuthService);

  users = signal<PageResponse<UserResponse> | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  page = signal(0);
  size = signal(10);

  // Search & filter state
  searchTerm = signal('');
  selectedRole = signal('');
  selectedStatus = signal<string>('');
  showDeleted = signal(false);
  sortField = signal('id');
  sortDirection = signal<'asc' | 'desc'>('asc');

  // Selection state for bulk operations
  selectedIds = signal<Set<number>>(new Set());
  allOnPageSelected = computed(() => {
    const data = this.users();
    if (!data || data.content.length === 0) return false;
    const ids = this.selectedIds();
    return data.content.every((u) => ids.has(u.id));
  });
  selectionCount = computed(() => this.selectedIds().size);

  // Edit panel state
  editingUser = signal<UserResponse | null>(null);
  panelOpen = signal(false);

  Math = Math;

  private searchSubject = new Subject<string>();

  ngOnInit(): void {
    this.loadUsers();

    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe((term) => {
      this.searchTerm.set(term);
      this.page.set(0);
      this.loadUsers();
    });
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  onRoleChange(role: string): void {
    this.selectedRole.set(role);
    this.page.set(0);
    this.loadUsers();
  }

  onStatusChange(status: string): void {
    this.selectedStatus.set(status);
    this.page.set(0);
    this.loadUsers();
  }

  onShowDeletedChange(show: boolean): void {
    this.showDeleted.set(show);
    this.page.set(0);
    this.selectedIds.set(new Set());
    this.loadUsers();
  }

  onSort(field: string): void {
    if (this.sortField() === field) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortField.set(field);
      this.sortDirection.set('asc');
    }
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.error.set(null);

    const params: UserSearchParams = {
      page: this.page(),
      size: this.size(),
      sort: `${this.sortField()},${this.sortDirection()}`,
    };

    if (this.searchTerm()) params.search = this.searchTerm();
    if (this.selectedRole()) params.role = this.selectedRole();
    if (this.selectedStatus() !== '') params.enabled = this.selectedStatus() === 'true';
    if (this.showDeleted()) params.showDeleted = true;

    this.userService.searchUsers(params).subscribe({
      next: (data) => {
        this.users.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load users');
        this.loading.set(false);
        console.error(err);
      },
    });
  }

  nextPage(): void {
    const data = this.users();
    if (data && this.page() < data.totalPages - 1) {
      this.page.update((p) => p + 1);
      this.loadUsers();
    }
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.loadUsers();
    }
  }

  // --- Selection ---

  toggleSelection(id: number): void {
    this.selectedIds.update((ids) => {
      const next = new Set(ids);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  toggleAllOnPage(): void {
    const data = this.users();
    if (!data) return;

    if (this.allOnPageSelected()) {
      this.selectedIds.update((ids) => {
        const next = new Set(ids);
        data.content.forEach((u) => next.delete(u.id));
        return next;
      });
    } else {
      this.selectedIds.update((ids) => {
        const next = new Set(ids);
        data.content.forEach((u) => next.add(u.id));
        return next;
      });
    }
  }

  isSelected(id: number): boolean {
    return this.selectedIds().has(id);
  }

  clearSelection(): void {
    this.selectedIds.set(new Set());
  }

  // --- Single Actions ---

  deleteUser(id: number): void {
    if (!confirm('Are you sure you want to delete this user?')) return;

    this.userService.deleteUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        alert('Failed to delete user');
        console.error(err);
      },
    });
  }

  restoreUser(id: number): void {
    this.userService.restoreUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        alert('Failed to restore user');
        console.error(err);
      },
    });
  }

  purgeUser(user: UserResponse): void {
    const confirmation = prompt(`Type "${user.username}" to permanently delete this user:`);
    if (confirmation !== user.username) return;

    this.userService.purgeUser(user.id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        alert('Failed to purge user');
        console.error(err);
      },
    });
  }

  // --- Bulk Actions ---

  bulkDelete(): void {
    const ids = Array.from(this.selectedIds());
    if (!confirm(`Delete ${ids.length} user(s)?`)) return;

    this.userService.bulkDelete({ userIds: ids }).subscribe({
      next: () => {
        this.clearSelection();
        this.loadUsers();
      },
      error: (err) => {
        alert(err.error?.message || 'Bulk delete failed');
        console.error(err);
      },
    });
  }

  bulkEnable(): void {
    const ids = Array.from(this.selectedIds());
    this.userService.bulkUpdateStatus({ userIds: ids, enabled: true }).subscribe({
      next: () => {
        this.clearSelection();
        this.loadUsers();
      },
      error: (err) => {
        alert('Bulk enable failed');
        console.error(err);
      },
    });
  }

  bulkDisable(): void {
    const ids = Array.from(this.selectedIds());
    this.userService.bulkUpdateStatus({ userIds: ids, enabled: false }).subscribe({
      next: () => {
        this.clearSelection();
        this.loadUsers();
      },
      error: (err) => {
        alert('Bulk disable failed');
        console.error(err);
      },
    });
  }

  // --- Edit Panel ---

  openCreatePanel(): void {
    this.editingUser.set(null);
    this.panelOpen.set(true);
  }

  openEditPanel(user: UserResponse): void {
    this.editingUser.set(user);
    this.panelOpen.set(true);
  }

  closeEditPanel(): void {
    this.editingUser.set(null);
    this.panelOpen.set(false);
  }

  onUserUpdated(): void {
    this.closeEditPanel();
    this.loadUsers();
  }
}
