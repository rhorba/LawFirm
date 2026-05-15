import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CalendarService } from '../../services/calendar.service';
import { AuthService } from '../../core/services/auth.service';
import {
  CalendarDayEvent,
  CalendarEventResponse,
  CalendarEventRequest,
} from '../../core/models/calendar.model';
import { CalendarEventFormComponent } from './calendar-event-form.component';

interface CalendarCell {
  date: Date;
  isCurrentMonth: boolean;
  isToday: boolean;
  events: CalendarDayEvent[];
}

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink, CalendarEventFormComponent],
  template: `
    <div class="p-6 space-y-4">
      <!-- Header -->
      <div class="flex items-center justify-between">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">Calendrier</h1>
        @if (authService.hasPermission('CALENDAR_CREATE')) {
          <button
            (click)="openCreate(null)"
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors"
          >
            + Événement
          </button>
        }
      </div>

      <!-- Month navigation -->
      <div class="flex items-center gap-4">
        <button
          (click)="prevMonth()"
          class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300"
        >
          &#8249;
        </button>
        <span class="text-lg font-semibold text-gray-900 dark:text-white min-w-[180px] text-center">
          {{ monthLabel() }}
        </span>
        <button
          (click)="nextMonth()"
          class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300"
        >
          &#8250;
        </button>
        <button
          (click)="goToday()"
          class="ml-2 px-3 py-1.5 text-xs font-medium border border-gray-300 dark:border-gray-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
        >
          Aujourd'hui
        </button>
      </div>

      @if (loading()) {
        <div class="text-sm text-gray-400 dark:text-gray-500 text-center py-12">Chargement...</div>
      } @else {
        <!-- Day-of-week headers -->
        <div class="grid grid-cols-7 border-b border-gray-200 dark:border-gray-700">
          @for (day of weekDays; track day) {
            <div
              class="py-2 text-center text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide"
            >
              {{ day }}
            </div>
          }
        </div>

        <!-- Calendar grid -->
        <div class="grid grid-cols-7 border-l border-t border-gray-200 dark:border-gray-700">
          @for (cell of cells(); track cell.date.toISOString()) {
            <div [class]="cellClass(cell)" (click)="openCreate(cell.date)">
              <div [class]="dayNumClass(cell)">
                {{ cell.date.getDate() }}
              </div>

              <div class="space-y-0.5">
                @for (event of cell.events.slice(0, 3); track event.id) {
                  <div
                    class="px-1.5 py-0.5 text-xs rounded truncate cursor-pointer"
                    [class]="eventChipClass(event)"
                    (click)="$event.stopPropagation(); openDetail(event)"
                    [title]="event.title"
                  >
                    {{ event.title }}
                  </div>
                }
                @if (cell.events.length > 3) {
                  <div class="text-xs text-gray-400 dark:text-gray-500 px-1">
                    +{{ cell.events.length - 3 }} autres
                  </div>
                }
              </div>
            </div>
          }
        </div>
      }
    </div>

    <!-- Event form modal -->
    @if (showForm()) {
      <app-calendar-event-form
        [event]="editingEvent()"
        [prefillDate]="prefillDate()"
        (save)="onFormSave($event)"
        (cancel)="closeForm()"
      />
    }

    <!-- Event detail panel -->
    @if (detailEvent()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
        <div
          class="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-md mx-4 p-6 space-y-3"
        >
          <div class="flex items-start justify-between">
            <div>
              <span
                class="inline-flex px-2 py-0.5 text-xs rounded-full mb-2"
                [class]="eventChipClass(detailEvent()!)"
              >
                {{ typeLabel(detailEvent()!.type) }}
              </span>
              <h3 class="text-base font-semibold text-gray-900 dark:text-gray-100">
                {{ detailEvent()!.title }}
              </h3>
              @if (detailEvent()!.caseNumber) {
                <a
                  [routerLink]="['/cases', detailEvent()!.caseId]"
                  class="text-xs text-blue-600 dark:text-blue-400 hover:underline"
                >
                  {{ detailEvent()!.caseNumber }}
                </a>
              }
            </div>
            <button
              (click)="detailEvent.set(null)"
              class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 text-xl leading-none"
            >
              ×
            </button>
          </div>
          <p class="text-sm text-gray-500 dark:text-gray-400">
            {{ detailEvent()!.date | date: 'EEEE dd MMMM yyyy' : '' : 'fr' }}
          </p>

          @if (detailEvent()!.source === 'CALENDAR_EVENT') {
            <div class="flex gap-2 pt-2">
              @if (authService.hasPermission('CALENDAR_UPDATE')) {
                <button
                  (click)="openEditFromDetail()"
                  class="px-3 py-1.5 text-xs font-medium border border-gray-300 dark:border-gray-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  Modifier
                </button>
              }
              @if (authService.hasPermission('CALENDAR_DELETE')) {
                <button
                  (click)="deleteEvent(detailEvent()!.id)"
                  class="px-3 py-1.5 text-xs font-medium text-red-600 border border-red-200 rounded-lg hover:bg-red-50"
                >
                  Supprimer
                </button>
              }
            </div>
          }
        </div>
      </div>
    }
  `,
})
export class CalendarComponent implements OnInit {
  private calendarService = inject(CalendarService);
  authService = inject(AuthService);

  weekDays = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  viewYear = signal(new Date().getFullYear());
  viewMonth = signal(new Date().getMonth() + 1);
  events = signal<CalendarDayEvent[]>([]);
  loading = signal(true);

  showForm = signal(false);
  editingEvent = signal<CalendarEventResponse | null>(null);
  prefillDate = signal<string | null>(null);
  detailEvent = signal<CalendarDayEvent | null>(null);

