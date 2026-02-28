import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, debounceTime, takeUntil } from 'rxjs';
import { ClientService } from '../../../services/client.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  ClientSummary,
  ClientResponse,
  ClientType,
  CreateClientRequest,
  UpdateClientRequest,
} from '../../../core/models/client.model';
import { PageResponse } from '../../../core/models/case.model';

@Component({
  selector: 'app-client-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './client-list.component.html',
})
export class ClientListComponent implements OnInit, OnDestroy {
  private clientService = inject(ClientService);
  authService = inject(AuthService);
  private destroy$ = new Subject<void>();

  Math = Math;

  // Data
  clients = signal<PageResponse<ClientSummary> | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);

  // Pagination
  page = signal(0);
  size = signal(5);

  // Filters
  search = signal('');
  typeFilter = signal<ClientType | ''>('');

  // Export
  exportLoading = signal(false);

  // Modal
  showModal = signal(false);
  editingClient = signal<ClientResponse | null>(null);
  modalLoading = signal(false);
  modalError = signal<string | null>(null);

  // Form
  form = signal<Partial<CreateClientRequest>>({ clientType: 'INDIVIDUAL' });

  readonly typeOptions: { value: ClientType; label: string }[] = [
    { value: 'INDIVIDUAL', label: 'Individual' },
    { value: 'CORPORATE', label: 'Corporate' },
    { value: 'GOVERNMENT', label: 'Government' },
  ];

  readonly genderOptions = [
    { value: 'MALE', label: 'Male' },
    { value: 'FEMALE', label: 'Female' },
  ];

  isIndividual = computed(() => this.form().clientType === 'INDIVIDUAL');
  isCorporate = computed(
    () => this.form().clientType === 'CORPORATE' || this.form().clientType === 'GOVERNMENT'
  );
  showTaxNumber = computed(() => this.form().clientType === 'CORPORATE');

  private searchSubject = new Subject<string>();

  ngOnInit(): void {
    this.searchSubject.pipe(debounceTime(300), takeUntil(this.destroy$)).subscribe(() => {
      this.page.set(0);
      this.loadClients();
    });
    this.loadClients();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadClients(): void {
    this.loading.set(true);
    this.error.set(null);
    this.clientService
      .search({
        search: this.search() || undefined,
        type: this.typeFilter() || undefined,
        page: this.page(),
        size: this.size(),
      })
      .subscribe({
        next: (data) => {
          this.clients.set(data);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(err.error?.message ?? 'Failed to load clients');
          this.loading.set(false);
        },
      });
  }

  onSearchChange(value: string): void {
    this.search.set(value);
    this.searchSubject.next(value);
  }

  onTypeFilterChange(value: string): void {
    this.typeFilter.set(value as ClientType | '');
    this.page.set(0);
    this.loadClients();
  }

  onPageChange(newPage: number): void {
    this.page.set(newPage);
    this.loadClients();
  }

  clearFilters(): void {
    this.search.set('');
    this.typeFilter.set('');
    this.page.set(0);
    this.loadClients();
  }

  // ── Modal ──────────────────────────────────────────────────────────────

  openCreate(): void {
    this.editingClient.set(null);
    this.form.set({ clientType: 'INDIVIDUAL' });
    this.modalError.set(null);
    this.showModal.set(true);
  }

  openEdit(id: number): void {
    this.modalLoading.set(true);
    this.showModal.set(true);
    this.modalError.set(null);
    this.clientService.getById(id).subscribe({
      next: (client) => {
        this.editingClient.set(client);
        this.form.set({
          clientType: client.clientType,
          firstName: client.firstName,
          lastName: client.lastName,
          phone: client.phone,
          email: client.email,
          address: client.address,
          notes: client.notes,
          cin: client.cin,
          gender: client.gender,
          dateOfBirth: client.dateOfBirth,
          companyName: client.companyName,
          taxNumber: client.taxNumber,
        });
        this.modalLoading.set(false);
      },
      error: (err) => {
        this.modalError.set(err.error?.message ?? 'Failed to load client');
        this.modalLoading.set(false);
      },
    });
  }

  closeModal(): void {
    this.showModal.set(false);
    this.editingClient.set(null);
    this.modalError.set(null);
    this.modalLoading.set(false);
  }

  onTypeChange(value: string): void {
    this.form.update((f) => ({ ...f, clientType: value as ClientType }));
  }

  updateField(field: string, value: string): void {
    this.form.update((f) => ({ ...f, [field]: value || undefined }));
  }

  saveClient(): void {
    this.modalLoading.set(true);
    this.modalError.set(null);
    const editing = this.editingClient();
    if (editing) {
      const req: UpdateClientRequest = {
        firstName: this.form().firstName,
        lastName: this.form().lastName,
        phone: this.form().phone,
        email: this.form().email,
        address: this.form().address,
        notes: this.form().notes,
        cin: this.form().cin,
        gender: this.form().gender,
        dateOfBirth: this.form().dateOfBirth,
        companyName: this.form().companyName,
        taxNumber: this.form().taxNumber,
      };
      this.clientService.update(editing.id, req).subscribe({
        next: () => {
          this.closeModal();
          this.loadClients();
        },
        error: (err) => {
          this.modalError.set(err.error?.message ?? 'Update failed');
          this.modalLoading.set(false);
        },
      });
    } else {
      this.clientService.create(this.form() as CreateClientRequest).subscribe({
        next: () => {
          this.closeModal();
          this.loadClients();
        },
        error: (err) => {
          this.modalError.set(err.error?.message ?? 'Create failed');
          this.modalLoading.set(false);
        },
      });
    }
  }

  deactivateClient(id: number): void {
    if (!confirm('Deactivate this client?')) return;
    this.clientService.deactivate(id).subscribe({
      next: () => this.loadClients(),
      error: (err) => this.error.set(err.error?.message ?? 'Deactivation failed'),
    });
  }

  exportClients(): void {
    this.exportLoading.set(true);
    this.clientService
      .export(this.search() || undefined, this.typeFilter() || undefined)
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = 'clients-export.xlsx';
          link.click();
          URL.revokeObjectURL(url);
          this.exportLoading.set(false);
        },
        error: () => {
          this.exportLoading.set(false);
        },
      });
  }

  typeBadgeClass(type: ClientType): string {
    const map: Record<ClientType, string> = {
      INDIVIDUAL: 'bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200',
      CORPORATE: 'bg-purple-100 dark:bg-purple-900 text-purple-800 dark:text-purple-200',
      GOVERNMENT: 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200',
    };
    return map[type] ?? '';
  }
}
