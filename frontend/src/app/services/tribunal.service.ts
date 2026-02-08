import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { TribunalResponse } from '../core/models/case.model';

@Injectable({ providedIn: 'root' })
export class TribunalService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/tribunals`;

  getAll(): Observable<TribunalResponse[]> {
    return this.http.get<TribunalResponse[]>(this.apiUrl);
  }

  getByCode(code: string): Observable<TribunalResponse> {
    return this.http.get<TribunalResponse>(`${this.apiUrl}/${code}`);
  }
}
