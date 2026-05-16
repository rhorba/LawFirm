import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnDestroy,
  signal,
  inject,
  forwardRef,
  HostListener,
  ElementRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Subject, debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs';

export interface SearchableOption {
  label: string;
  value: string | number;
  raw?: unknown;
}

@Component({
  selector: 'app-searchable-select',
  standalone: true,
  imports: [CommonModule, FormsModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SearchableSelectComponent),
      multi: true,
    },
  ],
  templateUrl: './searchable-select.component.html',
})
export class SearchableSelectComponent implements ControlValueAccessor, OnDestroy {
  @Input({ required: true }) endpoint!: string;
  @Input() searchParam = 'search';
  @Input() displayField!: string;
  @Input() valueField = 'id';
  @Input() placeholder = 'Rechercher...';
  @Input() multiple = false;
  @Input() additionalParams: Record<string, string> = {};

  @Output() selectionChange = new EventEmitter<SearchableOption | SearchableOption[] | null>();

  private http = inject(HttpClient);
  private elRef = inject(ElementRef);

  options = signal<SearchableOption[]>([]);
  loading = signal(false);
  open = signal(false);
  query = signal('');
  selectedValues = signal<SearchableOption[]>([]);

  private search$ = new Subject<string>();
  private destroy$ = new Subject<void>();

  private onChange: (val: unknown) => void = () => {};
  private onTouched: () => void = () => {};

  constructor() {
    this.search$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((q) => {
          this.loading.set(true);
          let params = new HttpParams().set(this.searchParam, q).set('page', '0').set('size', '20');
          Object.entries(this.additionalParams).forEach(([k, v]) => {
            params = params.set(k, v);
          });
          return this.http.get<{ content: unknown[] } | unknown[]>(this.endpoint, { params });
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (res) => {
          const items: unknown[] = Array.isArray(res)
            ? res
            : ((res as { content: unknown[] }).content ?? []);
          this.options.set(
            items.map((item: unknown) => ({
              label: String((item as Record<string, unknown>)[this.displayField] ?? ''),
              value: (item as Record<string, unknown>)[this.valueField] as string | number,
              raw: item,
            }))
          );
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elRef.nativeElement.contains(event.target)) {
      this.open.set(false);
    }
  }

  onInput(event: Event): void {
    const q = (event.target as HTMLInputElement).value;
    this.query.set(q);
    this.search$.next(q);
    this.open.set(true);
  }

  onFocus(): void {
    this.search$.next(this.query());
    this.open.set(true);
  }

  select(option: SearchableOption): void {
    if (this.multiple) {
      const existing = this.selectedValues();
      const idx = existing.findIndex((v) => v.value === option.value);
      const updated = idx > -1 ? existing.filter((_, i) => i !== idx) : [...existing, option];
      this.selectedValues.set(updated);
      this.onChange(updated.map((v) => v.value));
      this.selectionChange.emit(updated);
    } else {
      this.selectedValues.set([option]);
      this.query.set(option.label);
      this.open.set(false);
      this.onChange(option.value);
      this.selectionChange.emit(option);
    }
  }

  isSelected(option: SearchableOption): boolean {
    return this.selectedValues().some((v) => v.value === option.value);
  }

  clear(): void {
    this.selectedValues.set([]);
    this.query.set('');
    this.onChange(this.multiple ? [] : null);
    this.selectionChange.emit(null);
    this.search$.next('');
  }

  writeValue(val: unknown): void {
    if (!val) {
      this.selectedValues.set([]);
      this.query.set('');
      return;
    }
    const arr = Array.isArray(val) ? val : [val];
    this.selectedValues.set(
      arr.map((v) => ({
        label: String(v),
        value: v as string | number,
      }))
    );
    if (!this.multiple && arr.length === 1) {
      this.query.set(String(arr[0]));
    }
  }

  registerOnChange(fn: (val: unknown) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
