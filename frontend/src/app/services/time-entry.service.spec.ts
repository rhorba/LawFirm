import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TimeEntryService } from './time-entry.service';
import { environment } from '../../environments/environment';

describe('TimeEntryService', () => {
  let service: TimeEntryService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TimeEntryService],
    });
    service = TestBed.inject(TimeEntryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getByCase should GET /cases/:id/time-entries', () => {
    service.getByCase(10).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/cases/10/time-entries`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getSummary should GET /cases/:id/time-entries/summary', () => {
    service.getSummary(10).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/cases/10/time-entries/summary`);
    expect(req.request.method).toBe('GET');
    req.flush({ totalHours: 10, billableHours: 8, billedHours: 3 });
  });

  it('create should POST to /cases/:id/time-entries', () => {
    const payload = { lawyerId: 1, hours: 2.5, hourlyRate: 500, billable: true, entryDate: '2026-05-01' };
    service.create(10, payload as any).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/cases/10/time-entries`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: 1, ...payload });
  });

  it('delete should DELETE /time-entries/:id', () => {
    service.delete(1).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/time-entries/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('markAsBilled should POST /time-entries/:id/bill', () => {
    service.markAsBilled(1, undefined).subscribe();

    const req = httpMock.expectOne('/api/time-entries/1/bill');
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1, billed: true });
  });
});
