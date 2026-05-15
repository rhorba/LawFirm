import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TimeEntryRequest,
  TimeEntryResponse,
  TimeEntrySummaryResponse,
} from '../core/models/time-entry.model';

@Injectable({ providedIn: 'root' })
export class TimeEntryService {
  private http = inject(HttpClient);

  getByCase(caseId: number): Observable<TimeEntryResponse[]> {
    return this.http.get<TimeEntryResponse[]>(`/api/cases/${caseId}/time-entries`);
  }

  getSummary(caseId: number): Observable<TimeEntrySummaryResponse> {
    return this.http.get<TimeEntrySummaryResponse>(`/api/cases/${caseId}/time-entries/summary`);
  }

  create(caseId: number, request: TimeEntryRequest): Observable<TimeEntryResponse> {
    return this.http.post<TimeEntryResponse>(`/api/cases/${caseId}/time-entries`, request);
  }

  update(id: number, request: TimeEntryRequest): Observable<TimeEntryResponse> {
    return this.http.put<TimeEntryResponse>(`/api/time-entries/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/time-entries/${id}`);
  }

  markAsBilled(id: number, invoiceId?: number): Observable<TimeEntryResponse> {
    const params: Record<string, string> = {};
    if (invoiceId != null) params['invoiceId'] = invoiceId.toString();
    return this.http.post<TimeEntryResponse>(`/api/time-entries/${id}/bill`, null, { params });
  }
}
