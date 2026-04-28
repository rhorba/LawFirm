import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { FinancialService } from '../../../services/financial.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  TransactionResponse,
  TransactionDirection,
  FinancialFilter,
  FinancialSummary,
} from '../../../core/models/financial.model';
import { PageResponse } from '../../../core/models/case.model';
import { FinancialSummaryCardComponent } from '../shared/financial-summary-card/financial-summary-card.component';
import { TransactionFormComponent } from './transaction-form/transaction-form.component';

@Component({
  selector: 'app-financial-ledger',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, FinancialSummaryCardComponent, TransactionFormComponent],
  templateUrl: './financial-ledger.component.html',
})
export class FinancialLedgerComponent implements OnInit {
  private financialService = inject(FinancialService);
  authService = inject(AuthService);

  data = signal<PageResponse<TransactionResponse> | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  exportLoading = signal(false);
  showCreateModal = signal(false);

  page = signal(0);
  size = signal(20);
  filter = signal<FinancialFilter>({});

  readonly directionOptions: { value: TransactionDirection | ''; label: string }[] = [
    { value: '', label: 'Toutes directions' },
    { value: 'REVENUE', label: 'Revenus' },
    { value: 'EXPENSE', label: 'Dépenses' },
  ];

  summary = computed<FinancialSummary>(() => {
    const content = this.data()?.content ?? [];
    const totalRevenue = content
      .filter((t) => t.direction === 'REVENUE')
      .reduce((s, t) => s + t.amount, 0);
    const totalExpenses = content
      .filter((t) => t.direction === 'EXPENSE')
      .reduce((s, t) => s + t.amount, 0);
    return {
      totalRevenue,
      totalExpenses,
      balance: totalRevenue - totalExpenses,
      transactionCount: this.data()?.totalElements ?? 0,
    };
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.financialService
      .getTransactions(this.filter(), { page: this.page(), size: this.size() })
      .subscribe({
        next: (d) => {
          this.data.set(d);
          this.loading.set(false);
        },
        error: (err: { error?: { message?: string } }) => {
          this.error.set(err.error?.message ?? 'Erreur de chargement');
          this.loading.set(false);
        },
      });
  }

  onDirectionChange(value: string): void {
    this.filter.update((f) => ({
      ...f,
      direction: (value as TransactionDirection) || undefined,
    }));
    this.page.set(0);
    this.load();
  }

  onDateFromChange(value: string): void {
    this.filter.update((f) => ({ ...f, dateFrom: value || undefined }));
    this.page.set(0);
    this.load();
  }

  onDateToChange(value: string): void {
    this.filter.update((f) => ({ ...f, dateTo: value || undefined }));
    this.page.set(0);
    this.load();
  }

  onPageChange(newPage: number): void {
    this.page.set(newPage);
    this.load();
  }

  softDelete(id: number): void {
    if (!confirm('Supprimer cette transaction ?')) return;
    this.financialService.softDeleteTransaction(id).subscribe({
      next: () => this.load(),
      error: (err: { error?: { message?: string } }) =>
        this.error.set(err.error?.message ?? 'Échec de la suppression'),
    });
  }

  exportExcel(): void {
    this.exportLoading.set(true);
    this.financialService.exportExcel(this.filter()).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'transactions.xlsx';
        link.click();
        URL.revokeObjectURL(url);
        this.exportLoading.set(false);
      },
      error: () => this.exportLoading.set(false),
    });
  }

  onTransactionSaved(): void {
    this.showCreateModal.set(false);
    this.load();
  }

  directionBadgeClass(direction: TransactionDirection): string {
    return direction === 'REVENUE'
      ? 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200'
      : 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200';
  }

  directionLabel(direction: TransactionDirection): string {
    return direction === 'REVENUE' ? 'Revenu' : 'Dépense';
  }
}
