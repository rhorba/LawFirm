import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FinancialService } from '../../../../services/financial.service';
import { AuthService } from '../../../../core/services/auth.service';
import { InvoiceResponse, InvoiceStatus } from '../../../../core/models/financial.model';
import { PaymentModalComponent, PaymentModalResult } from '../payment-modal/payment-modal.component';

@Component({
  selector: 'app-invoice-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, PaymentModalComponent],
  templateUrl: './invoice-detail.component.html',
})
export class InvoiceDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private financialService = inject(FinancialService);
  authService = inject(AuthService);

  invoice = signal<InvoiceResponse | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  statusLoading = signal(false);
  showPaymentModal = signal(false);

  readonly TRANSITIONS: Partial<Record<InvoiceStatus, InvoiceStatus[]>> = {
    DRAFT: ['SENT', 'CANCELLED'],
    SENT: ['PAID', 'CANCELLED'],
  };

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.load(+id);
  }

  load(id: number): void {
    this.loading.set(true);
    this.financialService.getInvoice(id).subscribe({
      next: (inv) => {
        this.invoice.set(inv);
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.error.set(err.error?.message ?? 'Erreur');
        this.loading.set(false);
      },
    });
  }

  nextStatuses(): InvoiceStatus[] {
    const inv = this.invoice();
    return inv ? (this.TRANSITIONS[inv.status] ?? []) : [];
  }

  transitionTo(status: InvoiceStatus): void {
    if (status === 'PAID') {
      this.showPaymentModal.set(true);
      return;
    }
    const inv = this.invoice();
    if (!inv || !confirm(`Passer la facture en statut "${this.statusLabel(status)}" ?`)) return;
    this.statusLoading.set(true);
    this.financialService.updateInvoiceStatus(inv.id, { status }).subscribe({
      next: (updated) => {
        this.invoice.set(updated);
        this.statusLoading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.error.set(err.error?.message ?? 'Échec');
        this.statusLoading.set(false);
      },
    });
  }

  onPaymentConfirmed(result: PaymentModalResult): void {
    const inv = this.invoice();
    if (!inv) return;
    this.statusLoading.set(true);
    this.financialService.updateInvoiceStatus(inv.id, {
      status: 'PAID',
      paymentMode: result.paymentMode,
      paymentDate: result.paymentDate,
      paymentReference: result.paymentReference,
    }).subscribe({
      next: (updated) => {
        this.invoice.set(updated);
        this.showPaymentModal.set(false);
        this.statusLoading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.error.set(err.error?.message ?? 'Erreur lors du paiement');
        this.statusLoading.set(false);
      },
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
