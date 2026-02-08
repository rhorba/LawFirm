import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subject, debounceTime, takeUntil } from 'rxjs';
import { forkJoin } from 'rxjs';

import { CaseService } from '../../../services/case.service';
import { ReferenceDataService } from '../../../services/reference-data.service';
import { AuthService } from '../../../core/services/auth.service';
import { CaseSummary, CaseSearchParams, PageResponse } from '../../../core/models/case.model';

@Component({
  selector: 'app-case-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './case-list.component.html',
})
export class CaseListComponent implements OnInit, OnDestroy {
  private caseService = inject(CaseService);
  private router = inject(Router);
  private destroy$ = new Subject<void>();

  refData = inject(ReferenceDataService);
  authService = inject(AuthService);

  // Expose Math for template
  Math = Math;

  // Data
  cases = signal<PageResponse<CaseSummary> | null>(null);
  loading = signal(false);
  initialLoad = signal(true);
  error = signal<string | null>(null);

  // Pagination
  page = signal(0);
  size = signal(20);

  // Filters
  year = signal<number | null>(null);
  caseTypeCode = signal('');
  categoryCode = signal('');
  tribunalCode = signal('');
  lawyerId = signal<number | null>(null);
  statusCode = signal('');
  dateFrom = signal<string | null>(null);
  dateTo = signal<string | null>(null);
  searchTerm = signal('');
  dateRangeError = signal<string | null>(null);

  // UI State
  filtersExpanded = signal(true);

  // Selection (bulk operations)
  selectedIds = signal<Set<number>>(new Set());

  // Computed
  activeFilterCount = computed(() => {
    let count = 0;
    if (this.year()) count++;
    if (this.caseTypeCode()) count++;
    if (this.categoryCode()) count++;
    if (this.tribunalCode()) count++;
    if (this.lawyerId()) count++;
    if (this.statusCode()) count++;
    if (this.dateFrom()) count++;
    if (this.dateTo()) count++;
    if (this.searchTerm()) count++;
    return count;
  });

  filteredCategories = computed(() => {
    const typeCode = this.caseTypeCode();
    if (!typeCode) return this.refData.categories();
    return this.refData.categories().filter((c) => c.caseTypeCode === typeCode);
  });

  yearOptions = computed(() => {
    const currentYear = new Date().getFullYear();
    return Array.from({ length: 6 }, (_, i) => currentYear - i);
  });

  // Debounced search
  private searchSubject = new Subject<string>();

  ngOnInit() {
    this.searchSubject.pipe(debounceTime(300), takeUntil(this.destroy$)).subscribe((term) => {
      this.searchTerm.set(term);
      this.page.set(0);
      this.loadCases();
    });

    this.loadCases();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadCases() {
    this.loading.set(true);
    this.error.set(null);

    const params: CaseSearchParams = {
      page: this.page(),
      size: this.size(),
      year: this.year() ?? undefined,
      caseTypeCode: this.caseTypeCode() || undefined,
      categoryCode: this.categoryCode() || undefined,
      tribunalCode: this.tribunalCode() || undefined,
      lawyerId: this.lawyerId() ?? undefined,
      statusCode: this.statusCode() || undefined,
      dateFrom: this.dateFrom() ?? undefined,
      dateTo: this.dateTo() ?? undefined,
    };

    this.caseService.searchCases(params).subscribe({
      next: (data) => {
        this.cases.set(data);
        this.loading.set(false);
        this.initialLoad.set(false);
      },
      error: (err) => {
        this.error.set(this.extractErrorMessage(err));
        this.loading.set(false);
        this.initialLoad.set(false);
      },
    });
  }

  // Filter handlers
  onYearChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.year.set(value ? parseInt(value) : null);
    this.page.set(0);
    this.loadCases();
  }

  onCaseTypeChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.caseTypeCode.set(value);

    // Auto-clear category if not valid for new type
    if (
      this.categoryCode() &&
      !this.filteredCategories().some((c) => c.code === this.categoryCode())
    ) {
      this.categoryCode.set('');
    }

