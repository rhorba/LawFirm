import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentMode } from '../../../../core/models/financial.model';

export interface PaymentModalResult {
  paymentMode: PaymentMode;
  paymentDate: string;
  paymentReference?: string;
}

@Component({
  selector: 'app-payment-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './payment-modal.component.html',
})
export class PaymentModalComponent {
  @Input({ required: true }) invoiceNumber!: string;
  @Input({ required: true }) totalAmount!: number;
  @Output() confirmed = new EventEmitter<PaymentModalResult>();
  @Output() cancelled = new EventEmitter<void>();

  paymentMode = signal<PaymentMode | ''>('');
  paymentDate = signal(new Date().toISOString().substring(0, 10));
  paymentReference = signal('');
  error = signal<string | null>(null);

  readonly paymentModeOptions: { value: PaymentMode; label: string }[] = [
    { value: 'CHECK', label: 'Chèque' },
    { value: 'TRANSFER', label: 'Virement' },
    { value: 'CASH', label: 'Espèces' },
    { value: 'CREDIT_CARD', label: 'Carte bancaire' },
    { value: 'MONEY_ORDER', label: 'Mandat' },
  ];

  submit(): void {
    if (!this.paymentMode() || !this.paymentDate()) {
      this.error.set('Mode de paiement et date sont obligatoires.');
      return;
    }
    this.confirmed.emit({
      paymentMode: this.paymentMode() as PaymentMode,
      paymentDate: this.paymentDate(),
      paymentReference: this.paymentReference() || undefined,
    });
  }
}
