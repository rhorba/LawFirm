import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CalendarDayEvent,
  CalendarEventResponse,
  CalendarEventRequest,
} from '../core/models/calendar.model';

@Injectable({ providedIn: 'root' })
export class CalendarService {
  private http = inject(HttpClient);

  getMonthEvents(year: number, month: number): Observable<CalendarDayEvent[]> {
    return this.http.get<CalendarDayEvent[]>('/api/calendar/month', {
      params: { year: year.toString(), month: month.toString() },
    });
  }

  getEventsByCase(caseId: number): Observable<CalendarEventResponse[]> {
    return this.http.get<CalendarEventResponse[]>(`/api/calendar/cases/${caseId}`);
  }

  getById(id: number): Observable<CalendarEventResponse> {
    return this.http.get<CalendarEventResponse>(`/api/calendar/${id}`);
  }

  create(request: CalendarEventRequest): Observable<CalendarEventResponse> {
    return this.http.post<CalendarEventResponse>('/api/calendar', request);
  }

  update(id: number, request: CalendarEventRequest): Observable<CalendarEventResponse> {
    return this.http.put<CalendarEventResponse>(`/api/calendar/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/calendar/${id}`);
  }
}
