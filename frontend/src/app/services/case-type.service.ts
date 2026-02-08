import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CaseTypeResponse } from '../core/models/case.model';

@Injectable({ providedIn: 'root' })
export class CaseTypeService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/case-types`;

  getAll(): Observable<CaseTypeResponse[]> {
    return this.http.get<CaseTypeResponse[]>(this.apiUrl);
  }

  getByCode(code: string): Observable<CaseTypeResponse> {
    return this.http.get<CaseTypeResponse>(`${this.apiUrl}/${code}`);
  }
}
