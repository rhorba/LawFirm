# Case List & Advanced Search - Design Document

**Date:** 2026-02-08
**Feature:** Case List Component with Advanced Filtering
**Framework:** Angular 18 (Standalone)
**Status:** Design Approved

---

## 1. Architecture & Data Flow

### Reference Data Service (Global Cache)

**New Service:** `ReferenceDataService` (singleton, loaded on app startup)

**Purpose:** Cache small, rarely-changing reference datasets globally to avoid repeated API calls.

**Cached Data:**
- Tribunals (51 records)
- Case Types (4 records)
- Case Categories (29 records)
- Case Statuses (7 records)
- Lawyers (active only)

**Implementation:**
```typescript
@Injectable({ providedIn: 'root' })
export class ReferenceDataService {
  private tribunalService = inject(TribunalService);
  private caseTypeService = inject(CaseTypeService);
  private caseStatusService = inject(CaseStatusService);
  private lawyerService = inject(LawyerService);

  // Signals for reactive access
  tribunals = signal<TribunalResponse[]>([]);
  caseTypes = signal<CaseTypeResponse[]>([]);
  categories = signal<CaseCategoryResponse[]>([]);
  statuses = signal<CaseStatusResponse[]>([]);
  lawyers = signal<LawyerResponse[]>([]);

  loading = signal(false);
  error = signal<string | null>(null);

  loadAll(): void {
    this.loading.set(true);
    forkJoin({
      tribunals: this.tribunalService.getAll(),
      caseTypes: this.caseTypeService.getAll(),
      categories: this.caseCategoryService.getAll(),
      statuses: this.caseStatusService.getAll(),
      lawyers: this.lawyerService.getAll()
    }).subscribe({
      next: (data) => {
        this.tribunals.set(data.tribunals);
        this.caseTypes.set(data.caseTypes);
        this.categories.set(data.categories);
        this.statuses.set(data.statuses);
        this.lawyers.set(data.lawyers);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load reference data');
        this.loading.set(false);
      }
    });
  }
}
```

**Initialization:**
Add to `app.config.ts`:
```typescript
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    {
      provide: APP_INITIALIZER,
      useFactory: (refData: ReferenceDataService) => () => refData.loadAll(),
      deps: [ReferenceDataService],
      multi: true
    }
  ],
};
```

---

### Case Service (Standard CRUD)

**Service:** `CaseService` (follows UserService pattern)

**Key Method:**
```typescript
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

    if (params.year) httpParams = httpParams.set('year', params.year.toString());
    if (params.caseTypeCode) httpParams = httpParams.set('caseTypeCode', params.caseTypeCode);
    if (params.tribunalCode) httpParams = httpParams.set('tribunalCode', params.tribunalCode);
    if (params.lawyerId) httpParams = httpParams.set('lawyerId', params.lawyerId.toString());
    if (params.statusCode) httpParams = httpParams.set('statusCode', params.statusCode);
    if (params.categoryCode) httpParams = httpParams.set('categoryCode', params.categoryCode);
    if (params.dateFrom) httpParams = httpParams.set('registrationDateFrom', params.dateFrom);
    if (params.dateTo) httpParams = httpParams.set('registrationDateTo', params.dateTo);

    return this.http.get<PageResponse<CaseSummary>>(this.apiUrl, { params: httpParams });
  }

  deleteCase(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
```

---

### Component State (CaseListComponent)

**State Management:** Angular Signals + RxJS Observables

**Signals:**
```typescript
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
```

**Debounced Search:**
```typescript
private searchSubject = new Subject<string>();

ngOnInit() {
  this.searchSubject
    .pipe(debounceTime(300))
    .subscribe((term) => {
      this.searchTerm.set(term);
      this.page.set(0);
      this.loadCases();
    });

  this.loadCases();
}

onSearchChange(event: Event) {
  const value = (event.target as HTMLInputElement).value;
  this.searchSubject.next(value);
}
```

---

## 2. Components & UI Structure

### CaseListComponent Layout

**File:** `features/cases/case-list/case-list.component.html`

