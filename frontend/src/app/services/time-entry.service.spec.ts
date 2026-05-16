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

  it('getEntriesByCase should GET /cases/:id/time-entries', () => {
    service.getEntriesByCase(10).subscribe();

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

  it('createEntry should POST to /cases/:id/time-entries', () => {
    const payload = { lawyerId: 1, hours: 2.5, hourlyRate: 500, billable: true, entryDate: '2026-05-01' };
    service.createEntry(10, payload as any).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/cases/10/time-entries`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: 1, ...payload });
  });

  it('deleteEntry should DELETE /time-entries/:id', () => {
    service.deleteEntry(1).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/time-entries/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('markAsBilled should PATCH /time-entries/:id/billed', () => {
    service.markAsBilled(1, null).subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/time-entries/1/billed'));
    expect(req.request.method).toBe('PATCH');
    req.flush({ id: 1, billed: true });
  });
});
