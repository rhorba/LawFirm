import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TransactionResponse,
  TransactionRequest,
  FinancialFilter,
  InvoiceResponse,
  InvoiceRequest,
  InvoiceStatusRequest,
} from '../core/models/financial.model';
import { PageResponse } from '../core/models/case.model';

export interface PageParams {
  page: number;
  size: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class FinancialService {
  private http = inject(HttpClient);
  private readonly base = '/api/financial';

  // ── Transactions ───────────────────────────────────────────────────────────

  getTransactions(
    filter: FinancialFilter,
    paging: PageParams
  ): Observable<PageResponse<TransactionResponse>> {
    let params = new HttpParams()
      .set('page', paging.page.toString())
      .set('size', paging.size.toString())
      .set('sort', paging.sort ?? 'createdAt');
    if (filter.caseId) params = params.set('caseId', filter.caseId.toString());
    if (filter.clientId) params = params.set('clientId', filter.clientId.toString());
    if (filter.direction) params = params.set('direction', filter.direction);
    if (filter.operationType) params = params.set('operationType', filter.operationType);
    if (filter.dateFrom) params = params.set('dateFrom', filter.dateFrom);
    if (filter.dateTo) params = params.set('dateTo', filter.dateTo);
    return this.http.get<PageResponse<TransactionResponse>>(`${this.base}/transactions`, {
      params,
    });
  }

  getTransactionsByCase(caseId: number): Observable<TransactionResponse[]> {
    return this.http.get<TransactionResponse[]>(`${this.base}/cases/${caseId}/transactions`);
  }

  createTransaction(request: TransactionRequest): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.base}/transactions`, request);
  }

  softDeleteTransaction(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/transactions/${id}`);
  }

  exportExcel(filter: FinancialFilter): Observable<Blob> {
    let params = new HttpParams();
    if (filter.caseId) params = params.set('caseId', filter.caseId.toString());
    if (filter.clientId) params = params.set('clientId', filter.clientId.toString());
    if (filter.direction) params = params.set('direction', filter.direction);
    if (filter.operationType) params = params.set('operationType', filter.operationType);
    if (filter.dateFrom) params = params.set('dateFrom', filter.dateFrom);
    if (filter.dateTo) params = params.set('dateTo', filter.dateTo);
    return this.http.get(`${this.base}/transactions/export/excel`, {
      params,
      responseType: 'blob',
    });
  }

  // ── Invoices ───────────────────────────────────────────────────────────────

  getInvoices(paging: PageParams): Observable<PageResponse<InvoiceResponse>> {
    const params = new HttpParams()
      .set('page', paging.page.toString())
      .set('size', paging.size.toString());
    return this.http.get<PageResponse<InvoiceResponse>>(`${this.base}/invoices`, { params });
  }

  getInvoice(id: number): Observable<InvoiceResponse> {
    return this.http.get<InvoiceResponse>(`${this.base}/invoices/${id}`);
  }

  createInvoice(request: InvoiceRequest): Observable<InvoiceResponse> {
    return this.http.post<InvoiceResponse>(`${this.base}/invoices`, request);
  }

  updateInvoiceStatus(id: number, request: InvoiceStatusRequest): Observable<InvoiceResponse> {
    return this.http.patch<InvoiceResponse>(`${this.base}/invoices/${id}/status`, request);
  }

  softDeleteInvoice(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/invoices/${id}`);
  }
}