**Structure:**
```
┌─────────────────────────────────────────────┐
│ Page Header                                 │
│ "Cases" + New Case Button (if permitted)   │
└─────────────────────────────────────────────┘
┌─────────────────────────────────────────────┐
│ Filter Panel (Collapsible)                  │
│ ┌─────────┬─────────┬─────────┬─────────┐  │
│ │ Year    │ Type    │ Tribunal│ Lawyer  │  │
│ └─────────┴─────────┴─────────┴─────────┘  │
│ ┌─────────┬─────────┬─────────┬─────────┐  │
│ │ Status  │Category │ From    │ To      │  │
│ └─────────┴─────────┴─────────┴─────────┘  │
│ ┌─────────────────────────────────────┐    │
│ │ Search (case number/description)    │    │
│ └─────────────────────────────────────┘    │
│ [Reset Filters] [Active: 3 filters]        │
└─────────────────────────────────────────────┘
┌─────────────────────────────────────────────┐
│ Bulk Actions Bar (when items selected)     │
│ "3 selected" [Delete Selected]             │
└─────────────────────────────────────────────┘
┌─────────────────────────────────────────────┐
│ Cases Table                                 │
│ ┌──┬─────────┬─────────┬────┬────┬───┬───┐│
│ │☐ │Number   │Desc     │Law │Trib│Typ│Sta││
│ ├──┼─────────┼─────────┼────┼────┼───┼───┤│
│ │☐ │PENAL/...│Case desc│Ben │TA  │PEN│OPN││
│ └──┴─────────┴─────────┴────┴────┴───┴───┘│
└─────────────────────────────────────────────┘
┌─────────────────────────────────────────────┐
│ Pagination                                  │
│ "Showing 1-20 of 156" [< 1 2 3 ... >]     │
└─────────────────────────────────────────────┘
```

---

### Filter Panel Details

**Grid Layout:**
```html
<div class="bg-white dark:bg-gray-800 shadow rounded-lg p-6 mb-6">
  <div class="flex justify-between items-center mb-4">
    <h3 class="text-lg font-medium dark:text-white">Filters</h3>
    <button
      (click)="toggleFilters()"
      class="text-sm text-gray-600 dark:text-gray-400">
      {{ filtersExpanded() ? 'Collapse' : 'Expand' }}
    </button>
  </div>

  @if (filtersExpanded()) {
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- Year Dropdown -->
      <div>
        <label class="block text-sm font-medium mb-1 dark:text-gray-300">Year</label>
        <select
          [value]="year()"
          (change)="onYearChange($event)"
          class="w-full border rounded px-3 py-2 dark:bg-gray-700 dark:border-gray-600">
          <option value="">All Years</option>
          @for (y of yearOptions; track y) {
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
          class="w-full border rounded px-3 py-2 dark:bg-gray-700 dark:border-gray-600">
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
          class="w-full border rounded px-3 py-2 dark:bg-gray-700 dark:border-gray-600">
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
          class="w-full border rounded px-3 py-2 dark:bg-gray-700 dark:border-gray-600">
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
          [value]="lawyerId()"
          (change)="onLawyerChange($event)"
          class="w-full border rounded px-3 py-2 dark:bg-gray-700 dark:border-gray-600">
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
          class="w-full border rounded px-3 py-2 dark:bg-gray-700 dark:border-gray-600">
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
          class="w-full border rounded px-3 py-2 dark:bg-gray-700 dark:border-gray-600"
        />
      </div>

      <!-- Date To -->
      <div>
        <label class="block text-sm font-medium mb-1 dark:text-gray-300">Date To</label>
        <input
          type="date"
          [value]="dateTo() || ''"
          (change)="onDateToChange($event)"
          class="w-full border rounded px-3 py-2 dark:bg-gray-700 dark:border-gray-600"
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
        (input)="onSearchChange($event)"
        class="w-full border rounded px-3 py-2 dark:bg-gray-700 dark:border-gray-600"
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
```

---

### Table Columns

**Columns (7 total):**
1. **Checkbox** - Bulk selection
2. **Case Number** - `fullCaseNumber` (e.g., "PENAL/TA/2026/00001")
3. **Description** - `caseDescription` (truncated to 60 chars)
4. **Lawyer** - `lawyerName` from CaseSummary
5. **Tribunal** - `tribunalNameFr` from CaseSummary
6. **Case Type** - `caseTypeNameFr` from CaseSummary
7. **Status** - `statusNameFr` with color-coded badge
8. **Actions** - View/Edit icons (if permitted)

**Status Badge Colors:**
- DRAFT → Gray (`bg-gray-100 dark:bg-gray-700`)
- OPEN → Blue (`bg-blue-100 dark:bg-blue-900`)
- IN_PROGRESS → Yellow (`bg-yellow-100 dark:bg-yellow-900`)
- HEARING → Purple (`bg-purple-100 dark:bg-purple-900`)
- JUDGMENT → Green (`bg-green-100 dark:bg-green-900`)
- CLOSED → Red (`bg-red-100 dark:bg-red-900`)
- ARCHIVED → Gray (`bg-gray-300 dark:bg-gray-600`)

