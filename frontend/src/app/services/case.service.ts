import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AssignClientRequest,
  AuditLogResponse,
  CaseResponse,
  CaseSearchParams,
  CaseSummary,
  CaseSummaryResponse,
  CaseTemplateRequest,
  CaseTemplateResponse,
  ChangeStatusRequest,
  CreateCaseRequest,
  PageResponse,
  UpdateCaseRequest,
} from '../core/models/case.model';

@Injectable({ providedIn: 'root' })
export class CaseService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/cases`;

  searchCases(params: CaseSearchParams): Observable<PageResponse<CaseSummary>> {
    let httpParams = new HttpParams()
      .set('page', (params.page ?? 0).toString())
      .set('size', (params.size ?? 20).toString())
      .set('sortBy', params.sortBy ?? 'registrationDate')
      .set('sortDirection', params.sortDirection ?? 'DESC');

    if (params.year !== undefined && params.year !== null) {
      httpParams = httpParams.set('year', params.year.toString());
    }
    if (params.caseTypeCode) {
      httpParams = httpParams.set('caseTypeCode', params.caseTypeCode);
    }
    if (params.categoryCode) {
      httpParams = httpParams.set('categoryCode', params.categoryCode);
    }
    if (params.tribunalCode) {
      httpParams = httpParams.set('tribunalCode', params.tribunalCode);
    }
    if (params.lawyerId !== undefined && params.lawyerId !== null) {
      httpParams = httpParams.set('lawyerId', params.lawyerId.toString());
    }
    if (params.statusCode) {
      httpParams = httpParams.set('statusCode', params.statusCode);
    }
    if (params.priority) {
      httpParams = httpParams.set('priority', params.priority);
    }
    if (params.dateFrom) {
      httpParams = httpParams.set('registrationDateFrom', params.dateFrom);
    }
    if (params.dateTo) {
      httpParams = httpParams.set('registrationDateTo', params.dateTo);
    }
    if (params.search) {
      httpParams = httpParams.set('search', params.search);
    }

    return this.http.get<PageResponse<CaseSummary>>(this.apiUrl, { params: httpParams });
  }

  getCaseById(id: number): Observable<CaseResponse> {
    return this.http.get<CaseResponse>(`${this.apiUrl}/${id}`);
  }

  createCase(request: CreateCaseRequest): Observable<CaseResponse> {
    return this.http.post<CaseResponse>(this.apiUrl, request);
  }

  updateCase(id: number, request: UpdateCaseRequest): Observable<CaseResponse> {
    return this.http.put<CaseResponse>(`${this.apiUrl}/${id}`, request);
  }

  changeStatus(id: number, request: ChangeStatusRequest): Observable<CaseResponse> {
    return this.http.patch<CaseResponse>(`${this.apiUrl}/${id}/status`, request);
  }

  deleteCase(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  assignClient(caseId: number, request: AssignClientRequest): Observable<CaseResponse> {
    return this.http.patch<CaseResponse>(`${this.apiUrl}/${caseId}/client`, request);
  }

  exportCases(params: CaseSearchParams): Observable<Blob> {
    let httpParams = new HttpParams();

    if (params.year !== undefined && params.year !== null) {
      httpParams = httpParams.set('year', params.year.toString());
    }
    if (params.caseTypeCode) {
      httpParams = httpParams.set('caseTypeCode', params.caseTypeCode);
    }
    if (params.categoryCode) {
      httpParams = httpParams.set('categoryCode', params.categoryCode);
    }
    if (params.tribunalCode) {
      httpParams = httpParams.set('tribunalCode', params.tribunalCode);
    }
    if (params.lawyerId !== undefined && params.lawyerId !== null) {
      httpParams = httpParams.set('lawyerId', params.lawyerId.toString());
    }
    if (params.statusCode) {
      httpParams = httpParams.set('statusCode', params.statusCode);
    }
    if (params.dateFrom) {
      httpParams = httpParams.set('registrationDateFrom', params.dateFrom);
    }
    if (params.dateTo) {
      httpParams = httpParams.set('registrationDateTo', params.dateTo);
    }
    if (params.search) {
      httpParams = httpParams.set('search', params.search);
    }

    return this.http.get(`${this.apiUrl}/export`, {
      params: httpParams,
      responseType: 'blob',
    });
  }

  getCaseChildren(id: number): Observable<CaseSummaryResponse[]> {
    return this.http.get<CaseSummaryResponse[]>(`${this.apiUrl}/${id}/children`);
  }

  getCaseHistory(id: number): Observable<AuditLogResponse[]> {
    return this.http.get<AuditLogResponse[]>(`${this.apiUrl}/${id}/history`);
  }

  getTemplates(): Observable<CaseTemplateResponse[]> {
    return this.http.get<CaseTemplateResponse[]>(`${this.apiUrl}/templates`);
  }

  createTemplate(request: CaseTemplateRequest): Observable<CaseTemplateResponse> {
    return this.http.post<CaseTemplateResponse>(`${this.apiUrl}/templates`, request);
  }

  deleteTemplate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/templates/${id}`);
  }
}
