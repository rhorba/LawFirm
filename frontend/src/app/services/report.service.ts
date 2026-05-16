import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ChartDataResponse,
  LawyerWorkloadEntry,
  ReportSummaryResponse,
  UnpaidInvoiceEntry,
} from '../core/models/report.model';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private http = inject(HttpClient);

  private params(from?: string, to?: string): Record<string, string> {
    const p: Record<string, string> = {};
    if (from) p['from'] = from;
    if (to) p['to'] = to;
    return p;
  }

  getSummary(from?: string, to?: string): Observable<ReportSummaryResponse> {
    return this.http.get<ReportSummaryResponse>('/api/reports/summary', { params: this.params(from, to) });
  }

  getCasesByStatus(from?: string, to?: string): Observable<ChartDataResponse> {
    return this.http.get<ChartDataResponse>('/api/reports/cases-by-status', { params: this.params(from, to) });
  }

  getCasesByMonth(from?: string, to?: string): Observable<ChartDataResponse> {
    return this.http.get<ChartDataResponse>('/api/reports/cases-by-month', { params: this.params(from, to) });
  }

  getFinancialByMonth(from?: string, to?: string): Observable<ChartDataResponse> {
    return this.http.get<ChartDataResponse>('/api/reports/financial-by-month', { params: this.params(from, to) });
  }

  getLawyerWorkload(from?: string, to?: string): Observable<LawyerWorkloadEntry[]> {
    return this.http.get<LawyerWorkloadEntry[]>('/api/reports/lawyer-workload', { params: this.params(from, to) });
  }

  getUnpaidInvoices(): Observable<UnpaidInvoiceEntry[]> {
    return this.http.get<UnpaidInvoiceEntry[]>('/api/reports/unpaid-invoices');
  }
}