---

## 3. Forms & Validation

### Filter Change Handling

**No Reactive Form Required** - Each filter is a signal that triggers reload.

**Pattern:**
```typescript
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
  if (this.categoryCode() && !this.filteredCategories().some(c => c.code === this.categoryCode())) {
    this.categoryCode.set('');
  }

  this.page.set(0);
  this.loadCases();
}
```

---

### Cascading Dropdowns

**Category filtered by Case Type:**
```typescript
filteredCategories = computed(() => {
  const typeCode = this.caseTypeCode();
  if (!typeCode) return this.refData.categories();
  return this.refData.categories().filter(c => c.caseTypeCode === typeCode);
});
```

---

### Date Range Validation

```typescript
dateRangeError = signal<string | null>(null);

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

private validateDateRange() {
  const from = this.dateFrom();
  const to = this.dateTo();

  if (from && to && from > to) {
    this.dateRangeError.set('End date must be after start date');
  } else {
    this.dateRangeError.set(null);
  }
}
```

---

### Query Parameter Sync (Optional Enhancement)

**Enable bookmarkable filtered views:**
```typescript
private router = inject(Router);
private route = inject(ActivatedRoute);

ngOnInit() {
  // Restore filters from URL on init
  this.route.queryParams.pipe(take(1)).subscribe(params => {
    if (params['year']) this.year.set(parseInt(params['year']));
    if (params['caseTypeCode']) this.caseTypeCode.set(params['caseTypeCode']);
    if (params['statusCode']) this.statusCode.set(params['statusCode']);
    // ... other filters

    this.loadCases();
  });
}

private updateQueryParams() {
  const queryParams: any = {};

  if (this.year()) queryParams.year = this.year();
  if (this.caseTypeCode()) queryParams.caseTypeCode = this.caseTypeCode();
  if (this.statusCode()) queryParams.statusCode = this.statusCode();
  // ... other filters

  this.router.navigate([], {
    relativeTo: this.route,
    queryParams,
    queryParamsHandling: 'merge'
  });
}
```

---

## 4. Error Handling & Loading States

### Loading States

**Initial Load** (first time component loads):
```html
@if (initialLoad()) {
  <!-- Skeleton loader -->
  <div class="animate-pulse">
    <div class="h-10 bg-gray-200 dark:bg-gray-700 rounded mb-2"></div>
    <div class="h-10 bg-gray-200 dark:bg-gray-700 rounded mb-2"></div>
    <div class="h-10 bg-gray-200 dark:bg-gray-700 rounded mb-2"></div>
  </div>
}
```

**Subsequent Loads** (filter changes):
```html
<div [class.opacity-50]="loading()" [class.pointer-events-none]="loading()">
  <!-- Table content -->
</div>

@if (loading() && !initialLoad()) {
  <div class="absolute inset-0 flex items-center justify-center bg-white/50 dark:bg-gray-800/50">
    <div class="spinner"></div>
  </div>
}
```

---

### Error Handling

**Error Display:**
```html
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
```

**Error Extraction (reuse existing pattern):**
```typescript
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
```

---

### Empty States

**No Cases at All:**
```html
@if (cases()?.content.length === 0 && activeFilterCount() === 0) {
  <div class="text-center py-12">
    <div class="text-6xl mb-4">📋</div>
    <h3 class="text-xl font-medium mb-2 dark:text-white">No cases yet</h3>
    <p class="text-gray-600 dark:text-gray-400 mb-4">
      Create your first case to get started.
    </p>
    @if (authService.hasPermission('CASE_CREATE')) {
      <button
        (click)="router.navigate(['/cases/new'])"
        class="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded">
        Create First Case
      </button>
    }
  </div>
}
```

**No Results from Filters:**
```html
@if (cases()?.content.length === 0 && activeFilterCount() > 0) {
  <div class="text-center py-12">
    <div class="text-6xl mb-4">🔍</div>
    <h3 class="text-xl font-medium mb-2 dark:text-white">No cases found</h3>
    <p class="text-gray-600 dark:text-gray-400 mb-4">
      No cases match your current filters.
    </p>
    <button
      (click)="resetFilters()"
      class="bg-gray-600 hover:bg-gray-700 text-white px-6 py-2 rounded">
      Reset Filters
    </button>
  </div>
}
```

---

## 5. Permissions & Actions

### Permission Checks

**Create Button:**
```html
<div class="flex justify-between items-center mb-6">
  <h1 class="text-2xl font-bold dark:text-white">Cases</h1>
  @if (authService.hasPermission('CASE_CREATE')) {
    <button
      (click)="router.navigate(['/cases/new'])"
      class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded">
      + New Case
    </button>
  }
</div>
```

