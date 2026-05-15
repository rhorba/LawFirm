export type EventType = 'HEARING' | 'APPOINTMENT' | 'REMINDER';
export type EventSource = 'CALENDAR_EVENT' | 'TASK';

export interface CalendarEventResponse {
  id: number;
  title: string;
  description?: string;
  eventType: EventType;
  startDatetime: string;
  endDatetime?: string;
  allDay: boolean;
  caseId?: number;
  caseNumber?: string;
  createdByUserId: number;
  createdByUsername: string;
  createdAt: string;
  updatedAt: string;
}

export interface CalendarDayEvent {
  id: number;
  title: string;
  type: EventType | 'TASK';
  source: EventSource;
  date: string;
  caseId?: number;
  caseNumber?: string;
}

export interface CalendarEventRequest {
  title: string;
  description?: string;
  eventType: EventType;
  startDatetime: string;
  endDatetime?: string;
  allDay: boolean;
  caseId?: number;
}
