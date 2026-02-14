import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  LawyerResponse,
  CreateLawyerRequest,
  UpdateLawyerRequest,
  PageResponse,
  LawyerSearchParams,
} from '../core/models/lawyer.model';

@Injectable({ providedIn: 'root' })
export class LawyerService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/lawyers`;

  searchLawyers(params: LawyerSearchParams): Observable<PageResponse<LawyerResponse>> {
    let httpParams = new HttpParams()
      .set('page', (params.page ?? 0).toString())
      .set('size', (params.size ?? 20).toString());

    if (params.search) {
      httpParams = httpParams.set('search', params.search);
    }

    return this.http.get<PageResponse<LawyerResponse>>(this.apiUrl, { params: httpParams });
  }

  getAll(): Observable<LawyerResponse[]> {
    return this.http.get<LawyerResponse[]>(`${this.apiUrl}/all`);
  }

  getById(id: number): Observable<LawyerResponse> {
    return this.http.get<LawyerResponse>(`${this.apiUrl}/${id}`);
  }

  createLawyer(request: CreateLawyerRequest): Observable<LawyerResponse> {
    return this.http.post<LawyerResponse>(this.apiUrl, request);
  }

  create(request: CreateLawyerRequest): Observable<LawyerResponse> {
    return this.http.post<LawyerResponse>(this.apiUrl, request);
  }

  updateLawyer(id: number, request: UpdateLawyerRequest): Observable<LawyerResponse> {
    return this.http.put<LawyerResponse>(`${this.apiUrl}/${id}`, request);
  }

  update(id: number, request: UpdateLawyerRequest): Observable<LawyerResponse> {
    return this.http.put<LawyerResponse>(`${this.apiUrl}/${id}`, request);
  }

  deactivateLawyer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  activateLawyer(id: number): Observable<LawyerResponse> {
    return this.http.patch<LawyerResponse>(`${this.apiUrl}/${id}/activate`, {});
  }

  getCaseCount(id: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/${id}/cases/count`);
  }
}
