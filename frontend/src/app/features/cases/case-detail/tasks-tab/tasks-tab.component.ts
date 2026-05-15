import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { TaskService } from '../../../../services/task.service';
import { AuthService } from '../../../../core/services/auth.service';
import {
  TaskResponse,
  TaskRequest,
  TaskStatus,
  TaskPriority,
} from '../../../../core/models/task.model';
import { TaskFormComponent } from './task-form.component';

@Component({
  selector: 'app-tasks-tab',
  standalone: true,
  imports: [CommonModule, DatePipe, TaskFormComponent],
  templateUrl: './tasks-tab.component.html',
})
export class TasksTabComponent implements OnInit {
  @Input({ required: true }) caseId!: number;

  private taskService = inject(TaskService);
  authService = inject(AuthService);

  tasks = signal<TaskResponse[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  showForm = signal(false);
  editingTask = signal<TaskResponse | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.taskService.getTasksByCase(this.caseId).subscribe({
      next: (data) => {
        this.tasks.set(data);
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.error.set(err.error?.message ?? 'Erreur de chargement');
        this.loading.set(false);
      },
    });
  }

  openCreate(): void {
    this.editingTask.set(null);
    this.showForm.set(true);
  }

  openEdit(task: TaskResponse): void {
    this.editingTask.set(task);
    this.showForm.set(true);
  }

  onFormSave(request: TaskRequest): void {
    const editing = this.editingTask();
    if (editing) {
      this.taskService.update(editing.id, request).subscribe({
        next: () => {
          this.showForm.set(false);
          this.load();
        },
        error: (err: { error?: { message?: string } }) =>
          this.error.set(err.error?.message ?? 'Erreur de mise à jour'),
      });
    } else {
      this.taskService.create(this.caseId, request).subscribe({
        next: () => {
          this.showForm.set(false);
          this.load();
        },
        error: (err: { error?: { message?: string } }) =>
          this.error.set(err.error?.message ?? 'Erreur de création'),
      });
    }
  }

  onFormCancel(): void {
    this.showForm.set(false);
    this.editingTask.set(null);
  }

  deleteTask(id: number): void {
    if (!confirm('Supprimer cette tâche ?')) return;
    this.taskService.delete(id).subscribe({
      next: () => this.load(),
      error: (err: { error?: { message?: string } }) =>
        this.error.set(err.error?.message ?? 'Erreur de suppression'),
    });
  }

  statusBadge(status: TaskStatus): string {
    const map: Record<TaskStatus, string> = {
      TODO: 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300',
      IN_PROGRESS: 'bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200',
      DONE: 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200',
      CANCELLED: 'bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-300',
    };
    return map[status] ?? '';
  }

  priorityBadge(priority: TaskPriority): string {
    const map: Record<TaskPriority, string> = {
      LOW: 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300',
      MEDIUM: 'bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-200',
      HIGH: 'bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-300',
    };
    return map[priority] ?? '';
  }

  statusLabel(status: TaskStatus): string {
    const map: Record<TaskStatus, string> = {
      TODO: 'À faire',
      IN_PROGRESS: 'En cours',
      DONE: 'Terminée',
      CANCELLED: 'Annulée',
    };
    return map[status] ?? status;
  }

  priorityLabel(priority: TaskPriority): string {
    const map: Record<TaskPriority, string> = {
      LOW: 'Basse',
      MEDIUM: 'Moyenne',
      HIGH: 'Haute',
    };
    return map[priority] ?? priority;
  }
}
