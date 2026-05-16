import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CaseService } from './case.service';
import { environment } from '../../environments/environment';
import { CaseResponse, CaseSearchParams, CreateCaseRequest } from '../core/models/case.model';

describe('CaseService', () => {
  let service: CaseService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/cases`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CaseService],
    });
    service = TestBed.inject(CaseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('searchCases should GET with default pagination params', () => {
    const params: CaseSearchParams = { page: 0, size: 20 };
    service.searchCases(params).subscribe();

    const req = httpMock.expectOne((r) => r.url === apiUrl);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('searchCases should include filter params when provided', () => {
    const params: CaseSearchParams = {
      page: 0, size: 20,
      caseTypeCode: 'PEN',
      statusCode: 'ACTIVE',
      search: 'benali',
    };
    service.searchCases(params).subscribe();

    const req = httpMock.expectOne((r) => r.url === apiUrl);
    expect(req.request.params.get('caseTypeCode')).toBe('PEN');
    expect(req.request.params.get('statusCode')).toBe('ACTIVE');
    expect(req.request.params.get('search')).toBe('benali');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('getCaseById should GET /cases/:id', () => {
    const mockCase: Partial<CaseResponse> = { id: 1, fullCaseNumber: 'PEN-2026/TRB/001' };
    service.getCaseById(1).subscribe((res) => {
      expect(res.fullCaseNumber).toBe('PEN-2026/TRB/001');
    });

    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockCase);
  });

  it('createCase should POST to /cases', () => {
    const request: Partial<CreateCaseRequest> = {
      caseTypeCode: 'PEN',
      tribunalCode: 'TRB-001',
    };
    service.createCase(request as CreateCaseRequest).subscribe();

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 1 });
  });

  it('deleteCase should DELETE /cases/:id', () => {
    service.deleteCase(5).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/5`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('changeStatus should PATCH /cases/:id/status', () => {
    service.changeStatus(1, { statusCode: 'ACTIVE', reason: '' }).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/1/status`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ id: 1 });
  });

  it('exportCases should GET blob from /cases/export', () => {
    service.exportCases({}).subscribe();

    const req = httpMock.expectOne((r) => r.url === `${apiUrl}/export`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob());
  });
});
