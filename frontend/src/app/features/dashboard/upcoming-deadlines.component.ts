import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TaskService } from '../../services/task.service';
import { UpcomingTaskResponse, TaskPriority } from '../../core/models/task.model';

@Component({
  selector: 'app-upcoming-deadlines',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink],
  template: `
    <div class="bg-white dark:bg-gray-800 shadow-md rounded-lg p-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-base font-semibold text-gray-900 dark:text-white">
          Échéances à venir (7j)
        </h3>
        <span class="text-xs text-gray-400 dark:text-gray-500">{{ tasks().length }} tâche(s)</span>
      </div>

      @if (loading()) {
        <div class="text-sm text-gray-400 dark:text-gray-500 text-center py-4">Chargement...</div>
      } @else if (tasks().length === 0) {
        <div class="text-sm text-gray-400 dark:text-gray-500 text-center py-4">
          Aucune échéance dans les 7 prochains jours.
        </div>
      } @else {
        <ul class="divide-y divide-gray-100 dark:divide-gray-700">
          @for (task of tasks(); track task.id) {
            <li class="py-3">
              <a
                [routerLink]="['/cases', task.caseId]"
                [queryParams]="{ tab: 'tasks' }"
                class="flex items-start justify-between gap-2 group"
              >
                <div class="min-w-0">
                  <p
                    class="text-sm font-medium text-gray-900 dark:text-gray-100 truncate group-hover:text-indigo-600 dark:group-hover:text-indigo-400"
                  >
                    {{ task.title }}
                  </p>
                  <p class="text-xs text-gray-400 dark:text-gray-500 mt-0.5">
                    {{ task.caseNumber }}
                    @if (task.assignedLawyerName) {
                      · {{ task.assignedLawyerName }}
                    }
                  </p>
                </div>
                <div class="flex flex-col items-end gap-1 shrink-0">
                  <span
                    class="text-xs font-medium whitespace-nowrap"
                    [class]="dueDateClass(task.dueDate)"
                  >
                    {{ task.dueDate | date: 'dd/MM' }}
                  </span>
                  <span
                    class="inline-flex px-1.5 py-0.5 text-xs rounded-full"
                    [class]="priorityBadge(task.priority)"
                  >
                    {{ priorityLabel(task.priority) }}
                  </span>
                </div>
              </a>
            </li>
          }
        </ul>
      }
    </div>
  `,
})
export class UpcomingDeadlinesComponent implements OnInit {
  private taskService = inject(TaskService);

  tasks = signal<UpcomingTaskResponse[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    this.taskService.getUpcoming(7).subscribe({
      next: (data) => {
        this.tasks.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  dueDateClass(dueDate: string): string {
    const days = Math.ceil((new Date(dueDate).getTime() - Date.now()) / 86400000);
    if (days <= 1) return 'text-red-600 dark:text-red-400';
    if (days <= 3) return 'text-orange-500 dark:text-orange-400';
    return 'text-gray-600 dark:text-gray-400';
  }

  priorityBadge(priority: TaskPriority): string {
    const map: Record<TaskPriority, string> = {
      LOW: 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300',
      MEDIUM: 'bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-200',
      HIGH: 'bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-300',
    };
    return map[priority] ?? '';
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
