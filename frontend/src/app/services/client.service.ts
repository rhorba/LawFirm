import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ClientSummary,
  ClientResponse,
  CreateClientRequest,
  UpdateClientRequest,
  ClientSearchParams,
} from '../core/models/client.model';
import { PageResponse } from '../core/models/case.model';

@Injectable({ providedIn: 'root' })
export class ClientService {
  private http = inject(HttpClient);
  private readonly base = '/api/clients';

  search(params: ClientSearchParams): Observable<PageResponse<ClientSummary>> {
    let httpParams = new HttpParams();
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.type) httpParams = httpParams.set('type', params.type);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    return this.http.get<PageResponse<ClientSummary>>(this.base, { params: httpParams });
  }

  getById(id: number): Observable<ClientResponse> {
    return this.http.get<ClientResponse>(`${this.base}/${id}`);
  }

  create(request: CreateClientRequest): Observable<ClientResponse> {
    return this.http.post<ClientResponse>(this.base, request);
  }

  update(id: number, request: UpdateClientRequest): Observable<ClientResponse> {
    return this.http.put<ClientResponse>(`${this.base}/${id}`, request);
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  export(search?: string, type?: string): Observable<Blob> {
    let httpParams = new HttpParams();
    if (search) httpParams = httpParams.set('search', search);
    if (type) httpParams = httpParams.set('type', type);
    return this.http.get(`${this.base}/export`, {
      params: httpParams,
      responseType: 'blob',
    });
  }
}