  monthLabel = computed(() => {
    const d = new Date(this.viewYear(), this.viewMonth() - 1, 1);
    return d.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  });

  cells = computed<CalendarCell[]>(() => {
    const year = this.viewYear();
    const month = this.viewMonth();
    const firstDay = new Date(year, month - 1, 1);
    const lastDay = new Date(year, month, 0);
    const today = new Date();

    // Monday-first: shift so Monday=0
    const startDow = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1;
    const gridStart = new Date(firstDay);
    gridStart.setDate(gridStart.getDate() - startDow);

    const endDow = lastDay.getDay() === 0 ? 6 : lastDay.getDay() - 1;
    const gridEnd = new Date(lastDay);
    gridEnd.setDate(gridEnd.getDate() + (6 - endDow));

    const eventMap = new Map<string, CalendarDayEvent[]>();
    for (const e of this.events()) {
      const key = e.date.substring(0, 10);
      if (!eventMap.has(key)) eventMap.set(key, []);
      eventMap.get(key)!.push(e);
    }

    const cells: CalendarCell[] = [];
    const cursor = new Date(gridStart);
    while (cursor <= gridEnd) {
      const key = cursor.toISOString().substring(0, 10);
      cells.push({
        date: new Date(cursor),
        isCurrentMonth: cursor.getMonth() + 1 === month,
        isToday:
          cursor.getFullYear() === today.getFullYear() &&
          cursor.getMonth() === today.getMonth() &&
          cursor.getDate() === today.getDate(),
        events: eventMap.get(key) ?? [],
      });
      cursor.setDate(cursor.getDate() + 1);
    }
    return cells;
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.calendarService.getMonthEvents(this.viewYear(), this.viewMonth()).subscribe({
      next: (data) => {
        this.events.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  prevMonth(): void {
    if (this.viewMonth() === 1) {
      this.viewYear.set(this.viewYear() - 1);
      this.viewMonth.set(12);
    } else {
      this.viewMonth.set(this.viewMonth() - 1);
    }
    this.load();
  }

  nextMonth(): void {
    if (this.viewMonth() === 12) {
      this.viewYear.set(this.viewYear() + 1);
      this.viewMonth.set(1);
    } else {
      this.viewMonth.set(this.viewMonth() + 1);
    }
    this.load();
  }

  goToday(): void {
    const now = new Date();
    this.viewYear.set(now.getFullYear());
    this.viewMonth.set(now.getMonth() + 1);
    this.load();
  }

  openCreate(date: Date | null): void {
    if (!this.authService.hasPermission('CALENDAR_CREATE')) return;
    this.editingEvent.set(null);
    this.detailEvent.set(null);
    if (date) {
      const y = date.getFullYear();
      const m = String(date.getMonth() + 1).padStart(2, '0');
      const d = String(date.getDate()).padStart(2, '0');
      this.prefillDate.set(`${y}-${m}-${d}`);
    } else {
      this.prefillDate.set(null);
    }
    this.showForm.set(true);
  }

  openDetail(event: CalendarDayEvent): void {
    this.detailEvent.set(event);
  }

  openEditFromDetail(): void {
    const day = this.detailEvent();
    if (!day) return;
    this.calendarService.getById(day.id).subscribe({
      next: (full) => {
        this.editingEvent.set(full);
        this.prefillDate.set(null);
        this.detailEvent.set(null);
        this.showForm.set(true);
      },
    });
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editingEvent.set(null);
    this.prefillDate.set(null);
  }

  onFormSave(request: CalendarEventRequest): void {
    const editing = this.editingEvent();
    const op = editing
      ? this.calendarService.update(editing.id, request)
      : this.calendarService.create(request);

    op.subscribe({
      next: () => {
        this.closeForm();
        this.load();
      },
    });
  }

  deleteEvent(id: number): void {
    if (!confirm('Supprimer cet événement ?')) return;
    this.calendarService.delete(id).subscribe({
      next: () => {
        this.detailEvent.set(null);
        this.load();
      },
    });
  }

  cellClass(cell: CalendarCell): string {
    const base =
      'min-h-[110px] border-r border-b border-gray-200 dark:border-gray-700 p-1.5 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors';
    if (cell.isToday) return base + ' bg-blue-50 dark:bg-blue-900/20';
    if (!cell.isCurrentMonth) return base + ' opacity-40';
    return base;
  }

  dayNumClass(cell: CalendarCell): string {
    const base = 'text-xs font-medium mb-1 w-6 h-6 flex items-center justify-center rounded-full';
    if (cell.isToday) return base + ' bg-blue-600 text-white';
    return base + ' text-gray-700 dark:text-gray-300';
  }

  eventChipClass(event: CalendarDayEvent | { type: string; source: string }): string {
    if (event.source === 'TASK') {
      return 'bg-amber-100 dark:bg-amber-900/40 text-amber-800 dark:text-amber-200';
    }
    const map: Record<string, string> = {
      HEARING: 'bg-red-100 dark:bg-red-900/40 text-red-800 dark:text-red-200',
      APPOINTMENT: 'bg-blue-100 dark:bg-blue-900/40 text-blue-800 dark:text-blue-200',
      REMINDER: 'bg-purple-100 dark:bg-purple-900/40 text-purple-800 dark:text-purple-200',
    };
    return map[event.type] ?? 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300';
  }

  typeLabel(type: string): string {
    const map: Record<string, string> = {
      HEARING: 'Audience',
      APPOINTMENT: 'Rendez-vous',
      REMINDER: 'Rappel',
      TASK: 'Tâche',
    };
    return map[type] ?? type;
  }
}
