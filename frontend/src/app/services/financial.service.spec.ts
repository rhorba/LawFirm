import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FinancialService } from './financial.service';
import { environment } from '../../environments/environment';

describe('FinancialService', () => {
  let service: FinancialService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [FinancialService],
    });
    service = TestBed.inject(FinancialService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getTransactions should GET /financial/transactions', () => {
    service.getTransactions({}).subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/financial/transactions'));
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0 });
  });

  it('getSummaryByCase should GET /cases/:id/financial/summary', () => {
    service.getSummaryByCase(10).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/cases/10/financial/summary`);
    expect(req.request.method).toBe('GET');
    req.flush({ totalRevenue: 5000, totalExpenses: 1000, netBalance: 4000, transactionCount: 3 });
  });

  it('createTransaction should POST to /financial/transactions', () => {
    const payload = { caseId: 10, direction: 'REVENUE', amount: 5000, operationType: 'FEES', paymentDate: '2026-05-01' };
    service.createTransaction(payload as any).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/financial/transactions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: 1, ...payload });
  });

  it('exportTransactions should GET blob from /financial/transactions/export', () => {
    service.exportTransactions({}).subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/financial/transactions/export'));
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob());
  });
});
