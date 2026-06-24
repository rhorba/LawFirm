import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CalendarService } from './calendar.service';

describe('CalendarService', () => {
  let service: CalendarService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CalendarService],
    });
    service = TestBed.inject(CalendarService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getMonthEvents should GET /calendar/month with year and month params', () => {
    service.getMonthEvents(2026, 5).subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/calendar/month'));
    expect(req.request.params.get('year')).toBe('2026');
    expect(req.request.params.get('month')).toBe('5');
    req.flush([]);
  });

  it('getEventsByCase should GET /calendar/cases/:id', () => {
    service.getEventsByCase(10).subscribe();

    const req = httpMock.expectOne('/api/calendar/cases/10');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('create should POST to /calendar', () => {
    const payload = { title: 'Hearing', eventType: 'HEARING', startDatetime: '2026-12-01T10:00:00' };
    service.create(payload as any).subscribe();

    const req = httpMock.expectOne('/api/calendar');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: 1, ...payload });
  });

  it('update should PUT to /calendar/:id', () => {
    const payload = { title: 'Updated Hearing', eventType: 'HEARING', startDatetime: '2026-12-01T10:00:00' };
    service.update(1, payload as any).subscribe();

    const req = httpMock.expectOne('/api/calendar/1');
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 1, ...payload });
  });

  it('delete should DELETE /calendar/:id', () => {
    service.delete(1).subscribe();

    const req = httpMock.expectOne('/api/calendar/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
