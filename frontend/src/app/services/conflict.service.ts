import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ConflictCheckRequest,
  ConflictCheckResultResponse,
  ConflictCheckResponse,
  ConflictClearRequest,
  ConflictPartyRequest,
  ConflictPartyResponse,
} from '../core/models/conflict.model';

export interface ConflictCheckPage {
  content: ConflictCheckResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class ConflictService {
  private http = inject(HttpClient);

  // ── Conflict check ─────────────────────────────────────────────────────────

  performCheck(request: ConflictCheckRequest): Observable<ConflictCheckResultResponse> {
    return this.http.post<ConflictCheckResultResponse>('/api/conflicts/check', request);
  }

  getHistory(page = 0, size = 20): Observable<ConflictCheckPage> {
    return this.http.get<ConflictCheckPage>('/api/conflicts/history', {
      params: { page: page.toString(), size: size.toString(), sort: 'createdAt,desc' },
    });
  }

  clearCheck(id: number, request: ConflictClearRequest): Observable<ConflictCheckResponse> {
    return this.http.post<ConflictCheckResponse>(`/api/conflicts/history/${id}/clear`, request);
  }

  // ── Case parties ──────────────────────────────────────────────────────────

  getPartiesByCase(caseId: number): Observable<ConflictPartyResponse[]> {
    return this.http.get<ConflictPartyResponse[]>(`/api/conflicts/cases/${caseId}/parties`);
  }

  addParty(caseId: number, request: ConflictPartyRequest): Observable<ConflictPartyResponse> {
    return this.http.post<ConflictPartyResponse>(`/api/conflicts/cases/${caseId}/parties`, request);
  }

  deleteParty(partyId: number): Observable<void> {
    return this.http.delete<void>(`/api/conflicts/parties/${partyId}`);
  }
}
