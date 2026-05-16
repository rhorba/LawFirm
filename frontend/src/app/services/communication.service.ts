import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CommunicationRequest,
  CommunicationResponse,
  EmailSendRequest,
} from '../core/models/communication.model';

@Injectable({ providedIn: 'root' })
export class CommunicationService {
  private http = inject(HttpClient);

  getByCase(caseId: number): Observable<CommunicationResponse[]> {
    return this.http.get<CommunicationResponse[]>(`/api/cases/${caseId}/communications`);
  }

  log(caseId: number, request: CommunicationRequest): Observable<CommunicationResponse> {
    return this.http.post<CommunicationResponse>(`/api/cases/${caseId}/communications`, request);
  }

  sendEmail(caseId: number, request: EmailSendRequest): Observable<CommunicationResponse> {
    return this.http.post<CommunicationResponse>(`/api/cases/${caseId}/communications/send-email`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/communications/${id}`);
  }
}