**Actions Column:**
```html
<td class="px-6 py-4 whitespace-nowrap text-right">
  <div class="flex items-center justify-end space-x-2">
    <!-- View (always available) -->
    <button
      (click)="viewCase(case.id)"
      class="text-blue-600 dark:text-blue-400 hover:text-blue-800"
      title="View case">
      <span class="icon">visibility</span>
    </button>

    <!-- Edit (requires permission) -->
    @if (authService.hasPermission('CASE_UPDATE')) {
      <button
        (click)="editCase(case.id)"
        class="text-green-600 dark:text-green-400 hover:text-green-800"
        title="Edit case">
        <span class="icon">edit</span>
      </button>
    }
  </div>
</td>
```

---

### Bulk Operations

**Selection State:**
```typescript
selectedIds = signal<Set<number>>(new Set());

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
  const pageIds = data.content.map(c => c.id);

  if (pageIds.every(id => current.has(id))) {
    // Deselect all on page
    pageIds.forEach(id => current.delete(id));
  } else {
    // Select all on page
    pageIds.forEach(id => current.add(id));
  }

  this.selectedIds.set(current);
}
```

**Bulk Actions Bar:**
```html
@if (selectedIds().size > 0) {
  <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded p-4 mb-4">
    <div class="flex justify-between items-center">
      <span class="text-blue-800 dark:text-blue-300">
        {{ selectedIds().size }} case(s) selected
      </span>
      @if (authService.hasPermission('CASE_DELETE')) {
        <button
          (click)="deleteSelected()"
          class="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded">
          Delete Selected
        </button>
      }
    </div>
  </div>
}
```

**Bulk Delete:**
```typescript
deleteSelected() {
  const count = this.selectedIds().size;
  const confirmed = confirm(
    `Are you sure you want to delete ${count} case(s)? This action cannot be undone.`
  );

  if (!confirmed) return;

  this.loading.set(true);

  const deleteObs = Array.from(this.selectedIds()).map(id =>
    this.caseService.deleteCase(id)
  );

  forkJoin(deleteObs).subscribe({
    next: () => {
      this.selectedIds.set(new Set());
      this.loadCases();
      // Show success message
    },
    error: (err) => {
      this.error.set('Failed to delete some cases. Please try again.');
      this.loading.set(false);
    }
  });
}
```

---

## 6. File Structure

```
frontend/src/app/
├── core/
│   └── models/
│       └── case.model.ts (NEW - all case interfaces)
├── services/
│   ├── reference-data.service.ts (NEW - global cache)
│   ├── case.service.ts (NEW)
│   ├── tribunal.service.ts (NEW)
│   ├── case-type.service.ts (NEW)
│   ├── case-status.service.ts (NEW)
│   └── lawyer.service.ts (NEW)
└── features/
    └── cases/
        └── case-list/
            ├── case-list.component.ts (NEW)
            └── case-list.component.html (NEW)
```

---

## 7. Implementation Sequence

1. **Models** - Create `case.model.ts` with all TypeScript interfaces
2. **Services** - Create all 6 services (reference-data, case, tribunal, case-type, case-status, lawyer)
3. **App Config** - Add ReferenceDataService to APP_INITIALIZER
4. **Component** - Create CaseListComponent with filter panel + table
5. **Routing** - Add `/cases` route
6. **Navigation** - Add "Cases" to sidebar

---

## 8. Testing Checklist

- [ ] Reference data loads on app startup
- [ ] All dropdowns populate correctly
- [ ] Category dropdown filters by selected case type
- [ ] Search debounces (300ms)
- [ ] Date range validation works
- [ ] Filters trigger table reload
- [ ] Reset filters clears all and reloads
- [ ] Pagination works correctly
- [ ] Status badges show correct colors
- [ ] Bulk selection works
- [ ] Bulk delete requires confirmation
- [ ] Permission checks hide/show buttons correctly
- [ ] Empty state shows when no cases
- [ ] No results state shows when filters return nothing
- [ ] Loading states show correctly
- [ ] Error messages display and dismiss
- [ ] Dark mode styling works

---

## 9. Future Enhancements (Out of Scope)

- TanStack Query for caching/refetching
- Saved filter presets ("My Open Cases", etc.)
- Export to CSV/PDF
- Column customization (show/hide columns)
- Advanced sorting (multi-column)
- Keyboard shortcuts (Ctrl+F for search, etc.)

---

**Design Status:** ✅ Approved
**Next Step:** Create detailed implementation plan using writing-plans skill
