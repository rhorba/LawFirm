import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TimeEntryService } from '../../../../services/time-entry.service';
import { LawyerService } from '../../../../services/lawyer.service';
import { AuthService } from '../../../../core/services/auth.service';
import {
  TimeEntryResponse,
  TimeEntrySummaryResponse,
  TimeEntryRequest,
} from '../../../../core/models/time-entry.model';
import { LawyerResponse } from '../../../../core/models/lawyer.model';

@Component({
  selector: 'app-time-tab',
  standalone: true,
  imports: [CommonModule, DatePipe, DecimalPipe, FormsModule],
  template: `
    <div class="space-y-4">
      <!-- Summary cards -->
      @if (summary()) {
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
          <div class="bg-gray-50 dark:bg-gray-900/40 rounded-lg p-3 text-center">
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">Total heures</p>
            <p class="text-lg font-bold text-gray-900 dark:text-gray-100">
              {{ summary()!.totalHours | number: '1.1-2' }}
            </p>
          </div>
          <div class="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-3 text-center">
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">Facturable</p>
            <p class="text-lg font-bold text-blue-700 dark:text-blue-300">
              {{ summary()!.billableHours | number: '1.1-2' }}h
            </p>
            <p class="text-xs text-blue-600 dark:text-blue-400">
              {{ summary()!.billableAmount | number: '1.2-2' }} MAD
            </p>
          </div>
          <div class="bg-green-50 dark:bg-green-900/20 rounded-lg p-3 text-center">
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">Facturé</p>
            <p class="text-lg font-bold text-green-700 dark:text-green-300">
              {{ summary()!.billedHours | number: '1.1-2' }}h
            </p>
            <p class="text-xs text-green-600 dark:text-green-400">
              {{ summary()!.billedAmount | number: '1.2-2' }} MAD
            </p>
          </div>
          <div class="bg-amber-50 dark:bg-amber-900/20 rounded-lg p-3 text-center">
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-1">Non facturé</p>
            <p class="text-lg font-bold text-amber-700 dark:text-amber-300">
              {{ summary()!.unbilledHours | number: '1.1-2' }}h
            </p>
            <p class="text-xs text-amber-600 dark:text-amber-400">
              {{ summary()!.unbilledAmount | number: '1.2-2' }} MAD
            </p>
          </div>
        </div>
      }

      <!-- Add entry form -->
      @if (authService.hasPermission('TIME_CREATE')) {
        @if (!showForm()) {
          <button
            (click)="showForm.set(true)"
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors"
          >
            + Saisir du temps
          </button>
        } @else {
          <div
            class="bg-gray-50 dark:bg-gray-900/50 rounded-lg border border-gray-200 dark:border-gray-700 p-4 space-y-3"
          >
            <h4 class="text-sm font-medium text-gray-700 dark:text-gray-300">
              {{ editingEntry() ? 'Modifier l'entrée' : 'Nouvelle entrée de temps' }}
            </h4>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
                  >Avocat *</label
                >
                <select
                  [(ngModel)]="form.lawyerId"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option [ngValue]="null">— Sélectionner —</option>
                  @for (l of lawyers(); track l.id) {
                    <option [ngValue]="l.id">{{ l.firstName }} {{ l.lastName }}</option>
                  }
                </select>
              </div>
              <div>
                <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
                  >Date *</label
                >
                <input
                  type="date"
                  [(ngModel)]="form.entryDate"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
                  >Heures *</label
                >
                <input
                  type="number"
                  [(ngModel)]="form.hours"
                  min="0.01"
                  step="0.25"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
                  >Taux horaire (MAD)</label
                >
                <input
                  type="number"
                  [(ngModel)]="form.hourlyRate"
                  min="0"
                  step="50"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
                >Description *</label
              >
              <textarea
                [(ngModel)]="form.description"
                rows="2"
                class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              ></textarea>
            </div>
            <div class="flex items-center gap-2">
              <input type="checkbox" [(ngModel)]="form.billable" class="w-4 h-4 rounded" />
              <label class="text-sm text-gray-700 dark:text-gray-300">Facturable</label>
            </div>
            <div class="flex gap-2 pt-1">
              <button
                (click)="cancelForm()"
                class="px-4 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200"
              >
                Annuler
              </button>
              <button
                (click)="submitForm()"
                [disabled]="!isFormValid()"
                class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-lg transition-colors"
              >
                {{ editingEntry() ? 'Enregistrer' : 'Ajouter' }}
              </button>
            </div>
          </div>
        }
      }

      <!-- Entries table -->
      @if (loading()) {
        <div class="text-sm text-gray-400 dark:text-gray-500 text-center py-6">Chargement...</div>
      } @else if (entries().length === 0) {
        <div class="text-sm text-gray-400 dark:text-gray-500 text-center py-6">
          Aucune entrée de temps pour ce dossier.
        </div>
      } @else {
        <div class="overflow-x-auto rounded-lg border border-gray-200 dark:border-gray-700">
          <table class="w-full text-sm">
            <thead
              class="bg-gray-50 dark:bg-gray-900/50 text-xs text-gray-500 dark:text-gray-400 uppercase tracking-wide"
            >
              <tr>
                <th class="px-4 py-3 text-left">Date</th>
                <th class="px-4 py-3 text-left">Avocat</th>
                <th class="px-4 py-3 text-left">Description</th>
                <th class="px-4 py-3 text-right">Heures</th>
                <th class="px-4 py-3 text-right">Taux</th>
                <th class="px-4 py-3 text-right">Montant</th>
                <th class="px-4 py-3 text-center">Statut</th>
                <th class="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
              @for (entry of entries(); track entry.id) {
                <tr class="bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700/50">
                  <td class="px-4 py-3 text-gray-500 dark:text-gray-400 whitespace-nowrap">
                    {{ entry.entryDate | date: 'dd/MM/yyyy' }}
                  </td>
                  <td class="px-4 py-3 text-gray-900 dark:text-gray-100 whitespace-nowrap">
                    {{ entry.lawyerFullName }}
                  </td>
                  <td class="px-4 py-3 text-gray-700 dark:text-gray-300 max-w-[220px] truncate">
                    {{ entry.description }}
                  </td>
                  <td class="px-4 py-3 text-right font-medium text-gray-900 dark:text-gray-100">
                    {{ entry.hours | number: '1.1-2' }}h
                  </td>
                  <td
                    class="px-4 py-3 text-right text-gray-500 dark:text-gray-400 whitespace-nowrap"
                  >
                    {{ entry.hourlyRate | number: '1.0-0' }} MAD
                  </td>
                  <td
                    class="px-4 py-3 text-right font-medium"
                    [class]="
                      entry.billable
                        ? 'text-blue-600 dark:text-blue-400'
                        : 'text-gray-400 dark:text-gray-500'
                    "
                  >
                    {{ entry.billable ? (entry.billableAmount | number: '1.2-2') + ' MAD' : '—' }}
                  </td>
                  <td class="px-4 py-3 text-center">
                    @if (!entry.billable) {
                      <span
                        class="inline-flex px-1.5 py-0.5 text-xs rounded-full bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400"
                        >Non fact.</span
                      >
                    } @else if (entry.billed) {
                      <span
                        class="inline-flex px-1.5 py-0.5 text-xs rounded-full bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-300"
                        >Facturé</span
                      >
                    } @else {
                      <span
                        class="inline-flex px-1.5 py-0.5 text-xs rounded-full bg-amber-100 dark:bg-amber-900/40 text-amber-700 dark:text-amber-300"
                        >En attente</span
                      >
                    }
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap">
                    <div class="flex items-center justify-end gap-2">
                      @if (
                        authService.hasPermission('TIME_MANAGE') && entry.billable && !entry.billed
                      ) {
                        <button
                          (click)="bill(entry.id)"
                          class="text-xs text-green-600 hover:text-green-800 dark:hover:text-green-400"
                        >
                          Facturer
                        </button>
                      }
                      @if (authService.hasPermission('TIME_UPDATE') && !entry.billed) {
                        <button
                          (click)="startEdit(entry)"
                          class="text-xs text-blue-500 hover:text-blue-700 dark:hover:text-blue-400"
                        >
                          Modifier
                        </button>
                      }
                      @if (authService.hasPermission('TIME_DELETE') && !entry.billed) {
                        <button
                          (click)="deleteEntry(entry.id)"
                          class="text-xs text-red-500 hover:text-red-700 dark:hover:text-red-400"
                        >
                          Supprimer
                        </button>
                      }
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
})
export class TimeTabComponent implements OnInit {
  @Input({ required: true }) caseId!: number;

  private timeService = inject(TimeEntryService);
  private lawyerService = inject(LawyerService);
  authService = inject(AuthService);

  entries = signal<TimeEntryResponse[]>([]);
  summary = signal<TimeEntrySummaryResponse | null>(null);
  lawyers = signal<LawyerResponse[]>([]);
  loading = signal(false);
  showForm = signal(false);
  editingEntry = signal<TimeEntryResponse | null>(null);

  form = this.emptyForm();

  ngOnInit(): void {
    this.load();
    this.lawyerService.searchLawyers({ page: 0, size: 100 }).subscribe({
      next: (page) => this.lawyers.set(page.content),
      error: () => {},
    });
  }

  load(): void {
    this.loading.set(true);
    this.timeService.getByCase(this.caseId).subscribe({
      next: (data) => {
        this.entries.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.timeService.getSummary(this.caseId).subscribe({
      next: (s) => this.summary.set(s),
      error: () => {},
    });
  }

  startEdit(entry: TimeEntryResponse): void {
    this.editingEntry.set(entry);
    this.form = {
      lawyerId: entry.lawyerId,
      entryDate: entry.entryDate,
      hours: entry.hours,
      description: entry.description,
      hourlyRate: entry.hourlyRate,
      billable: entry.billable,
    };
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingEntry.set(null);
    this.form = this.emptyForm();
  }

  isFormValid(): boolean {
    return (
      this.form.lawyerId != null &&
      !!this.form.entryDate &&
      this.form.hours > 0 &&
      !!this.form.description.trim()
    );
  }

  submitForm(): void {
    if (!this.isFormValid()) return;
    const request: TimeEntryRequest = { ...this.form, lawyerId: this.form.lawyerId! };
    const editing = this.editingEntry();
    const op = editing
      ? this.timeService.update(editing.id, request)
      : this.timeService.create(this.caseId, request);

    op.subscribe({
      next: () => {
        this.cancelForm();
        this.load();
      },
    });
  }

  deleteEntry(id: number): void {
    if (!confirm('Supprimer cette entrée ?')) return;
    this.timeService.delete(id).subscribe({ next: () => this.load() });
  }

  bill(id: number): void {
    if (!confirm('Marquer comme facturé ?')) return;
    this.timeService.markAsBilled(id).subscribe({ next: () => this.load() });
  }

  private emptyForm(): {
    lawyerId: number | null;
    entryDate: string;
    hours: number;
    description: string;
    hourlyRate: number;
    billable: boolean;
  } {
    return {
      lawyerId: null,
      entryDate: new Date().toISOString().substring(0, 10),
      hours: 1,
      description: '',
      hourlyRate: 0,
      billable: true,
    };
  }
}
