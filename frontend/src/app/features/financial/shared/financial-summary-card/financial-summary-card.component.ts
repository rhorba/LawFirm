import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FinancialSummary } from '../../../../core/models/financial.model';

@Component({
  selector: 'app-financial-summary-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './financial-summary-card.component.html',
})
export class FinancialSummaryCardComponent {
  @Input({ required: true }) summary!: FinancialSummary;
}
