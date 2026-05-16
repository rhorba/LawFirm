import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { ReportService } from '../../services/report.service';
import {
  LawyerWorkloadEntry,
  ReportSummaryResponse,
  UnpaidInvoiceEntry,
} from '../../core/models/report.model';

Chart.register(...registerables);

type Preset = 'month' | 'quarter' | 'year' | 'custom';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, DecimalPipe, FormsModule, RouterLink],
  template: `
    <div class="p-6 space-y-6">
      <div class="flex items-center justify-between">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Rapports & Analytiques</h1>
        <button
          onclick="window.print()"
          class="px-4 py-2 text-sm font-medium text-gray-600 dark:text-gray-400 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors print:hidden"
        >
          Imprimer
        </button>
      </div>

      <!-- Date range selector -->
      <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4 print:hidden">
        <div class="flex flex-wrap gap-2 items-center">
          <span class="text-sm text-gray-500 dark:text-gray-400 mr-2">Période :</span>
          @for (p of presets; track p.value) {
            <button
              (click)="setPreset(p.value)"
              [class]="preset() === p.value ? activePresetClass : inactivePresetClass"
            >
              {{ p.label }}
            </button>
          }
          @if (preset() === 'custom') {
            <input
              type="date"
              [(ngModel)]="customFrom"
              (change)="loadAll()"
              class="px-2 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
            />
            <span class="text-gray-400">→</span>
            <input
              type="date"
              [(ngModel)]="customTo"
              (change)="loadAll()"
              class="px-2 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
            />
          }
        </div>
      </div>

      <!-- KPI Cards -->
      @if (summary()) {
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">Total dossiers</p>
            <p class="text-2xl font-bold text-gray-900 dark:text-white">{{ summary()!.totalCases }}</p>
            <p class="text-xs text-blue-600 dark:text-blue-400 mt-1">{{ summary()!.openCases }} ouverts</p>
          </div>
          <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">Revenus période</p>
            <p class="text-2xl font-bold text-green-600 dark:text-green-400">
              {{ summary()!.totalRevenue | number: '1.0-0' }}
            </p>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">MAD</p>
          </div>
          <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">Non facturé</p>
            <p class="text-2xl font-bold text-amber-600 dark:text-amber-400">
              {{ summary()!.unbilledAmount | number: '1.0-0' }}
            </p>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {{ summary()!.unbilledHours | number: '1.1-1' }}h non facturées
            </p>
          </div>
          <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">Alertes</p>
            <p class="text-2xl font-bold text-red-600 dark:text-red-400">{{ summary()!.overdueTasks }}</p>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              tâches en retard · {{ summary()!.pendingInvoices }} factures impayées
            </p>
          </div>
        </div>
      }

      <!-- Charts row -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
          <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-4">Dossiers par statut</h3>
          <div class="relative h-56">
            <canvas #statusChart></canvas>
          </div>
        </div>
        <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
          <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-4">Dossiers par mois</h3>
          <div class="relative h-56">
            <canvas #monthChart></canvas>
          </div>
        </div>
      </div>

      <!-- Financial chart -->
      <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
        <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-4">Revenus & Dépenses par mois (MAD)</h3>
        <div class="relative h-64">
          <canvas #financialChart></canvas>
        </div>
      </div>

      <!-- Lawyer workload -->
      @if (lawyers().length > 0) {
        <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
          <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-4">Charge de travail par avocat</h3>
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="text-xs text-gray-500 dark:text-gray-400 uppercase border-b border-gray-200 dark:border-gray-700">
                <tr>
                  <th class="pb-2 text-left">Avocat</th>
                  <th class="pb-2 text-right">Heures totales</th>
                  <th class="pb-2 text-right">Heures facturables</th>
                  <th class="pb-2 text-right">Montant facturable (MAD)</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
                @for (l of lawyers(); track l.lawyerId) {
                  <tr class="hover:bg-gray-50 dark:hover:bg-gray-700/40">
                    <td class="py-2 text-gray-900 dark:text-gray-100 font-medium">{{ l.lawyerName }}</td>
                    <td class="py-2 text-right text-gray-700 dark:text-gray-300">{{ l.totalHours | number: '1.1-1' }}h</td>
                    <td class="py-2 text-right text-blue-600 dark:text-blue-400">{{ l.billableHours | number: '1.1-1' }}h</td>
                    <td class="py-2 text-right text-green-600 dark:text-green-400">{{ l.billableAmount | number: '1.0-0' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      }

      <!-- Unpaid invoices -->
      @if (unpaid().length > 0) {
        <div class="bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4">
          <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-4">
            Factures impayées (top {{ unpaid().length }})
          </h3>
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="text-xs text-gray-500 dark:text-gray-400 uppercase border-b border-gray-200 dark:border-gray-700">
                <tr>
                  <th class="pb-2 text-left">N° Facture</th>
                  <th class="pb-2 text-left">Dossier</th>
                  <th class="pb-2 text-right">Montant (MAD)</th>
                  <th class="pb-2 text-left">Échéance</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
                @for (inv of unpaid(); track inv.invoiceId) {
                  <tr class="hover:bg-gray-50 dark:hover:bg-gray-700/40">
                    <td class="py-2 text-blue-600 dark:text-blue-400">
                      <a [routerLink]="['/financial/invoices', inv.invoiceId]">{{ inv.invoiceNumber }}</a>
                    </td>
                    <td class="py-2 text-gray-700 dark:text-gray-300">{{ inv.caseNumber }}</td>
                    <td class="py-2 text-right font-medium text-gray-900 dark:text-gray-100">
                      {{ inv.totalAmount | number: '1.0-0' }}
                    </td>
                    <td class="py-2 text-gray-500 dark:text-gray-400">{{ inv.dueDate ?? '—' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      }
    </div>
  `,
})
export class ReportsComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('statusChart') statusChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('monthChart') monthChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('financialChart') financialChartRef!: ElementRef<HTMLCanvasElement>;

  private reportService = inject(ReportService);

  summary = signal<ReportSummaryResponse | null>(null);
  lawyers = signal<LawyerWorkloadEntry[]>([]);
  unpaid = signal<UnpaidInvoiceEntry[]>([]);

  preset = signal<Preset>('year');
  customFrom = '';
  customTo = '';

  private statusChartInstance?: Chart;
  private monthChartInstance?: Chart;
  private financialChartInstance?: Chart;
  private chartsReady = false;

  readonly presets: { value: Preset; label: string }[] = [
    { value: 'month', label: 'Ce mois' },
    { value: 'quarter', label: '3 mois' },
    { value: 'year', label: '12 mois' },
    { value: 'custom', label: 'Personnalisé' },
  ];

  readonly activePresetClass =
    'px-3 py-1.5 text-sm font-medium rounded-lg bg-blue-600 text-white';
  readonly inactivePresetClass =
    'px-3 py-1.5 text-sm font-medium rounded-lg border border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700';

  ngOnInit(): void {
    this.loadAll();
  }

  ngAfterViewInit(): void {
    this.chartsReady = true;
    this.loadCharts();
  }

  ngOnDestroy(): void {
    this.statusChartInstance?.destroy();
    this.monthChartInstance?.destroy();
    this.financialChartInstance?.destroy();
  }

  setPreset(p: Preset): void {
    this.preset.set(p);
    if (p !== 'custom') this.loadAll();
  }

  loadAll(): void {
    const { from, to } = this.dateRange();
    this.reportService.getSummary(from, to).subscribe({ next: (s) => this.summary.set(s), error: () => {} });
    this.reportService.getLawyerWorkload(from, to).subscribe({ next: (l) => this.lawyers.set(l), error: () => {} });
    this.reportService.getUnpaidInvoices().subscribe({ next: (u) => this.unpaid.set(u), error: () => {} });
    if (this.chartsReady) this.loadCharts();
  }

  private loadCharts(): void {
    const { from, to } = this.dateRange();
    this.reportService.getCasesByStatus(from, to).subscribe({
      next: (d) => this.renderDoughnut(d.labels, d.values),
      error: () => {},
    });
    this.reportService.getCasesByMonth(from, to).subscribe({
      next: (d) => this.renderBar(d.labels, d.values),
      error: () => {},
    });
    this.reportService.getFinancialByMonth(from, to).subscribe({
      next: (d) => this.renderLine(d.labels, d.values, d.secondaryValues),
      error: () => {},
    });
  }

  private renderDoughnut(labels: string[], values: number[]): void {
    this.statusChartInstance?.destroy();
    const colors = ['#3b82f6','#f59e0b','#10b981','#8b5cf6','#ef4444','#6b7280','#ec4899'];
    this.statusChartInstance = new Chart(this.statusChartRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{ data: values, backgroundColor: colors.slice(0, labels.length), borderWidth: 1 }],
      },
      options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'right' } } },
    });
  }

  private renderBar(labels: string[], values: number[]): void {
    this.monthChartInstance?.destroy();
    this.monthChartInstance = new Chart(this.monthChartRef.nativeElement, {
      type: 'bar',
      data: {
        labels,
        datasets: [{ label: 'Dossiers', data: values, backgroundColor: '#3b82f6', borderRadius: 4 }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } },
      },
    });
  }

  private renderLine(labels: string[], revenue: number[], expenses: number[]): void {
    this.financialChartInstance?.destroy();
    this.financialChartInstance = new Chart(this.financialChartRef.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [
          { label: 'Revenus', data: revenue, borderColor: '#10b981', backgroundColor: '#10b98120', fill: true, tension: 0.3 },
          { label: 'Dépenses', data: expenses, borderColor: '#ef4444', backgroundColor: '#ef444420', fill: true, tension: 0.3 },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'top' } },
        scales: { y: { beginAtZero: true } },
      },
    });
  }

  private dateRange(): { from: string; to: string } {
    const today = new Date();
    const fmt = (d: Date) => d.toISOString().substring(0, 10);
    const to = fmt(today);
    switch (this.preset()) {
      case 'month':
        return { from: fmt(new Date(today.getFullYear(), today.getMonth(), 1)), to };
      case 'quarter':
        return { from: fmt(new Date(today.getFullYear(), today.getMonth() - 3, today.getDate())), to };
      case 'custom':
        return { from: this.customFrom || fmt(new Date(today.getFullYear() - 1, today.getMonth(), today.getDate())), to: this.customTo || to };
      default: // year
        return { from: fmt(new Date(today.getFullYear() - 1, today.getMonth(), today.getDate())), to };
    }
  }
}
