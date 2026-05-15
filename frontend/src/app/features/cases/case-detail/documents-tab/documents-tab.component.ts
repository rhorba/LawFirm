import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocumentService } from '../../../../services/document.service';
import { AuthService } from '../../../../core/services/auth.service';
import {
  DocumentCategory,
  DocumentResponse,
  DOCUMENT_CATEGORY_LABELS,
} from '../../../../core/models/document.model';

@Component({
  selector: 'app-documents-tab',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule],
  template: `
    <div class="space-y-4">
      <!-- Toolbar -->
      <div class="flex flex-wrap gap-2 items-center justify-between">
        <div class="flex gap-2 flex-1 min-w-0">
          <input
            type="text"
            [(ngModel)]="searchQuery"
            (ngModelChange)="onSearch()"
            placeholder="Rechercher un document..."
            class="flex-1 min-w-0 px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500"
          />
          <select
            [(ngModel)]="filterCategory"
            (ngModelChange)="onSearch()"
            class="px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Toutes catégories</option>
            @for (cat of categories; track cat.value) {
              <option [value]="cat.value">{{ cat.label }}</option>
            }
          </select>
        </div>
        @if (authService.hasPermission('DOCUMENT_CREATE')) {
          <button
            (click)="showUploadForm.set(true)"
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors whitespace-nowrap"
          >
            + Ajouter un document
          </button>
        }
      </div>

      <!-- Upload form -->
      @if (showUploadForm()) {
        <div
          class="bg-gray-50 dark:bg-gray-900/50 rounded-lg border border-gray-200 dark:border-gray-700 p-4 space-y-3"
        >
          <h4 class="text-sm font-medium text-gray-700 dark:text-gray-300">Nouveau document</h4>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
                >Titre *</label
              >
              <input
                type="text"
                [(ngModel)]="form.title"
                class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
                >Catégorie *</label
              >
              <select
                [(ngModel)]="form.category"
                class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500"
              >
                @for (cat of categories; track cat.value) {
                  <option [value]="cat.value">{{ cat.label }}</option>
                }
              </select>
            </div>
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
              >Description</label
            >
            <textarea
              [(ngModel)]="form.description"
              rows="2"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 outline-none focus:ring-2 focus:ring-blue-500 resize-none"
            ></textarea>
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
              >Fichier *</label
            >
            <input
              type="file"
              (change)="onFileSelected($event)"
              accept=".pdf,.doc,.docx,.xls,.xlsx,.jpg,.jpeg,.png,.gif,.webp,.txt"
              class="w-full text-sm text-gray-600 dark:text-gray-400 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 dark:file:bg-blue-900/30 dark:file:text-blue-300"
            />
            @if (form.file) {
              <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
                {{ form.file.name }} ({{ formatSize(form.file.size) }})
              </p>
            }
          </div>
          @if (uploadError()) {
            <p class="text-xs text-red-600 dark:text-red-400">{{ uploadError() }}</p>
          }
          <div class="flex gap-2 pt-1">
            <button
              (click)="cancelUpload()"
              class="px-4 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200"
            >
              Annuler
            </button>
            <button
              (click)="submitUpload()"
              [disabled]="!isFormValid() || uploading()"
              class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-lg transition-colors"
            >
              {{ uploading() ? 'Envoi...' : 'Téléverser' }}
            </button>
          </div>
        </div>
      }

      <!-- List -->
      @if (loading()) {
        <div class="text-sm text-gray-400 dark:text-gray-500 text-center py-6">Chargement...</div>
      } @else if (filtered().length === 0) {
        <div class="text-sm text-gray-400 dark:text-gray-500 text-center py-6">
          Aucun document pour ce dossier.
        </div>
      } @else {
        <div class="overflow-x-auto rounded-lg border border-gray-200 dark:border-gray-700">
          <table class="w-full text-sm">
            <thead
              class="bg-gray-50 dark:bg-gray-900/50 text-xs text-gray-500 dark:text-gray-400 uppercase tracking-wide"
            >
              <tr>
                <th class="px-4 py-3 text-left">Titre</th>
                <th class="px-4 py-3 text-left">Catégorie</th>
                <th class="px-4 py-3 text-left">Fichier</th>
                <th class="px-4 py-3 text-right">Taille</th>
                <th class="px-4 py-3 text-left">Téléversé par</th>
                <th class="px-4 py-3 text-left">Date</th>
                <th class="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
              @for (doc of filtered(); track doc.id) {
                <tr class="bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700/50">
                  <td class="px-4 py-3 text-gray-900 dark:text-gray-100 max-w-[180px] truncate">
                    {{ doc.title }}
                    @if (doc.description) {
                      <p class="text-xs text-gray-400 dark:text-gray-500 truncate">
                        {{ doc.description }}
                      </p>
                    }
                  </td>
                  <td class="px-4 py-3">
                    <span [class]="categoryBadge(doc.category)">
                      {{ categoryLabel(doc.category) }}
                    </span>
                  </td>
                  <td
                    class="px-4 py-3 text-gray-500 dark:text-gray-400 max-w-[160px] truncate"
                    [title]="doc.originalFilename"
                  >
                    <span [class]="typeTag(doc.contentType)">{{ typeLabel(doc.contentType) }}</span>
                    {{ doc.originalFilename }}
                  </td>
                  <td
                    class="px-4 py-3 text-right text-gray-500 dark:text-gray-400 whitespace-nowrap"
                  >
                    {{ formatSize(doc.fileSize) }}
                  </td>
                  <td class="px-4 py-3 text-gray-500 dark:text-gray-400 whitespace-nowrap">
                    {{ doc.uploadedByUsername }}
                  </td>
                  <td class="px-4 py-3 text-gray-500 dark:text-gray-400 whitespace-nowrap">
                    {{ doc.createdAt | date: 'dd/MM/yyyy' }}
                  </td>
                  <td class="px-4 py-3 text-right whitespace-nowrap">
                    <div class="flex items-center justify-end gap-2">
                      @if (isPreviewable(doc.contentType)) {
                        <a
                          [href]="docService.getPreviewUrl(doc.id)"
                          target="_blank"
                          class="text-xs text-purple-600 hover:text-purple-800 dark:hover:text-purple-400"
                        >
                          Aperçu
                        </a>
                      }
                      <a
                        [href]="docService.getDownloadUrl(doc.id)"
                        download
                        class="text-xs text-blue-600 hover:text-blue-800 dark:hover:text-blue-400"
                      >
                        Télécharger
                      </a>
                      @if (authService.hasPermission('DOCUMENT_DELETE')) {
                        <button
                          (click)="deleteDoc(doc.id)"
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
export class DocumentsTabComponent implements OnInit {
  @Input({ required: true }) caseId!: number;

  docService = inject(DocumentService);
  authService = inject(AuthService);

  documents = signal<DocumentResponse[]>([]);
  filtered = signal<DocumentResponse[]>([]);
  loading = signal(false);
  showUploadForm = signal(false);
  uploading = signal(false);
  uploadError = signal<string | null>(null);

  searchQuery = '';
  filterCategory = '';

  form: { title: string; description: string; category: DocumentCategory; file: File | null } = {
    title: '',
    description: '',
    category: 'AUTRE',
    file: null,
  };

  readonly categories = (Object.keys(DOCUMENT_CATEGORY_LABELS) as DocumentCategory[]).map((v) => ({
    value: v,
    label: DOCUMENT_CATEGORY_LABELS[v],
  }));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.docService.getByCase(this.caseId).subscribe({
      next: (docs) => {
        this.documents.set(docs);
        this.applyFilter();
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onSearch(): void {
    this.applyFilter();
  }

  private applyFilter(): void {
    let result = this.documents();
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter(
        (d) => d.title.toLowerCase().includes(q) || d.originalFilename.toLowerCase().includes(q)
      );
    }
    if (this.filterCategory) {
      result = result.filter((d) => d.category === this.filterCategory);
    }
    this.filtered.set(result);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.form.file = input.files?.[0] ?? null;
  }

  isFormValid(): boolean {
    return !!this.form.title.trim() && !!this.form.file;
  }

  cancelUpload(): void {
    this.showUploadForm.set(false);
    this.uploadError.set(null);
    this.form = { title: '', description: '', category: 'AUTRE', file: null };
  }

  submitUpload(): void {
    if (!this.isFormValid() || !this.form.file) return;
    this.uploading.set(true);
    this.uploadError.set(null);
    this.docService
      .upload(
        this.caseId,
        this.form.file,
        this.form.title,
        this.form.description,
        this.form.category
      )
      .subscribe({
        next: () => {
          this.cancelUpload();
          this.load();
          this.uploading.set(false);
        },
        error: (err: unknown) => {
          const msg =
            (err as { error?: { message?: string } })?.error?.message ??
            'Erreur lors du téléversement';
          this.uploadError.set(msg);
          this.uploading.set(false);
        },
      });
  }

  deleteDoc(id: number): void {
    if (!confirm('Supprimer ce document ?')) return;
    this.docService.delete(id).subscribe({ next: () => this.load() });
  }

  categoryLabel(cat: DocumentCategory): string {
    return DOCUMENT_CATEGORY_LABELS[cat] ?? cat;
  }

  categoryBadge(cat: DocumentCategory): string {
    const map: Record<DocumentCategory, string> = {
      CONTRAT:
        'inline-flex px-1.5 py-0.5 text-xs rounded-full bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300',
      JUGEMENT:
        'inline-flex px-1.5 py-0.5 text-xs rounded-full bg-purple-100 dark:bg-purple-900/40 text-purple-700 dark:text-purple-300',
      ASSIGNATION:
        'inline-flex px-1.5 py-0.5 text-xs rounded-full bg-red-100 dark:bg-red-900/40 text-red-700 dark:text-red-300',
      COURRIER:
        'inline-flex px-1.5 py-0.5 text-xs rounded-full bg-yellow-100 dark:bg-yellow-900/40 text-yellow-700 dark:text-yellow-300',
      REQUETE:
        'inline-flex px-1.5 py-0.5 text-xs rounded-full bg-orange-100 dark:bg-orange-900/40 text-orange-700 dark:text-orange-300',
      AUTRE:
        'inline-flex px-1.5 py-0.5 text-xs rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300',
    };
    return map[cat] ?? map['AUTRE'];
  }

  typeLabel(contentType: string): string {
    if (contentType.includes('pdf')) return 'PDF';
    if (contentType.includes('word') || contentType.includes('document')) return 'DOC';
    if (contentType.includes('excel') || contentType.includes('sheet')) return 'XLS';
    if (contentType.startsWith('image/')) return 'IMG';
    if (contentType.includes('text')) return 'TXT';
    return 'FILE';
  }

  typeTag(contentType: string): string {
    const base = 'inline-flex mr-1.5 px-1 py-0.5 text-xs font-mono rounded ';
    if (contentType.includes('pdf'))
      return base + 'bg-red-100 dark:bg-red-900/40 text-red-700 dark:text-red-300';
    if (contentType.includes('word') || contentType.includes('document'))
      return base + 'bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300';
    if (contentType.includes('excel') || contentType.includes('sheet'))
      return base + 'bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-300';
    if (contentType.startsWith('image/'))
      return base + 'bg-purple-100 dark:bg-purple-900/40 text-purple-700 dark:text-purple-300';
    return base + 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300';
  }

  isPreviewable(contentType: string): boolean {
    return contentType.includes('pdf') || contentType.startsWith('image/');
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1024 / 1024).toFixed(1) + ' MB';
  }
}
