import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FinancialService } from '../../../../services/financial.service';
import { CaseService } from '../../../../services/case.service';
import { CaseSummary } from '../../../../core/models/case.model';
import {
  TransactionDirection,
  OperationType,
  PaymentMode,
  TransactionRequest,
} from '../../../../core/models/financial.model';

@Component({
  selector: 'app-transaction-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transaction-form.component.html',
})
export class TransactionFormComponent implements OnInit {
  @Input() caseId?: number;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  private financialService = inject(FinancialService);
  private caseService = inject(CaseService);

  loading = signal(false);
  error = signal<string | null>(null);
  cases = signal<CaseSummary[]>([]);

  form = signal<Partial<TransactionRequest>>({
    direction: 'EXPENSE',
    operationType: 'OTHER',
  });

  readonly directionOptions: { value: TransactionDirection; label: string }[] = [
    { value: 'REVENUE', label: 'Revenu' },
    { value: 'EXPENSE', label: 'Dépense' },
  ];

  readonly operationTypeOptions: { value: OperationType; label: string }[] = [
    { value: 'OPENING_FEE', label: "Frais d'ouverture" },
    { value: 'PROCEDURE_FEE', label: 'Frais de procédure' },
    { value: 'INTERVENTION_FEE', label: "Frais d'intervention" },
    { value: 'EXPERT_FEE', label: "Frais d'expert" },
    { value: 'DOCUMENT_FEE', label: 'Frais de document' },
    { value: 'NOTIFICATION_FEE', label: 'Frais de notification' },
    { value: 'JUDICIAL_TAX', label: 'Taxe judiciaire' },
    { value: 'OTHER', label: 'Autre' },
  ];

  readonly paymentModeOptions: { value: PaymentMode; label: string }[] = [
    { value: 'CHECK', label: 'Chèque' },
    { value: 'TRANSFER', label: 'Virement' },
    { value: 'CASH', label: 'Espèces' },
    { value: 'CREDIT_CARD', label: 'Carte bancaire' },
    { value: 'MONEY_ORDER', label: 'Mandat' },
  ];

  isExpense(): boolean {
    return this.form().direction === 'EXPENSE';
  }

  ngOnInit(): void {
    if (this.caseId) {
      this.form.update((f) => ({ ...f, caseId: this.caseId }));
    } else {
      this.caseService.searchCases({ page: 0, size: 200 }).subscribe({
        next: (page) => this.cases.set(page.content),
        error: () => {},
      });
    }
  }

  updateField(field: keyof TransactionRequest, value: unknown): void {
    this.form.update((f) => ({ ...f, [field]: value || undefined }));
  }

  onDirectionChange(value: string): void {
    this.form.update((f) => ({
      ...f,
      direction: value as TransactionDirection,
      operationType: value === 'REVENUE' ? 'OTHER' : f.operationType,
    }));
  }

  submit(): void {
    const f = this.form();
    if (!f.caseId || !f.direction || !f.operationType || !f.amount) {
      this.error.set('Veuillez remplir tous les champs obligatoires.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.financialService.createTransaction(f as TransactionRequest).subscribe({
      next: () => {
        this.loading.set(false);
        this.saved.emit();
      },
      error: (err: { error?: { message?: string } }) => {
        this.error.set(err.error?.message ?? 'Échec de la création');
        this.loading.set(false);
      },
    });
  }
}
