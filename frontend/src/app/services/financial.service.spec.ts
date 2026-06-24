import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FinancialService } from './financial.service';

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

  it('getTransactions should GET /financial/transactions with filter and paging', () => {
    service.getTransactions({}, { page: 0, size: 20 }).subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/financial/transactions') && !r.url.includes('export'));
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0 });
  });

  it('getTransactionsByCase should GET /financial/cases/:id/transactions', () => {
    service.getTransactionsByCase(10).subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/cases/10/transactions'));
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('createTransaction should POST to /financial/transactions', () => {
    const payload = { caseId: 10, direction: 'REVENUE', amount: 5000, operationType: 'FEES', paymentDate: '2026-05-01' };
    service.createTransaction(payload as any).subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/financial/transactions') && !r.url.includes('export'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: 1, ...payload });
  });

  it('exportExcel should GET blob from /financial/transactions/export/excel', () => {
    service.exportExcel({}).subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/financial/transactions/export/excel'));
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob());
  });

  it('getInvoices should GET /financial/invoices with pagination', () => {
    service.getInvoices({ page: 0, size: 10 }).subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/invoices'));
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0 });
  });

  it('createInvoice should POST to /financial/invoices', () => {
    const payload = { caseId: 1, clientId: 2, issueDate: '2026-05-01', dueDate: '2026-06-01', items: [] };
    service.createInvoice(payload as any).subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/invoices'));
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1, ...payload });
  });
});
