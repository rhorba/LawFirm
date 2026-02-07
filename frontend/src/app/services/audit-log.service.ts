import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuditLog } from '../core/models/audit-log.model';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root',
})
export class AuditLogService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/audit-logs`;

  getAuditLogs(page: number = 0, size: number = 10): Observable<Page<AuditLog>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    return this.http.get<Page<AuditLog>>(this.apiUrl, { params });
  }
}
