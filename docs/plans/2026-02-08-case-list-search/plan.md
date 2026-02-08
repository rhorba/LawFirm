# Case List & Advanced Search - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: use executing-plans skill to implement this plan task-by-task.

**Goal:** Build a comprehensive case list component with advanced filtering, pagination, bulk operations, and permission-based UI for the Angular 18 frontend.

**Architecture:** Frontend-only implementation consuming existing backend APIs. Uses global reference data caching (loaded on app startup) + Angular Signals for reactive state + debounced search. Follows existing UserListComponent patterns exactly.

**Tech Stack:** Angular 18 (Standalone), RxJS, Tailwind CSS, TypeScript strict mode.

---

## BATCH 1: Models & Type Definitions

### Task 1.1: Create Case Models

**Files:**
- Create: `frontend/src/app/core/models/case.model.ts`

**Implementation:**

```typescript
export interface CaseResponse {
  id: number;
  version: number;
  createdAt: string;
  updatedAt: string;
  year: number;
  sequenceNumber: number;
  fullCaseNumber: string;
  registrationDate: string;
  caseDescription: string;
  matterDescription?: string;
  tribunal: TribunalResponse;
  caseType: CaseTypeResponse;
  caseCategory?: CaseCategoryResponse;
  lawyer: LawyerResponse;
  status: CaseStatusResponse;
  financialSummary: FinancialSummary;
}

export interface CaseSummary {
  id: number;
  fullCaseNumber: string;
  caseDescription: string;
  tribunalNameFr: string;
  caseTypeNameFr: string;
  lawyerName: string;
  statusNameFr: string;
  registrationDate: string;
}

export interface TribunalResponse {
  id: number;
  code: string;
  nameFr: string;
  nameAr: string;
  active: boolean;
}

export interface CaseTypeResponse {
  id: number;
  code: string;
  nameFr: string;
  nameAr: string;
  numberFormatTemplate: string;
  active: boolean;
  allowedStatuses: CaseStatusResponse[];
}

export interface CaseCategoryResponse {
  id: number;
  code: string;
  nameFr: string;
  nameAr: string;
  caseTypeCode: string;
}

export interface CaseStatusResponse {
  id: number;
  code: string;
  nameFr: string;
  nameAr: string;
  sortOrder: number;
  isTerminal: boolean;
}

export interface FinancialSummary {
  totalPayments: number;
  totalExpenses: number;
  balance: number;
  transactionCount: number;
}

export interface CaseSearchParams {
  year?: number;
  caseTypeCode?: string;
  categoryCode?: string;
  tribunalCode?: string;
  lawyerId?: number;
  statusCode?: string;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
```

**Verification:**
```bash
cd frontend
pnpm run build
```

Expected: TypeScript compiles without errors.

---

### Task 1.2: Create Lawyer Models

**Files:**
- Create: `frontend/src/app/core/models/lawyer.model.ts`

**Implementation:**

```typescript
export interface LawyerResponse {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  taxId?: string;
  email?: string;
  phone?: string;
  active: boolean;
}

export interface CreateLawyerRequest {
  firstName: string;
  lastName: string;
  taxId?: string;
  email?: string;
  phone?: string;
}

export interface UpdateLawyerRequest {
  firstName?: string;
  lastName?: string;
  taxId?: string;
  email?: string;
  phone?: string;
}
```

**Verification:**
```bash
cd frontend
pnpm run build
```

Expected: TypeScript compiles without errors.

---

## BATCH 2: API Services

### Task 2.1: Create Tribunal Service

**Files:**
- Create: `frontend/src/app/services/tribunal.service.ts`

**Implementation:**

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { TribunalResponse } from '../core/models/case.model';

@Injectable({ providedIn: 'root' })
export class TribunalService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/tribunals`;

  getAll(): Observable<TribunalResponse[]> {
    return this.http.get<TribunalResponse[]>(this.apiUrl);
  }

  getByCode(code: string): Observable<TribunalResponse> {
    return this.http.get<TribunalResponse>(`${this.apiUrl}/${code}`);
  }
}
```

**Verification:**
```bash
cd frontend
pnpm run build
```

Expected: Service compiles without errors.

---

