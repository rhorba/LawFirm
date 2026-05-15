import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConflictService } from '../../../../services/conflict.service';
import { AuthService } from '../../../../core/services/auth.service';
import {
  ConflictPartyResponse,
  ConflictPartyRequest,
  PartyType,
} from '../../../../core/models/conflict.model';

@Component({
  selector: 'app-parties-tab',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule],
  template: `
    <div class="space-y-4">
      <!-- Add party form (CONFLICT_CREATE) -->
      @if (authService.hasPermission('CONFLICT_CREATE')) {
        <div
          class="bg-gray-50 dark:bg-gray-900/50 rounded-lg border border-gray-200 dark:border-gray-700 p-4"
        >
          <h4 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
            Ajouter une partie
          </h4>
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <select
              [(ngModel)]="newPartyType"
              class="px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
            >
              <option value="OPPOSING_PARTY">Partie adverse</option>
              <option value="OPPOSING_COUNSEL">Avocat adverse</option>
              <option value="WITNESS">Témoin</option>
              <option value="RELATED_ENTITY">Entité liée</option>
              <option value="CLIENT">Client associé</option>
            </select>
            <input
              [(ngModel)]="newPartyName"
              placeholder="Nom *"
              class="px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none sm:col-span-1"
            />
            <div class="flex gap-2">
              <input
                [(ngModel)]="newPartyNotes"
                placeholder="Notes (optionnel)"
                class="flex-1 px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
              />
              <button
                (click)="addParty()"
                [disabled]="!newPartyName.trim()"
                class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-lg transition-colors shrink-0"
              >
                Ajouter
              </button>
            </div>
          </div>
        </div>
      }

      <!-- Parties list -->
      @if (loading()) {
        <div class="text-sm text-gray-400 dark:text-gray-500 text-center py-6">Chargement...</div>
      } @else if (parties().length === 0) {
        <div class="text-sm text-gray-400 dark:text-gray-500 text-center py-6">
          Aucune partie enregistrée pour ce dossier.
        </div>
      } @else {
        <div class="overflow-x-auto rounded-lg border border-gray-200 dark:border-gray-700">
          <table class="w-full text-sm">
            <thead
              class="bg-gray-50 dark:bg-gray-900/50 text-xs text-gray-500 dark:text-gray-400 uppercase tracking-wide"
            >
              <tr>
                <th class="px-4 py-3 text-left">Type</th>
                <th class="px-4 py-3 text-left">Nom</th>
                <th class="px-4 py-3 text-left">Notes</th>
                <th class="px-4 py-3 text-left">Ajouté le</th>
                @if (authService.hasPermission('CONFLICT_MANAGE')) {
                  <th class="px-4 py-3"></th>
                }
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
              @for (party of parties(); track party.id) {
                <tr class="bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700/50">
                  <td class="px-4 py-3">
                    <span
                      class="inline-flex px-2 py-0.5 text-xs rounded-full"
                      [class]="partyTypeClass(party.partyType)"
                    >
                      {{ partyTypeLabel(party.partyType) }}
                    </span>
                  </td>
                  <td class="px-4 py-3 font-medium text-gray-900 dark:text-gray-100">
                    {{ party.name }}
                  </td>
                  <td class="px-4 py-3 text-gray-500 dark:text-gray-400">
                    {{ party.notes ?? '—' }}
                  </td>
                  <td class="px-4 py-3 text-gray-400 dark:text-gray-500">
                    {{ party.createdAt | date: 'dd/MM/yyyy' }}
                  </td>
                  @if (authService.hasPermission('CONFLICT_MANAGE')) {
                    <td class="px-4 py-3 text-right">
                      <button
                        (click)="deleteParty(party.id)"
                        class="text-xs text-red-500 hover:text-red-700 dark:hover:text-red-400"
                      >
                        Supprimer
                      </button>
                    </td>
                  }
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
})
export class PartiesTabComponent implements OnInit {
  @Input({ required: true }) caseId!: number;

  private conflictService = inject(ConflictService);
  authService = inject(AuthService);

  parties = signal<ConflictPartyResponse[]>([]);
  loading = signal(false);

  newPartyType: PartyType = 'OPPOSING_PARTY';
  newPartyName = '';
  newPartyNotes = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.conflictService.getPartiesByCase(this.caseId).subscribe({
      next: (data) => {
        this.parties.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  addParty(): void {
    if (!this.newPartyName.trim()) return;
    const request: ConflictPartyRequest = {
      partyType: this.newPartyType,
      name: this.newPartyName.trim(),
      notes: this.newPartyNotes.trim() || undefined,
    };
    this.conflictService.addParty(this.caseId, request).subscribe({
      next: () => {
        this.newPartyName = '';
        this.newPartyNotes = '';
        this.load();
      },
    });
  }

  deleteParty(id: number): void {
    if (!confirm('Supprimer cette partie ?')) return;
    this.conflictService.deleteParty(id).subscribe({ next: () => this.load() });
  }

  partyTypeClass(type: PartyType): string {
    const map: Record<PartyType, string> = {
      CLIENT: 'bg-blue-100 dark:bg-blue-900/40 text-blue-800 dark:text-blue-200',
      OPPOSING_PARTY: 'bg-red-100 dark:bg-red-900/40 text-red-800 dark:text-red-200',
      OPPOSING_COUNSEL: 'bg-orange-100 dark:bg-orange-900/40 text-orange-800 dark:text-orange-200',
      WITNESS: 'bg-yellow-100 dark:bg-yellow-900/40 text-yellow-800 dark:text-yellow-200',
      RELATED_ENTITY: 'bg-purple-100 dark:bg-purple-900/40 text-purple-800 dark:text-purple-200',
    };
    return map[type] ?? '';
  }

  partyTypeLabel(type: PartyType): string {
    const map: Record<PartyType, string> = {
      CLIENT: 'Client',
      OPPOSING_PARTY: 'Partie adverse',
      OPPOSING_COUNSEL: 'Avocat adverse',
      WITNESS: 'Témoin',
      RELATED_ENTITY: 'Entité liée',
    };
    return map[type] ?? type;
  }
}
