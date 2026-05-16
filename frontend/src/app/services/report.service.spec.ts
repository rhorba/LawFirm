import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ReportService } from './report.service';
import { environment } from '../../environments/environment';

describe('ReportService', () => {
  let service: ReportService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ReportService],
    });
    service = TestBed.inject(ReportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getSummary should GET /reports/summary with date range', () => {
    service.getSummary('2026-01-01', '2026-12-31').subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/reports/summary'));
    expect(req.request.params.get('from')).toBe('2026-01-01');
    expect(req.request.params.get('to')).toBe('2026-12-31');
    req.flush({ totalCases: 10, openCases: 5, overdueTasks: 2 });
  });

  it('getCasesByStatus should GET /reports/cases-by-status', () => {
    service.getCasesByStatus('2026-01-01', '2026-12-31').subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/reports/cases-by-status'));
    expect(req.request.method).toBe('GET');
    req.flush({ labels: ['DRAFT', 'ACTIVE'], values: [3, 7] });
  });

  it('getFinancialByMonth should GET /reports/financial-by-month', () => {
    service.getFinancialByMonth('2026-01-01', '2026-12-31').subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/reports/financial-by-month'));
    expect(req.request.method).toBe('GET');
    req.flush({ labels: ['2026-01'], revenue: [5000], expenses: [1000] });
  });

  it('getLawyerWorkload should GET /reports/lawyer-workload', () => {
    service.getLawyerWorkload('2026-01-01', '2026-12-31').subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/reports/lawyer-workload'));
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getTopUnpaidInvoices should GET /reports/top-unpaid-invoices', () => {
    service.getTopUnpaidInvoices().subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/reports/top-unpaid-invoices'));
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
