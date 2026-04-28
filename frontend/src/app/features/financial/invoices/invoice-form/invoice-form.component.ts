import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FinancialService } from '../../../../services/financial.service';
import { CaseService } from '../../../../services/case.service';
import {
  InvoiceItemRequest,
  InvoiceRequest,
  OperationType,
} from '../../../../core/models/financial.model';
import { CaseSummary } from '../../../../core/models/case.model';

@Component({
  selector: 'app-invoice-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './invoice-form.component.html',
})
export class InvoiceFormComponent implements OnInit {
  private financialService = inject(FinancialService);
  private caseService = inject(CaseService);
  private router = inject(Router);

  loading = signal(false);
  error = signal<string | null>(null);
  cases = signal<CaseSummary[]>([]);

  // Form fields
  caseId = signal<number | null>(null);
  issueDate = signal('');
  dueDate = signal('');
  taxRate = signal(0);
  notes = signal('');
  items = signal<InvoiceItemRequest[]>([
    { description: '', operationType: 'OTHER', quantity: 1, unitPrice: 0 },
  ]);

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

  ngOnInit(): void {
    this.caseService.searchCases({ page: 0, size: 200 }).subscribe({
      next: (page) => this.cases.set(page.content),
    });
  }

  get subtotal(): number {
    return this.items().reduce((s, item) => s + item.quantity * item.unitPrice, 0);
  }

  get taxAmount(): number {
    return this.subtotal * this.taxRate() / 100;
  }

  get total(): number {
    return this.subtotal + this.taxAmount;
  }

  addItem(): void {
    this.items.update((list) => [
      ...list,
      { description: '', operationType: 'OTHER', quantity: 1, unitPrice: 0 },
    ]);
  }

  removeItem(index: number): void {
    this.items.update((list) => list.filter((_, i) => i !== index));
  }

  updateItem(index: number, field: keyof InvoiceItemRequest, value: unknown): void {
    this.items.update((list) => {
      const updated = [...list];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  }

  submit(): void {
    if (!this.caseId() || !this.issueDate() || this.items().length === 0) {
      this.error.set('Dossier, date et au moins un article sont obligatoires.');
      return;
    }
    const request: InvoiceRequest = {
      caseId: this.caseId()!,
      issueDate: this.issueDate(),
      dueDate: this.dueDate() || undefined,
      taxAmount: this.taxAmount,
      notes: this.notes() || undefined,
      items: this.items(),
    };
    this.loading.set(true);
    this.error.set(null);
    this.financialService.createInvoice(request).subscribe({
      next: (inv) => this.router.navigate(['/financial/invoices', inv.id]),
      error: (err: { error?: { message?: string } }) => {
        this.error.set(err.error?.message ?? 'Échec');
        this.loading.set(false);
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/financial/invoices']);
  }
}
