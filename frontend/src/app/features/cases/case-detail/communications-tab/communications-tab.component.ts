import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommunicationService } from '../../../../services/communication.service';
import { AuthService } from '../../../../core/services/auth.service';
import {
  COMM_TYPE_LABELS,
  CommunicationDirection,
  CommunicationResponse,
  CommunicationType,
} from '../../../../core/models/communication.model';

@Component({
  selector: 'app-communications-tab',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule],
  template: `
    <div class="space-y-4">
      <!-- Toolbar -->
      @if (authService.hasPermission('COMMUNICATION_CREATE')) {
        <div class="flex gap-2 flex-wrap">
          @if (!showForm()) {
            <button
              (click)="openLog()"
              class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors"
            >
              + Enregistrer
            </button>
            <button
              (click)="openEmail()"
              class="px-4 py-2 text-sm font-medium text-white bg-green-600 hover:bg-green-700 rounded-lg transition-colors"
            >
              ✉ Envoyer un email
            </button>
          }
        </div>
      }

      <!-- Log form -->
      @if (showForm() === 'log') {
        <div
          class="bg-gray-50 dark:bg-gray-900/50 rounded-lg border border-gray-200 dark:border-gray-700 p-4 space-y-3"
        >
          <h4 class="text-sm font-medium text-gray-700 dark:text-gray-300">Nouvelle entrée</h4>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Type *</label>
              <select
                [(ngModel)]="logForm.commType"
                class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500"
              >
                @for (t of commTypes; track t.value) {
                  <option [value]="t.value">{{ t.label }}</option>
                }
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Direction *</label>
              <select
                [(ngModel)]="logForm.direction"
                class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="INBOUND">Entrant</option>
                <option value="OUTBOUND">Sortant</option>
                <option value="INTERNAL">Interne</option>
              </select>
            </div>
            @if (logForm.commType === 'EMAIL_SENT' || logForm.commType === 'EMAIL_RECEIVED') {
              <div>
                <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Email</label>
                <input
                  type="email"
                  [(ngModel)]="logForm.recipientEmail"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            }
            @if (logForm.commType === 'CALL' || logForm.commType === 'SMS') {
              <div>
                <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Téléphone</label>
                <input
                  type="tel"
                  [(ngModel)]="logForm.recipientPhone"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            }
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Sujet</label>
            <input
              type="text"
              [(ngModel)]="logForm.subject"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Contenu / Notes</label>
            <textarea
              [(ngModel)]="logForm.body"
              rows="3"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500 resize-none"
            ></textarea>
          </div>
          <div class="flex gap-2 pt-1">
            <button
              (click)="cancelForm()"
              class="px-4 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-800"
            >
              Annuler
            </button>
            <button
              (click)="submitLog()"
              [disabled]="saving()"
              class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-lg transition-colors"
            >
              {{ saving() ? 'Enregistrement...' : 'Enregistrer' }}
            </button>
          </div>
        </div>
      }

      <!-- Send email form -->
      @if (showForm() === 'email') {
        <div
          class="bg-green-50 dark:bg-green-900/20 rounded-lg border border-green-200 dark:border-green-800 p-4 space-y-3"
        >
          <h4 class="text-sm font-medium text-gray-700 dark:text-gray-300">Envoyer un email</h4>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Destinataire *</label>
            <input
              type="email"
              [(ngModel)]="emailForm.to"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Objet *</label>
            <input
              type="text"
              [(ngModel)]="emailForm.subject"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-green-500"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Message *</label>
            <textarea
              [(ngModel)]="emailForm.body"
              rows="4"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-green-500 resize-none"
            ></textarea>
          </div>
          <div class="flex gap-2 pt-1">
            <button
              (click)="cancelForm()"
              class="px-4 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-800"
            >
              Annuler
            </button>
            <button
              (click)="submitEmail()"
              [disabled]="saving() || !emailForm.to || !emailForm.subject || !emailForm.body"
              class="px-4 py-2 text-sm font-medium text-white bg-green-600 hover:bg-green-700 disabled:opacity-50 rounded-lg transition-colors"
            >
              {{ saving() ? 'Envoi...' : 'Envoyer & enregistrer' }}
            </button>
          </div>
        </div>
      }

      <!-- Timeline -->
      @if (loading()) {
        <div class="text-sm text-gray-400 dark:text-gray-500 text-center py-6">Chargement...</div>
      } @else if (entries().length === 0) {
        <div class="text-sm text-gray-400 dark:text-gray-500 text-center py-6">
          Aucune communication pour ce dossier.
        </div>
      } @else {
        <div class="space-y-3">
          @for (entry of entries(); track entry.id) {
            <div
              class="flex gap-3 bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 p-4"
            >
              <div class="flex-shrink-0 mt-0.5">
                <span [class]="typeIconClass(entry.commType)">{{ typeIcon(entry.commType) }}</span>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between gap-2 flex-wrap">
                  <div class="flex items-center gap-2">
                    <span [class]="typeBadge(entry.commType)">
                      {{ typeLabel(entry.commType) }}
                    </span>
                    <span [class]="directionBadge(entry.direction)">
                      {{ directionLabel(entry.direction) }}
                    </span>
                    @if (entry.recipientEmail) {
                      <span class="text-xs text-gray-500 dark:text-gray-400">→ {{ entry.recipientEmail }}</span>
                    }
                    @if (entry.recipientPhone) {
                      <span class="text-xs text-gray-500 dark:text-gray-400">→ {{ entry.recipientPhone }}</span>
                    }
                  </div>
                  <div class="flex items-center gap-3 text-xs text-gray-400 dark:text-gray-500">
                    <span>{{ entry.sentByUsername }}</span>
                    <span>{{ entry.createdAt | date: 'dd/MM/yyyy HH:mm' }}</span>
                    @if (authService.hasPermission('COMMUNICATION_DELETE')) {
                      <button
                        (click)="deleteEntry(entry.id)"
                        class="text-red-400 hover:text-red-600 dark:hover:text-red-400"
                      >
                        ✕
                      </button>
                    }
                  </div>
                </div>
                @if (entry.subject) {
                  <p class="mt-1 text-sm font-medium text-gray-800 dark:text-gray-200">{{ entry.subject }}</p>
                }
                @if (entry.body) {
                  <p class="mt-1 text-sm text-gray-600 dark:text-gray-400 whitespace-pre-wrap">{{ entry.body }}</p>
                }
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class CommunicationsTabComponent implements OnInit {
  @Input({ required: true }) caseId!: number;

  private commService = inject(CommunicationService);
  authService = inject(AuthService);

  entries = signal<CommunicationResponse[]>([]);
  loading = signal(false);
  saving = signal(false);
  showForm = signal<'log' | 'email' | null>(null);

  logForm: {
    commType: CommunicationType;
    direction: CommunicationDirection;
    subject: string;
    body: string;
    recipientEmail: string;
    recipientPhone: string;
  } = { commType: 'NOTE', direction: 'INTERNAL', subject: '', body: '', recipientEmail: '', recipientPhone: '' };

  emailForm = { to: '', subject: '', body: '' };

  readonly commTypes = (Object.keys(COMM_TYPE_LABELS) as CommunicationType[]).map((v) => ({
    value: v,
    label: COMM_TYPE_LABELS[v],
  }));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.commService.getByCase(this.caseId).subscribe({
      next: (e) => { this.entries.set(e); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  openLog(): void { this.showForm.set('log'); }
  openEmail(): void { this.showForm.set('email'); }
  cancelForm(): void { this.showForm.set(null); }

  submitLog(): void {
    this.saving.set(true);
    this.commService.log(this.caseId, {
      commType: this.logForm.commType,
      direction: this.logForm.direction,
      subject: this.logForm.subject || undefined,
      body: this.logForm.body || undefined,
      recipientEmail: this.logForm.recipientEmail || undefined,
      recipientPhone: this.logForm.recipientPhone || undefined,
    }).subscribe({
      next: () => { this.cancelForm(); this.load(); this.saving.set(false); },
      error: () => this.saving.set(false),
    });
  }

  submitEmail(): void {
    this.saving.set(true);
    this.commService.sendEmail(this.caseId, this.emailForm).subscribe({
      next: () => { this.cancelForm(); this.load(); this.saving.set(false); },
      error: () => this.saving.set(false),
    });
  }

  deleteEntry(id: number): void {
    if (!confirm('Supprimer cette entrée ?')) return;
    this.commService.delete(id).subscribe({ next: () => this.load() });
  }

  typeLabel(t: CommunicationType): string { return COMM_TYPE_LABELS[t] ?? t; }

  typeIcon(t: CommunicationType): string {
    const map: Record<CommunicationType, string> = {
      NOTE: '📝', EMAIL_SENT: '📤', EMAIL_RECEIVED: '📥', CALL: '📞', SMS: '💬',
    };
    return map[t] ?? '•';
  }

  typeIconClass(_t: CommunicationType): string {
    return 'flex items-center justify-center w-8 h-8 rounded-full bg-gray-100 dark:bg-gray-700 text-base';
  }

  typeBadge(t: CommunicationType): string {
    const base = 'inline-flex px-1.5 py-0.5 text-xs rounded-full ';
    const map: Record<CommunicationType, string> = {
      NOTE: base + 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300',
      EMAIL_SENT: base + 'bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-300',
      EMAIL_RECEIVED: base + 'bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300',
      CALL: base + 'bg-purple-100 dark:bg-purple-900/40 text-purple-700 dark:text-purple-300',
      SMS: base + 'bg-amber-100 dark:bg-amber-900/40 text-amber-700 dark:text-amber-300',
    };
    return map[t] ?? base + 'bg-gray-100 text-gray-600';
  }

  directionBadge(d: CommunicationDirection): string {
    const base = 'inline-flex px-1.5 py-0.5 text-xs rounded ';
    if (d === 'INBOUND') return base + 'bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400';
    if (d === 'OUTBOUND') return base + 'bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400';
    return base + 'bg-gray-50 dark:bg-gray-700 text-gray-500 dark:text-gray-400';
  }

  directionLabel(d: CommunicationDirection): string {
    return d === 'INBOUND' ? 'Entrant' : d === 'OUTBOUND' ? 'Sortant' : 'Interne';
  }
}
