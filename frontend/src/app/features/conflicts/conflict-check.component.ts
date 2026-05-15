import { Component, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ConflictService } from '../../services/conflict.service';
import { AuthService } from '../../core/services/auth.service';
import {
  ConflictCheckResultResponse,
  ConflictCheckResponse,
  ConflictMatchResponse,
} from '../../core/models/conflict.model';

@Component({
  selector: 'app-conflict-check',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule, RouterLink],
  template: `
    <div class="p-6 max-w-4xl mx-auto space-y-6">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Vérification des conflits</h1>

      <!-- Search form -->
      <div
        class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-6"
      >
        <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">
          Entrez le nom d'un client, d'une partie adverse ou d'une organisation pour vérifier s'il
          existe des conflits d'intérêts avec les dossiers existants.
        </p>
        <div class="flex gap-3">
          <input
            [(ngModel)]="searchName"
            placeholder="Nom à vérifier..."
            (keydown.enter)="search()"
            class="flex-1 px-4 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
          />
          <button
            (click)="search()"
            [disabled]="!searchName.trim() || searching()"
            class="px-5 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-lg transition-colors"
          >
            {{ searching() ? 'Recherche...' : 'Vérifier' }}
          </button>
        </div>
      </div>

      <!-- Result -->
      @if (result()) {
        <div
          class="rounded-xl border-2 p-6 space-y-4"
          [class]="
            result()!.hasConflict
              ? 'border-red-300 bg-red-50 dark:bg-red-900/20 dark:border-red-700'
              : 'border-green-300 bg-green-50 dark:bg-green-900/20 dark:border-green-700'
          "
        >
          <div class="flex items-center gap-3">
            <span
              class="text-2xl font-bold"
              [class]="
                result()!.hasConflict
                  ? 'text-red-600 dark:text-red-400'
                  : 'text-green-600 dark:text-green-400'
              "
            >
              {{ result()!.hasConflict ? '⚠ Conflit détecté' : '✓ Aucun conflit' }}
            </span>
            <span class="text-sm text-gray-500 dark:text-gray-400">
              pour « {{ result()!.searchName }} »
            </span>
          </div>

          @if (result()!.hasConflict) {
            <div class="space-y-2">
              @for (match of result()!.matches; track $index) {
                <div
                  class="flex items-start gap-3 p-3 bg-white dark:bg-gray-800 rounded-lg border border-red-200 dark:border-red-800"
                >
                  <span
                    class="inline-flex px-2 py-0.5 text-xs rounded-full shrink-0 mt-0.5"
                    [class]="sourceClass(match)"
                  >
                    {{ sourceLabel(match.source) }}
                  </span>
                  <div class="min-w-0">
                    <p class="text-sm font-medium text-gray-900 dark:text-gray-100">
                      {{ match.matchedName }}
                    </p>
                    @if (match.partyType) {
                      <p class="text-xs text-gray-500 dark:text-gray-400">
                        {{ partyTypeLabel(match.partyType) }}
                      </p>
                    }
                    @if (match.caseNumber) {
                      <a
                        [routerLink]="['/cases', match.caseId]"
                        class="text-xs text-blue-600 dark:text-blue-400 hover:underline"
                      >
                        {{ match.caseNumber }}
                      </a>
                    }
                  </div>
                </div>
              }
            </div>

            @if (authService.hasPermission('CONFLICT_MANAGE')) {
              <div class="pt-2 border-t border-red-200 dark:border-red-700">
                <p class="text-xs font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Marquer comme résolu :
                </p>
                <div class="flex gap-2">
                  <input
                    [(ngModel)]="clearNote"
                    placeholder="Note de résolution..."
                    class="flex-1 px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-green-500"
                  />
                  <button
                    (click)="clearConflict()"
                    [disabled]="!clearNote.trim()"
                    class="px-4 py-1.5 text-sm font-medium text-white bg-green-600 hover:bg-green-700 disabled:opacity-50 rounded-lg transition-colors"
                  >
                    Résoudre
                  </button>
                </div>
              </div>
            }
          }
        </div>
      }

      <!-- History -->
      @if (authService.hasPermission('CONFLICT_READ')) {
        <div
          class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700"
        >
          <div
            class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700"
          >
            <h2 class="text-sm font-semibold text-gray-900 dark:text-white">
              Historique des vérifications
            </h2>
            <button
              (click)="loadHistory()"
              class="text-xs text-blue-600 dark:text-blue-400 hover:underline"
            >
              Rafraîchir
            </button>
          </div>

          @if (historyLoading()) {
            <div class="text-sm text-gray-400 dark:text-gray-500 text-center py-6">
              Chargement...
            </div>
          } @else if (history().length === 0) {
            <div class="text-sm text-gray-400 dark:text-gray-500 text-center py-6">
              Aucune vérification effectuée.
            </div>
          } @else {
            <div class="divide-y divide-gray-100 dark:divide-gray-700">
              @for (check of history(); track check.id) {
                <div class="flex items-center gap-4 px-6 py-3">
                  <span
                    class="inline-flex px-2 py-0.5 text-xs rounded-full shrink-0"
                    [class]="
                      check.result === 'CONFLICT_FOUND'
                        ? 'bg-red-100 dark:bg-red-900/40 text-red-700 dark:text-red-300'
                        : 'bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-300'
                    "
                  >
                    {{ check.result === 'CONFLICT_FOUND' ? 'Conflit' : 'Aucun conflit' }}
                  </span>
                  <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                      {{ check.searchName }}
                    </p>
                    @if (check.clearedNote) {
                      <p class="text-xs text-green-600 dark:text-green-400 truncate">
                        ✓ {{ check.clearedNote }}
                      </p>
                    }
                  </div>
                  <div class="text-right shrink-0">
                    <p class="text-xs text-gray-400 dark:text-gray-500">
                      {{ check.performedByUsername }}
                    </p>
                    <p class="text-xs text-gray-400 dark:text-gray-500">
                      {{ check.createdAt | date: 'dd/MM/yy HH:mm' }}
                    </p>
                  </div>
                </div>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class ConflictCheckComponent {
  private conflictService = inject(ConflictService);
  authService = inject(AuthService);

  searchName = '';
  clearNote = '';
  searching = signal(false);
  result = signal<ConflictCheckResultResponse | null>(null);
  history = signal<ConflictCheckResponse[]>([]);
  historyLoading = signal(false);

  constructor() {
    this.loadHistory();
  }

  search(): void {
    if (!this.searchName.trim()) return;
    this.searching.set(true);
    this.result.set(null);
    this.conflictService.performCheck({ searchName: this.searchName.trim() }).subscribe({
      next: (res) => {
        this.result.set(res);
        this.searching.set(false);
        this.clearNote = '';
        this.loadHistory();
      },
      error: () => this.searching.set(false),
    });
  }

  clearConflict(): void {
    const res = this.result();
    if (!res || !this.clearNote.trim()) return;
    this.conflictService.clearCheck(res.checkId, { clearedNote: this.clearNote.trim() }).subscribe({
      next: () => {
        this.result.set(null);
        this.clearNote = '';
        this.loadHistory();
      },
    });
  }

  loadHistory(): void {
    if (!this.authService.hasPermission('CONFLICT_READ')) return;
    this.historyLoading.set(true);
    this.conflictService.getHistory().subscribe({
      next: (page) => {
        this.history.set(page.content);
        this.historyLoading.set(false);
      },
      error: () => this.historyLoading.set(false),
    });
  }

  sourceClass(match: ConflictMatchResponse): string {
    const map: Record<string, string> = {
      CLIENT: 'bg-blue-100 dark:bg-blue-900/40 text-blue-800 dark:text-blue-200',
      CASE_PARTY: 'bg-orange-100 dark:bg-orange-900/40 text-orange-800 dark:text-orange-200',
      CASE: 'bg-purple-100 dark:bg-purple-900/40 text-purple-800 dark:text-purple-200',
    };
    return map[match.source] ?? '';
  }

  sourceLabel(source: string): string {
    const map: Record<string, string> = {
      CLIENT: 'Client',
      CASE_PARTY: 'Partie',
      CASE: 'Dossier',
    };
    return map[source] ?? source;
  }

  partyTypeLabel(type: string): string {
    const map: Record<string, string> = {
      CLIENT: 'Client',
      OPPOSING_PARTY: 'Partie adverse',
      OPPOSING_COUNSEL: 'Avocat adverse',
      WITNESS: 'Témoin',
      RELATED_ENTITY: 'Entité liée',
    };
    return map[type] ?? type;
  }
}
