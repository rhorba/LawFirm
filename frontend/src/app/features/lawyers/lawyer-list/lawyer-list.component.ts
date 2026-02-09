import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { LawyerService } from '../../../services/lawyer.service';
import { AuthService } from '../../../core/services/auth.service';
import { LawyerResponse, PageResponse } from '../../../core/models/lawyer.model';
import { LawyerFormComponent } from '../lawyer-form/lawyer-form.component';

@Component({
  selector: 'app-lawyer-list',
  standalone: true,
  imports: [CommonModule, FormsModule, LawyerFormComponent],
  templateUrl: './lawyer-list.component.html',
})
export class LawyerListComponent implements OnInit, OnDestroy {
  private lawyerService = inject(LawyerService);
  private authService = inject(AuthService);

  // State
  lawyers = signal<PageResponse<LawyerResponse> | null>(null);
  loading = signal(false);
  initialLoad = signal(true);
  error = signal<string | null>(null);

  // Pagination
  page = signal(0);
  size = signal(20);

  // Search
  searchTerm = signal('');
  private searchSubject = new Subject<string>();

  // Selection
  selectedIds = signal<Set<number>>(new Set());

  // Modal
  showForm = signal(false);
  editingLawyer = signal<LawyerResponse | null>(null);

  // Permissions
  canCreate = this.authService.hasPermission('LAWYER_CREATE');
  canUpdate = this.authService.hasPermission('LAWYER_UPDATE');
  canDelete = this.authService.hasPermission('LAWYER_DELETE');

  // Expose Math for template
  Math = Math;

  ngOnInit(): void {
    // Setup debounced search
    this.searchSubject
      .pipe(debounceTime(300))
      .subscribe((term) => {
        this.searchTerm.set(term);
        this.page.set(0);
        this.loadLawyers();
      });

    this.loadLawyers();
  }

  ngOnDestroy(): void {
    this.searchSubject.complete();
  }

  loadLawyers(): void {
    this.loading.set(true);
    this.error.set(null);

    const params = {
      page: this.page(),
      size: this.size(),
      search: this.searchTerm() || undefined,
    };

    this.lawyerService.searchLawyers(params).subscribe({
      next: (response: PageResponse<LawyerResponse>) => {
        this.lawyers.set(response);
        this.loading.set(false);
        this.initialLoad.set(false);
      },
      error: (err: unknown) => {
        this.error.set(this.extractErrorMessage(err, 'Failed to load lawyers'));
        this.loading.set(false);
        this.initialLoad.set(false);
      },
    });
  }

  onSearchChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  onPageChange(newPage: number): void {
    this.page.set(newPage);
    this.loadLawyers();
  }

  onPageSizeChange(event: Event): void {
    const newSize = parseInt((event.target as HTMLSelectElement).value, 10);
    this.size.set(newSize);
    this.page.set(0);
    this.loadLawyers();
  }

  openCreateForm(): void {
    this.editingLawyer.set(null);
    this.showForm.set(true);
  }

  openEditForm(lawyer: LawyerResponse): void {
    this.editingLawyer.set(lawyer);
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editingLawyer.set(null);
  }

  onLawyerSaved(): void {
    this.closeForm();
    this.loadLawyers();
  }

  toggleSelection(id: number): void {
    const current = new Set(this.selectedIds());
    if (current.has(id)) {
      current.delete(id);
    } else {
      current.add(id);
    }
    this.selectedIds.set(current);
  }

  toggleSelectAll(): void {
    const current = this.selectedIds();
    const allIds = this.lawyers()?.content.map((l: LawyerResponse) => l.id) || [];

    if (current.size === allIds.length) {
      this.selectedIds.set(new Set());
    } else {
      this.selectedIds.set(new Set(allIds));
    }
  }

  isSelected(id: number): boolean {
    return this.selectedIds().has(id);
  }

  get allSelected(): boolean {
    const lawyers = this.lawyers();
    if (!lawyers || !lawyers.content || lawyers.content.length === 0) return false;
    return this.selectedIds().size === lawyers.content.length;
  }

  deactivateLawyer(id: number): void {
    if (!confirm('Are you sure you want to deactivate this lawyer?')) {
      return;
    }

    this.lawyerService.deactivateLawyer(id).subscribe({
      next: () => {
        this.loadLawyers();
      },
      error: (err: unknown) => {
        alert(this.extractErrorMessage(err, 'Failed to deactivate lawyer'));
      },
    });
  }

  bulkDeactivate(): void {
    const count = this.selectedIds().size;
    if (count === 0) return;

    if (!confirm(`Are you sure you want to deactivate ${count} lawyer(s)?`)) {
      return;
    }

    const deleteRequests = Array.from(this.selectedIds()).map((id) =>
      this.lawyerService.deactivateLawyer(id)
    );

    // Execute all delete requests
    Promise.all(deleteRequests.map((req) => req.toPromise()))
      .then(() => {
        this.selectedIds.set(new Set());
        this.loadLawyers();
      })
      .catch((err: unknown) => {
        alert(this.extractErrorMessage(err, 'Failed to deactivate some lawyers'));
      });
  }

  private extractErrorMessage(err: unknown, fallback: string): string {
    const httpErr = err as {
      error?: { message?: string; validationErrors?: Record<string, string> };
    };
    const body = httpErr.error;
    if (body?.validationErrors) {
      return Object.values(body.validationErrors).join('. ');
    }
    return body?.message || fallback;
  }
}