### Task 2.2: Create Case Type Service

**Files:**
- Create: `frontend/src/app/services/case-type.service.ts`

**Implementation:**

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CaseTypeResponse } from '../core/models/case.model';

@Injectable({ providedIn: 'root' })
export class CaseTypeService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/case-types`;

  getAll(): Observable<CaseTypeResponse[]> {
    return this.http.get<CaseTypeResponse[]>(this.apiUrl);
  }

  getByCode(code: string): Observable<CaseTypeResponse> {
    return this.http.get<CaseTypeResponse>(`${this.apiUrl}/${code}`);
  }
}
```

**Verification:**
```bash
cd frontend
pnpm run build
```

Expected: Service compiles without errors.

---

### Task 2.3: Create Case Status Service

**Files:**
- Create: `frontend/src/app/services/case-status.service.ts`

**Implementation:**

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CaseStatusResponse } from '../core/models/case.model';

@Injectable({ providedIn: 'root' })
export class CaseStatusService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/case-statuses`;

  getAll(): Observable<CaseStatusResponse[]> {
    return this.http.get<CaseStatusResponse[]>(this.apiUrl);
  }

  getByCode(code: string): Observable<CaseStatusResponse> {
    return this.http.get<CaseStatusResponse>(`${this.apiUrl}/${code}`);
  }
}
```

**Verification:**
```bash
cd frontend
pnpm run build
```

Expected: Service compiles without errors.

---

### Task 2.4: Create Case Category Service

**Files:**
- Create: `frontend/src/app/services/case-category.service.ts`

**Implementation:**

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CaseCategoryResponse } from '../core/models/case.model';

@Injectable({ providedIn: 'root' })
export class CaseCategoryService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/case-categories`;

  getAll(): Observable<CaseCategoryResponse[]> {
    return this.http.get<CaseCategoryResponse[]>(this.apiUrl);
  }

  getByCaseType(caseTypeCode: string): Observable<CaseCategoryResponse[]> {
    return this.http.get<CaseCategoryResponse[]>(`${this.apiUrl}?caseTypeCode=${caseTypeCode}`);
  }
}
```

**Verification:**
```bash
cd frontend
pnpm run build
```

Expected: Service compiles without errors.

---

### Task 2.5: Create Lawyer Service

**Files:**
- Create: `frontend/src/app/services/lawyer.service.ts`

**Implementation:**

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { LawyerResponse, CreateLawyerRequest, UpdateLawyerRequest } from '../core/models/lawyer.model';

