import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CaseStatusResponse } from '../core/models/case.model';

@Injectable({ providedIn: 'root' })
export class CaseStatusService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/case-statuses`;

  getAll(): Observable<CaseStatusResponse[]> {
    return this.http.get<CaseStatusResponse[]>(this.apiUrl);
  }

  getByCode(code: string): Observable<CaseStatusResponse> {
    return this.http.get<CaseStatusResponse>(`${this.apiUrl}/${code}`);
  }

  getStatusesForCaseType(caseTypeCode: string): Observable<CaseStatusResponse[]> {
    const params = new HttpParams().set('caseTypeCode', caseTypeCode);
    return this.http.get<CaseStatusResponse[]>(this.apiUrl, { params });
  }
}
