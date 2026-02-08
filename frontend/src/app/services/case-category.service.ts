import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CaseCategoryResponse } from '../core/models/case.model';

@Injectable({ providedIn: 'root' })
export class CaseCategoryService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/case-categories`;

  getAll(): Observable<CaseCategoryResponse[]> {
    return this.http.get<CaseCategoryResponse[]>(this.apiUrl);
  }

  getByCaseType(caseTypeCode: string): Observable<CaseCategoryResponse[]> {
    return this.http.get<CaseCategoryResponse[]>(`${this.apiUrl}?caseTypeCode=${caseTypeCode}`);
  }
}
