import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FinancialService } from '../../../../services/financial.service';
import { AuthService } from '../../../../core/services/auth.service';
import { TransactionResponse, FinancialSummary } from '../../../../core/models/financial.model';
import { FinancialSummaryCardComponent } from '../../../financial/shared/financial-summary-card/financial-summary-card.component';
import { TransactionFormComponent } from '../../../financial/ledger/transaction-form/transaction-form.component';

@Component({
  selector: 'app-financial-tab',
  standalone: true,
  imports: [CommonModule, FinancialSummaryCardComponent, TransactionFormComponent],
  templateUrl: './financial-tab.component.html',
})
export class FinancialTabComponent implements OnInit {
  @Input({ required: true }) caseId!: number;

  private financialService = inject(FinancialService);
  authService = inject(AuthService);

  transactions = signal<TransactionResponse[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  showCreateModal = signal(false);

  summary = signal<FinancialSummary>({
    totalRevenue: 0,
    totalExpenses: 0,
    balance: 0,
    transactionCount: 0,
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.financialService.getTransactionsByCase(this.caseId).subscribe({
      next: (data) => {
        this.transactions.set(data);
        const totalRevenue = data
          .filter((t) => t.direction === 'REVENUE')
          .reduce((s, t) => s + t.amount, 0);
        const totalExpenses = data
          .filter((t) => t.direction === 'EXPENSE')
          .reduce((s, t) => s + t.amount, 0);
        this.summary.set({
          totalRevenue,
          totalExpenses,
          balance: totalRevenue - totalExpenses,
          transactionCount: data.length,
        });
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.error.set(err.error?.message ?? 'Erreur de chargement');
        this.loading.set(false);
      },
    });
  }

  softDelete(id: number): void {
    if (!confirm('Supprimer cette transaction ?')) return;
    this.financialService.softDeleteTransaction(id).subscribe({
      next: () => this.load(),
      error: (err: { error?: { message?: string } }) =>
        this.error.set(err.error?.message ?? 'Échec de la suppression'),
    });
  }

  onTransactionSaved(): void {
    this.showCreateModal.set(false);
    this.load();
  }

  directionBadgeClass(direction: string): string {
    return direction === 'REVENUE'
      ? 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200'
      : 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200';
  }

  directionLabel(direction: string): string {
    return direction === 'REVENUE' ? 'Revenu' : 'Dépense';
  }
}
