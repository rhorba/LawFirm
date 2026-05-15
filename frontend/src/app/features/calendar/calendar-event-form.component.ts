import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CalendarEventResponse,
  CalendarEventRequest,
  EventType,
} from '../../core/models/calendar.model';

@Component({
  selector: 'app-calendar-event-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div
        class="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-lg mx-4 p-6 space-y-4"
      >
        <h3 class="text-base font-semibold text-gray-900 dark:text-gray-100">
          {{ event ? "Modifier l'événement" : 'Nouvel événement' }}
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

        <!-- Type + All day -->
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
              >Type *</label
            >
            <select
              [(ngModel)]="eventType"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
            >
              <option value="HEARING">Audience</option>
              <option value="APPOINTMENT">Rendez-vous</option>
              <option value="REMINDER">Rappel</option>
            </select>
          </div>
          <div class="flex items-end pb-2">
            <label class="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
              <input type="checkbox" [(ngModel)]="allDay" class="w-4 h-4 rounded" />
              Journée entière
            </label>
          </div>
        </div>

        <!-- Start + End datetime -->
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
              >Début *</label
            >
            <input
              [type]="allDay ? 'date' : 'datetime-local'"
              [(ngModel)]="startDatetime"
              required
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1"
              >Fin</label
            >
            <input
              [type]="allDay ? 'date' : 'datetime-local'"
              [(ngModel)]="endDatetime"
              class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 outline-none"
            />
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
            {{ event ? 'Enregistrer' : 'Créer' }}
          </button>
        </div>
      </div>
    </div>
  `,
})
export class CalendarEventFormComponent implements OnInit {
  @Input() event: CalendarEventResponse | null = null;
  @Input() prefillDate: string | null = null;
  @Output() save = new EventEmitter<CalendarEventRequest>();
  @Output() cancel = new EventEmitter<void>();

  title = '';
  description = '';
  eventType: EventType = 'APPOINTMENT';
  startDatetime = '';
  endDatetime = '';
  allDay = false;

  ngOnInit(): void {
    if (this.event) {
      this.title = this.event.title;
      this.description = this.event.description ?? '';
      this.eventType = this.event.eventType;
      this.allDay = this.event.allDay;
      this.startDatetime = this.toInputValue(this.event.startDatetime);
      this.endDatetime = this.event.endDatetime ? this.toInputValue(this.event.endDatetime) : '';
    } else if (this.prefillDate) {
      this.startDatetime = this.prefillDate + 'T09:00';
    }
  }

  isValid(): boolean {
    return this.title.trim().length > 0 && this.startDatetime.length > 0;
  }

  submit(): void {
    if (!this.isValid()) return;
    this.save.emit({
      title: this.title.trim(),
      description: this.description.trim() || undefined,
      eventType: this.eventType,
      startDatetime: this.toIso(this.startDatetime),
      endDatetime: this.endDatetime ? this.toIso(this.endDatetime) : undefined,
      allDay: this.allDay,
    });
  }

  private toInputValue(iso: string): string {
    return iso.substring(0, 16);
  }

  private toIso(value: string): string {
    if (value.includes('T')) return value + ':00';
    return value + 'T00:00:00';
  }
}
