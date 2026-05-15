import { Component, Input, Output, EventEmitter, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LawyerService } from '../../../../services/lawyer.service';
import {
  TaskResponse,
  TaskRequest,
  TaskStatus,
  TaskPriority,
} from '../../../../core/models/task.model';
import { LawyerResponse } from '../../../../core/models/lawyer.model';

@Component({
  selector: 'app-task-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div
        class="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-lg mx-4 p-6 space-y-4"
      >
        <h3 class="text-base font-semibold text-gray-900 dark:text-gray-100">
          {{ task ? 'Modifier la tâche' : 'Nouvelle tâche' }}
        </h3>

        <!-- Title -->
        <div>
          <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
            >Titre *</label
          >
          <input
            [(ngModel)]="title"
            maxlength="255"
            required
            class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
          />
        </div>

        <!-- Description -->
        <div>
          <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
            >Description</label
          >
          <textarea
            [(ngModel)]="description"
            rows="2"
            class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none resize-none"
          ></textarea>
        </div>

        <!-- Due date + Priority -->
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
              >Échéance *</label
            >
            <input
              type="date"
              [(ngModel)]="dueDate"
              required
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
              >Priorité *</label
            >
            <select
              [(ngModel)]="priority"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
            >
              <option value="LOW">Basse</option>
              <option value="MEDIUM">Moyenne</option>
              <option value="HIGH">Haute</option>
            </select>
          </div>
        </div>

        <!-- Status + Assignee -->
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
              >Statut *</label
            >
            <select
              [(ngModel)]="status"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
            >
              <option value="TODO">À faire</option>
              <option value="IN_PROGRESS">En cours</option>
              <option value="DONE">Terminée</option>
              <option value="CANCELLED">Annulée</option>
            </select>
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
              >Assigné à</label
            >
            <select
              [(ngModel)]="assignedLawyerId"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
            >
              <option [ngValue]="null">— Aucun —</option>
              @for (lawyer of lawyers(); track lawyer.id) {
                <option [ngValue]="lawyer.id">{{ lawyer.firstName }} {{ lawyer.lastName }}</option>
              }
            </select>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex justify-end gap-3 pt-2">
          <button
            (click)="cancel.emit()"
            class="px-4 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200"
          >
            Annuler
          </button>
          <button
            (click)="submit()"
            [disabled]="!isValid()"
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-lg transition-colors"
          >
            {{ task ? 'Enregistrer' : 'Créer' }}
          </button>
        </div>
      </div>
    </div>
  `,
})
export class TaskFormComponent implements OnInit {
  @Input() task: TaskResponse | null = null;
  @Output() save = new EventEmitter<TaskRequest>();
  @Output() cancel = new EventEmitter<void>();

  private lawyerService = inject(LawyerService);

  lawyers = signal<LawyerResponse[]>([]);

  title = '';
  description = '';
  dueDate = '';
  status: TaskStatus = 'TODO';
  priority: TaskPriority = 'MEDIUM';
  assignedLawyerId: number | null = null;

  ngOnInit(): void {
    if (this.task) {
      this.title = this.task.title;
      this.description = this.task.description ?? '';
      this.dueDate = this.task.dueDate;
      this.status = this.task.status;
      this.priority = this.task.priority;
      this.assignedLawyerId = this.task.assignedLawyerId ?? null;
    }
    this.lawyerService.searchLawyers({ page: 0, size: 100 }).subscribe({
      next: (page) => this.lawyers.set(page.content),
      error: () => {},
    });
  }

  isValid(): boolean {
    return this.title.trim().length > 0 && this.dueDate.length > 0;
  }

  submit(): void {
    if (!this.isValid()) return;
    this.save.emit({
      title: this.title.trim(),
      description: this.description.trim() || undefined,
      dueDate: this.dueDate,
      status: this.status,
      priority: this.priority,
      assignedLawyerId: this.assignedLawyerId ?? undefined,
    });
  }
}