@Injectable({ providedIn: 'root' })
export class LawyerService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/lawyers`;

  getAll(): Observable<LawyerResponse[]> {
    return this.http.get<LawyerResponse[]>(this.apiUrl);
  }

  getById(id: number): Observable<LawyerResponse> {
    return this.http.get<LawyerResponse>(`${this.apiUrl}/${id}`);
  }

  create(request: CreateLawyerRequest): Observable<LawyerResponse> {
    return this.http.post<LawyerResponse>(this.apiUrl, request);
  }

  update(id: number, request: UpdateLawyerRequest): Observable<LawyerResponse> {
    return this.http.put<LawyerResponse>(`${this.apiUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getCaseCount(id: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/${id}/cases/count`);
  }
}
```

**Verification:**
```bash
cd frontend
pnpm run build
```

Expected: Service compiles without errors.

---

### Task 2.6: Create Case Service

**Files:**
- Create: `frontend/src/app/services/case.service.ts`

**Implementation:**

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CaseResponse, CaseSummary, CaseSearchParams, PageResponse } from '../core/models/case.model';

@Injectable({ providedIn: 'root' })
export class CaseService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/cases`;

  searchCases(params: CaseSearchParams): Observable<PageResponse<CaseSummary>> {
    let httpParams = new HttpParams()
      .set('page', (params.page ?? 0).toString())
      .set('size', (params.size ?? 20).toString())
      .set('sortBy', params.sortBy ?? 'registrationDate')
      .set('sortDirection', params.sortDirection ?? 'DESC');

    if (params.year !== undefined && params.year !== null) {
      httpParams = httpParams.set('year', params.year.toString());
    }
    if (params.caseTypeCode) {
      httpParams = httpParams.set('caseTypeCode', params.caseTypeCode);
    }
    if (params.categoryCode) {
      httpParams = httpParams.set('categoryCode', params.categoryCode);
    }
    if (params.tribunalCode) {
      httpParams = httpParams.set('tribunalCode', params.tribunalCode);
    }
    if (params.lawyerId !== undefined && params.lawyerId !== null) {
      httpParams = httpParams.set('lawyerId', params.lawyerId.toString());
    }
    if (params.statusCode) {
      httpParams = httpParams.set('statusCode', params.statusCode);
    }
    if (params.dateFrom) {
      httpParams = httpParams.set('registrationDateFrom', params.dateFrom);
    }
    if (params.dateTo) {
      httpParams = httpParams.set('registrationDateTo', params.dateTo);
    }

    return this.http.get<PageResponse<CaseSummary>>(this.apiUrl, { params: httpParams });
  }

  getCaseById(id: number): Observable<CaseResponse> {
    return this.http.get<CaseResponse>(`${this.apiUrl}/${id}`);
  }

  deleteCase(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
```

**Verification:**
```bash
cd frontend
pnpm run build
```

Expected: Service compiles without errors.

---

### Task 2.7: Create Reference Data Service

**Files:**
- Create: `frontend/src/app/services/reference-data.service.ts`

**Implementation:**

```typescript
import { Injectable, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { TribunalService } from './tribunal.service';
import { CaseTypeService } from './case-type.service';
import { CaseStatusService } from './case-status.service';
import { CaseCategoryService } from './case-category.service';
import { LawyerService } from './lawyer.service';
import {
  TribunalResponse,
  CaseTypeResponse,
  CaseStatusResponse,
  CaseCategoryResponse,
} from '../core/models/case.model';
import { LawyerResponse } from '../core/models/lawyer.model';

@Injectable({ providedIn: 'root' })
export class ReferenceDataService {
  private tribunalService = inject(TribunalService);
  private caseTypeService = inject(CaseTypeService);
  private caseStatusService = inject(CaseStatusService);
  private caseCategoryService = inject(CaseCategoryService);
  private lawyerService = inject(LawyerService);

  // Signals for reactive access
  tribunals = signal<TribunalResponse[]>([]);
  caseTypes = signal<CaseTypeResponse[]>([]);
  categories = signal<CaseCategoryResponse[]>([]);
  statuses = signal<CaseStatusResponse[]>([]);
  lawyers = signal<LawyerResponse[]>([]);

  loading = signal(false);
  error = signal<string | null>(null);

  loadAll(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.loading.set(true);

      forkJoin({
        tribunals: this.tribunalService.getAll(),
        caseTypes: this.caseTypeService.getAll(),
        categories: this.caseCategoryService.getAll(),
        statuses: this.caseStatusService.getAll(),
        lawyers: this.lawyerService.getAll(),
      }).subscribe({
        next: (data) => {
          this.tribunals.set(data.tribunals);
          this.caseTypes.set(data.caseTypes);
          this.categories.set(data.categories);
          this.statuses.set(data.statuses);
          this.lawyers.set(data.lawyers);
          this.loading.set(false);
          resolve();
        },
        error: (err) => {
          console.error('Failed to load reference data:', err);
          this.error.set('Failed to load reference data');
          this.loading.set(false);
          reject(err);
        },
      });
    });
  }
}
```

**Verification:**
```bash
cd frontend
pnpm run build
```

Expected: Service compiles without errors.

---

## BATCH 3: App Configuration

### Task 3.1: Configure Reference Data to Load on Startup

**Files:**
- Modify: `frontend/src/app/app.config.ts`

**Implementation:**

Add APP_INITIALIZER to load reference data on app startup:

```typescript
import { ApplicationConfig, provideZoneChangeDetection, APP_INITIALIZER } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { ReferenceDataService } from './services/reference-data.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    {
      provide: APP_INITIALIZER,
      useFactory: (refData: ReferenceDataService) => () => refData.loadAll(),
      deps: [ReferenceDataService],
      multi: true,
    },
  ],
};
```

**Verification:**
```bash
cd frontend
pnpm run build
pnpm dev
```

Expected: App starts, reference data loads (check Network tab for API calls to /tribunals, /case-types, etc.).

---

## BATCH 4: Case List Component (TypeScript)

### Task 4.1: Create Case List Component TypeScript

**Files:**
- Create: `frontend/src/app/features/cases/case-list/case-list.component.ts`

**Implementation:**

```typescript
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
```

**Verification:**
```bash
cd frontend
pnpm run build
```

Expected: Component TypeScript compiles without errors.

---

## BATCH 5: Case List Component (Template)

### Task 5.1: Create Case List Component HTML Template

**Files:**
- Create: `frontend/src/app/features/cases/case-list/case-list.component.html`

**Implementation:**

```html
<div class="container mx-auto px-4 py-8">
  <!-- Page Header -->
  <div class="flex justify-between items-center mb-6">
    <h1 class="text-2xl font-bold dark:text-white">Cases</h1>
    @if (authService.hasPermission('CASE_CREATE')) {
      <button
        (click)="createCase()"
        class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded focus:outline-none">
        + New Case
      </button>
    }
  </div>

  <!-- Error Message -->
  @if (error()) {
    <div class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded p-4 mb-6">
      <div class="flex justify-between items-start">
        <div class="flex items-start">
          <span class="text-red-600 dark:text-red-400 mr-2">⚠</span>
          <p class="text-red-800 dark:text-red-300">{{ error() }}</p>
        </div>
        <button
          (click)="error.set(null)"
          class="text-red-600 dark:text-red-400 hover:text-red-800">
          ✕
        </button>
      </div>
    </div>
  }

  <!-- Filter Panel -->
  <div class="bg-white dark:bg-gray-800 shadow rounded-lg p-6 mb-6">
    <div class="flex justify-between items-center mb-4">
      <h3 class="text-lg font-medium dark:text-white">Filters</h3>
      <button
        (click)="toggleFilters()"
        class="text-sm text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200">
        {{ filtersExpanded() ? 'Collapse' : 'Expand' }}
      </button>
    </div>

    @if (filtersExpanded()) {
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <!-- Year Dropdown -->
        <div>
          <label class="block text-sm font-medium mb-1 dark:text-gray-300">Year</label>
          <select
            [value]="year() ?? ''"
            (change)="onYearChange($event)"
            class="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="">All Years</option>
            @for (y of yearOptions(); track y) {
              <option [value]="y">{{ y }}</option>
            }
          </select>
        </div>

        <!-- Case Type Dropdown -->
        <div>
          <label class="block text-sm font-medium mb-1 dark:text-gray-300">Case Type</label>
          <select
            [value]="caseTypeCode()"
            (change)="onCaseTypeChange($event)"
            class="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="">All Types</option>
            @for (type of refData.caseTypes(); track type.id) {
              <option [value]="type.code">{{ type.nameFr }}</option>
            }
          </select>
        </div>

        <!-- Category Dropdown (filtered by case type) -->
        <div>
          <label class="block text-sm font-medium mb-1 dark:text-gray-300">Category</label>
          <select
            [value]="categoryCode()"
            (change)="onCategoryChange($event)"
            [disabled]="!caseTypeCode()"
            class="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed">
            <option value="">All Categories</option>
            @for (cat of filteredCategories(); track cat.id) {
              <option [value]="cat.code">{{ cat.nameFr }}</option>
            }
          </select>
        </div>

        <!-- Tribunal Dropdown -->
        <div>
          <label class="block text-sm font-medium mb-1 dark:text-gray-300">Tribunal</label>
          <select
            [value]="tribunalCode()"
            (change)="onTribunalChange($event)"
            class="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="">All Tribunals</option>
            @for (tribunal of refData.tribunals(); track tribunal.id) {
              <option [value]="tribunal.code">{{ tribunal.nameFr }}</option>
            }
          </select>
        </div>

        <!-- Lawyer Dropdown -->
        <div>
          <label class="block text-sm font-medium mb-1 dark:text-gray-300">Lawyer</label>
          <select
            [value]="lawyerId() ?? ''"
            (change)="onLawyerChange($event)"
            class="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="">All Lawyers</option>
            @for (lawyer of refData.lawyers(); track lawyer.id) {
              <option [value]="lawyer.id">{{ lawyer.fullName }}</option>
            }
          </select>
        </div>

        <!-- Status Dropdown -->
        <div>
          <label class="block text-sm font-medium mb-1 dark:text-gray-300">Status</label>
          <select
            [value]="statusCode()"
            (change)="onStatusChange($event)"
            class="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="">All Statuses</option>
            @for (status of refData.statuses(); track status.id) {
              <option [value]="status.code">{{ status.nameFr }}</option>
            }
          </select>
        </div>

        <!-- Date From -->
        <div>
          <label class="block text-sm font-medium mb-1 dark:text-gray-300">Date From</label>
          <input
            type="date"
            [value]="dateFrom() || ''"
            (change)="onDateFromChange($event)"
            class="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <!-- Date To -->
        <div>
          <label class="block text-sm font-medium mb-1 dark:text-gray-300">Date To</label>
          <input
            type="date"
            [value]="dateTo() || ''"
            (change)="onDateToChange($event)"
            class="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          @if (dateRangeError()) {
            <p class="text-red-500 text-xs mt-1">{{ dateRangeError() }}</p>
          }
        </div>
      </div>

      <!-- Search Bar -->
      <div class="mt-4">
        <label class="block text-sm font-medium mb-1 dark:text-gray-300">Search</label>
        <input
          type="text"
          placeholder="Search by case number or description..."
          [value]="searchTerm()"
          (input)="onSearchChange($event)"
          class="w-full border border-gray-300 dark:border-gray-600 rounded px-3 py-2 dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <!-- Actions -->
      <div class="flex justify-between items-center mt-4">
        <button
          (click)="resetFilters()"
          class="text-sm text-blue-600 dark:text-blue-400 hover:underline">
          Reset All Filters
        </button>
        @if (activeFilterCount() > 0) {
          <span class="text-sm text-gray-600 dark:text-gray-400">
            {{ activeFilterCount() }} active filters
          </span>
        }
      </div>
    }
  </div>

  <!-- Bulk Actions Bar -->
  @if (selectedIds().size > 0) {
    <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded p-4 mb-4">
      <div class="flex justify-between items-center">
        <span class="text-blue-800 dark:text-blue-300">
          {{ selectedIds().size }} case(s) selected
        </span>
        @if (authService.hasPermission('CASE_DELETE')) {
          <button
            (click)="deleteSelected()"
            class="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded focus:outline-none">
            Delete Selected
          </button>
        }
      </div>
    </div>
  }

  <!-- Loading State (Initial Load) -->
  @if (initialLoad() && loading()) {
    <div class="bg-white dark:bg-gray-800 shadow rounded-lg p-6">
      <div class="animate-pulse space-y-4">
        <div class="h-10 bg-gray-200 dark:bg-gray-700 rounded"></div>
        <div class="h-10 bg-gray-200 dark:bg-gray-700 rounded"></div>
        <div class="h-10 bg-gray-200 dark:bg-gray-700 rounded"></div>
        <div class="h-10 bg-gray-200 dark:bg-gray-700 rounded"></div>
      </div>
    </div>
  }

  <!-- Cases Table -->
  @if (!initialLoad() || !loading()) {
    <div class="bg-white dark:bg-gray-800 shadow rounded-lg overflow-hidden relative">
      <!-- Loading Overlay -->
      @if (loading() && !initialLoad()) {
        <div class="absolute inset-0 flex items-center justify-center bg-white/50 dark:bg-gray-800/50 z-10">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>
      }

      <div [class.opacity-50]="loading()" [class.pointer-events-none]="loading()">
        @if (cases() && cases()!.content.length > 0) {
          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
              <thead class="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th class="px-6 py-3 text-left">
                    <input
                      type="checkbox"
                      [checked]="allOnPageSelected()"
                      (change)="toggleAllOnPage()"
                      class="rounded border-gray-300 dark:border-gray-600"
                    />
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Case Number
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Description
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Lawyer
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Tribunal
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Type
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Status
                  </th>
                  <th class="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody class="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                @for (caseItem of cases()!.content; track caseItem.id) {
                  <tr class="hover:bg-gray-50 dark:hover:bg-gray-700/50 cursor-pointer" (click)="viewCase(caseItem.id)">
                    <td class="px-6 py-4 whitespace-nowrap" (click)="$event.stopPropagation()">
                      <input
                        type="checkbox"
                        [checked]="isSelected(caseItem.id)"
                        (change)="toggleSelection(caseItem.id, $event)"
                        class="rounded border-gray-300 dark:border-gray-600"
                      />
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-white">
                      {{ caseItem.fullCaseNumber }}
                    </td>
                    <td class="px-6 py-4 text-sm text-gray-700 dark:text-gray-300">
                      {{ truncate(caseItem.caseDescription, 60) }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700 dark:text-gray-300">
                      {{ caseItem.lawyerName }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700 dark:text-gray-300">
                      {{ caseItem.tribunalNameFr }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700 dark:text-gray-300">
                      {{ caseItem.caseTypeNameFr }}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap">
                      <span [class]="getStatusBadgeClass(caseItem.statusNameFr)">
                        {{ caseItem.statusNameFr }}
                      </span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm" (click)="$event.stopPropagation()">
                      <div class="flex items-center justify-end space-x-2">
                        <button
                          (click)="viewCase(caseItem.id)"
                          class="text-blue-600 dark:text-blue-400 hover:text-blue-800"
                          title="View case">
                          👁️
                        </button>
                        @if (authService.hasPermission('CASE_UPDATE')) {
                          <button
                            (click)="editCase(caseItem.id)"
                            class="text-green-600 dark:text-green-400 hover:text-green-800"
                            title="Edit case">
                            ✏️
                          </button>
                        }
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <!-- Pagination -->
          <div class="bg-white dark:bg-gray-800 px-4 py-3 border-t border-gray-200 dark:border-gray-700 sm:px-6">
            <div class="flex items-center justify-between">
              <div class="text-sm text-gray-700 dark:text-gray-300">
                Showing {{ (page() * size()) + 1 }} to
                {{ Math.min((page() + 1) * size(), cases()!.totalElements) }} of
                {{ cases()!.totalElements }} cases
              </div>
              <div class="flex items-center space-x-2">
                <button
                  (click)="previousPage()"
                  [disabled]="page() === 0"
                  class="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50 dark:hover:bg-gray-700">
                  Previous
                </button>
                <span class="text-sm text-gray-700 dark:text-gray-300">
                  Page {{ page() + 1 }} of {{ cases()!.totalPages }}
                </span>
                <button
                  (click)="nextPage()"
                  [disabled]="page() >= cases()!.totalPages - 1"
                  class="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50 dark:hover:bg-gray-700">
                  Next
                </button>
              </div>
            </div>
          </div>
        } @else {
          <!-- Empty States -->
          @if (activeFilterCount() === 0) {
            <!-- No cases at all -->
            <div class="text-center py-12">
              <div class="text-6xl mb-4">📋</div>
              <h3 class="text-xl font-medium mb-2 dark:text-white">No cases yet</h3>
              <p class="text-gray-600 dark:text-gray-400 mb-4">
                Create your first case to get started.
              </p>
              @if (authService.hasPermission('CASE_CREATE')) {
                <button
                  (click)="createCase()"
                  class="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded focus:outline-none">
                  Create First Case
                </button>
              }
            </div>
          } @else {
            <!-- No results from filters -->
            <div class="text-center py-12">
              <div class="text-6xl mb-4">🔍</div>
              <h3 class="text-xl font-medium mb-2 dark:text-white">No cases found</h3>
              <p class="text-gray-600 dark:text-gray-400 mb-4">
                No cases match your current filters.
              </p>
              <button
                (click)="resetFilters()"
                class="bg-gray-600 hover:bg-gray-700 text-white px-6 py-2 rounded focus:outline-none">
                Reset Filters
              </button>
            </div>
          }
        }
      </div>
    </div>
  }
</div>
```

**Verification:**
```bash
cd frontend
pnpm run build
```

Expected: Template compiles without errors. Math reference should work (Math.min).

---

## BATCH 6: Routing & Navigation

### Task 6.1: Add Cases Route

**Files:**
- Modify: `frontend/src/app/app.routes.ts`

**Implementation:**

Add the cases route to the authenticated layout children:

```typescript
{
  path: 'cases',
  loadComponent: () =>
    import('./features/cases/case-list/case-list.component').then(
      (m) => m.CaseListComponent
    ),
},
```

Full authenticated children array should look like:

```typescript
children: [
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'cases',
    loadComponent: () =>
      import('./features/cases/case-list/case-list.component').then(
        (m) => m.CaseListComponent
      ),
  },
  {
    path: 'users',
    loadComponent: () =>
      import('./features/users/user-list/user-list.component').then((m) => m.UserListComponent),
  },
  // ... other routes
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
],
```

**Verification:**
```bash
cd frontend
pnpm run build
pnpm dev
```

Expected: App compiles and runs. Navigate to http://localhost:4200/cases manually in browser.

---

### Task 6.2: Add Cases to Sidebar Navigation

**Files:**
- Modify: `frontend/src/app/features/layout/sidebar/sidebar.component.ts`

**Implementation:**

Add Cases navigation item to the navItems array:

```typescript
navItems: NavItem[] = [
  { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
  { label: 'Cases', icon: 'description', route: '/cases', permission: 'CASE_READ' },
  { label: 'Users', icon: 'people', route: '/users', permission: 'USER_READ' },
  { label: 'Groups', icon: 'groups', route: '/groups', permission: 'ROLE_READ' },
  { label: 'Audit Logs', icon: 'history', route: '/audit-logs' },
  { label: 'Profile', icon: 'person', route: '/profile' },
  { label: 'Settings', icon: 'settings', route: '/settings' },
];
```

**Verification:**
```bash
cd frontend
pnpm dev
```

Expected: "Cases" link appears in sidebar (if user has CASE_READ permission). Clicking navigates to /cases route.

---

## BATCH 7: Final Verification

### Task 7.1: End-to-End Testing

**Manual Testing Checklist:**

1. **Reference Data Loading:**
   - Open browser DevTools Network tab
   - Refresh app
   - Verify API calls to: /tribunals, /case-types, /case-categories, /case-statuses, /lawyers
   - All should return 200 OK with data

2. **Filter Panel:**
   - Click "Cases" in sidebar
   - Verify filter panel is visible
   - Select year → table should reload
   - Select case type → category dropdown should filter
   - Select invalid date range → error message appears
   - Type in search → debounces 300ms → table reloads
   - Click "Reset All Filters" → all filters clear

3. **Table Display:**
   - Verify 7 columns display correctly
   - Verify status badges have correct colors
   - Verify pagination shows correct counts
   - Click Next/Previous → page changes

4. **Bulk Operations:**
   - Check individual checkbox → row selected
   - Check header checkbox → all on page selected
   - Click "Delete Selected" → confirmation prompt → cases deleted

5. **Permissions:**
   - Login as admin → "New Case" button visible
   - Login as USER role → "New Case" button hidden
   - Edit icons only visible if CASE_UPDATE permission

6. **Dark Mode:**
   - Toggle dark mode
   - Verify all components styled correctly in both modes

7. **Empty States:**
   - Clear all cases → "No cases yet" message
   - Add filter with no results → "No cases found" message

**Verification Commands:**
```bash
cd frontend
pnpm run build
pnpm dev
```

Navigate to http://localhost:4200/cases and test all scenarios above.

**Expected Result:** All tests pass, no console errors, UI matches design document.

---

## Implementation Complete!

**Files Created:** 13 files
- 2 model files (case.model.ts, lawyer.model.ts)
- 6 service files (tribunal, case-type, case-status, case-category, lawyer, case)
- 1 reference data service (reference-data.service.ts)
- 2 component files (case-list.component.ts, case-list.component.html)
- 2 modified files (app.config.ts, app.routes.ts, sidebar.component.ts)

**Next Steps:**
- Implement case detail view component
- Implement case form component (create/edit)
- Implement change status modal
- Implement lawyer management components

---

**Plan complete and saved to `docs/plans/2026-02-08-case-list-search/plan.md`.**

**Ready for execution:**

Would you like to execute this plan now using the **executing-plans** skill, or would you prefer to review it first?