    this.page.set(0);
    this.loadCases();
  }

  onCategoryChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.categoryCode.set(value);
    this.page.set(0);
    this.loadCases();
  }

  onTribunalChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.tribunalCode.set(value);
    this.page.set(0);
    this.loadCases();
  }

  onLawyerChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.lawyerId.set(value ? parseInt(value) : null);
    this.page.set(0);
    this.loadCases();
  }

  onStatusChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.statusCode.set(value);
    this.page.set(0);
    this.loadCases();
  }

  onDateFromChange(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.dateFrom.set(value || null);
    this.validateDateRange();
    this.page.set(0);
    this.loadCases();
  }

  onDateToChange(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.dateTo.set(value || null);
    this.validateDateRange();
    this.page.set(0);
    this.loadCases();
  }

  onSearchChange(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  private validateDateRange() {
    const from = this.dateFrom();
    const to = this.dateTo();

    if (from && to && from > to) {
      this.dateRangeError.set('End date must be after start date');
    } else {
      this.dateRangeError.set(null);
    }
  }

  resetFilters() {
    this.year.set(null);
    this.caseTypeCode.set('');
    this.categoryCode.set('');
    this.tribunalCode.set('');
    this.lawyerId.set(null);
    this.statusCode.set('');
    this.dateFrom.set(null);
    this.dateTo.set(null);
    this.searchTerm.set('');
    this.dateRangeError.set(null);
    this.page.set(0);
    this.loadCases();
  }

  toggleFilters() {
    this.filtersExpanded.set(!this.filtersExpanded());
  }

  // Pagination
  onPageChange(newPage: number) {
    this.page.set(newPage);
    this.loadCases();
  }

  nextPage() {
    const data = this.cases();
    if (data && this.page() < data.totalPages - 1) {
      this.onPageChange(this.page() + 1);
    }
  }

  previousPage() {
    if (this.page() > 0) {
      this.onPageChange(this.page() - 1);
    }
  }

  // Selection
  toggleSelection(id: number, event: Event) {
    event.stopPropagation();
    const current = new Set(this.selectedIds());
    if (current.has(id)) {
      current.delete(id);
    } else {
      current.add(id);
    }
    this.selectedIds.set(current);
  }

  toggleAllOnPage() {
    const data = this.cases();
    if (!data) return;

    const current = new Set(this.selectedIds());
    const pageIds = data.content.map((c) => c.id);

    if (pageIds.every((id) => current.has(id))) {
      // Deselect all on page
      pageIds.forEach((id) => current.delete(id));
    } else {
      // Select all on page
      pageIds.forEach((id) => current.add(id));
    }

    this.selectedIds.set(current);
  }

  isSelected(id: number): boolean {
    return this.selectedIds().has(id);
  }

  allOnPageSelected(): boolean {
    const data = this.cases();
    if (!data || data.content.length === 0) return false;
    const ids = this.selectedIds();
    return data.content.every((c) => ids.has(c.id));
  }

  // Bulk operations
  deleteSelected() {
    const count = this.selectedIds().size;
    const confirmed = confirm(
      `Are you sure you want to delete ${count} case(s)? This action cannot be undone.`
    );

    if (!confirmed) return;

    this.loading.set(true);

    const deleteObs = Array.from(this.selectedIds()).map((id) =>
      this.caseService.deleteCase(id)
    );

    forkJoin(deleteObs).subscribe({
      next: () => {
        this.selectedIds.set(new Set());
        this.loadCases();
      },
      error: (err) => {
        this.error.set('Failed to delete some cases. Please try again.');
        this.loading.set(false);
      },
    });
  }

  // Navigation
  viewCase(id: number) {
    this.router.navigate(['/cases', id]);
  }

  editCase(id: number) {
    this.router.navigate(['/cases', id, 'edit']);
  }

  createCase() {
    this.router.navigate(['/cases/new']);
  }

  // Status badge colors
  getStatusBadgeClass(statusCode: string): string {
    const baseClasses = 'inline-flex items-center px-2 py-1 text-xs font-medium rounded';

    switch (statusCode) {
      case 'DRAFT':
        return `${baseClasses} bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200`;
      case 'OPEN':
        return `${baseClasses} bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200`;
      case 'IN_PROGRESS':
        return `${baseClasses} bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-200`;
      case 'HEARING':
        return `${baseClasses} bg-purple-100 dark:bg-purple-900 text-purple-800 dark:text-purple-200`;
      case 'JUDGMENT':
        return `${baseClasses} bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200`;
      case 'CLOSED':
        return `${baseClasses} bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200`;
      case 'ARCHIVED':
        return `${baseClasses} bg-gray-300 dark:bg-gray-600 text-gray-700 dark:text-gray-300`;
      default:
        return `${baseClasses} bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200`;
    }
  }

  // Utility
  truncate(text: string, length: number): string {
    if (text.length <= length) return text;
    return text.substring(0, length) + '...';
  }

  private extractErrorMessage(err: unknown): string {
    const httpErr = err as {
      error?: { message?: string; validationErrors?: Record<string, string> };
    };
    const body = httpErr.error;
    if (body?.validationErrors) {
      return Object.values(body.validationErrors).join('. ');
    }
    return body?.message || 'Failed to load cases. Please try again.';
  }
}
