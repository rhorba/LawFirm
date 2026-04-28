import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FinancialService } from '../../../../services/financial.service';
import { AuthService } from '../../../../core/services/auth.service';
import { InvoiceResponse, InvoiceStatus } from '../../../../core/models/financial.model';
import { PageResponse } from '../../../../core/models/case.model';

@Component({
  selector: 'app-invoice-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './invoice-list.component.html',
})
export class InvoiceListComponent implements OnInit {
  private financialService = inject(FinancialService);
  authService = inject(AuthService);

  data = signal<PageResponse<InvoiceResponse> | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  page = signal(0);
  size = signal(20);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.financialService.getInvoices({ page: this.page(), size: this.size() }).subscribe({
      next: (d) => {
        this.data.set(d);
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.error.set(err.error?.message ?? 'Erreur');
        this.loading.set(false);
      },
    });
  }

  onPageChange(newPage: number): void {
    this.page.set(newPage);
    this.load();
  }

  softDelete(id: number): void {
    if (!confirm('Supprimer cette facture ?')) return;
    this.financialService.softDeleteInvoice(id).subscribe({
      next: () => this.load(),
    });
  }

  statusBadgeClass(status: InvoiceStatus): string {
    const map: Record<InvoiceStatus, string> = {
      DRAFT: 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200',
      SENT: 'bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200',
      PAID: 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200',
      CANCELLED: 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200',
    };
    return map[status] ?? '';
  }

  statusLabel(status: InvoiceStatus): string {
    const map: Record<InvoiceStatus, string> = {
      DRAFT: 'Brouillon',
      SENT: 'Envoyée',
      PAID: 'Payée',
      CANCELLED: 'Annulée',
    };
    return map[status] ?? status;
  }
}
