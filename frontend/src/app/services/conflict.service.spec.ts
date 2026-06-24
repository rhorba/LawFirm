import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ConflictService } from './conflict.service';

describe('ConflictService', () => {
  let service: ConflictService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ConflictService],
    });
    service = TestBed.inject(ConflictService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('performCheck should POST ConflictCheckRequest to /conflicts/check', () => {
    service.performCheck({ searchName: 'Benali' }).subscribe();

    const req = httpMock.expectOne('/api/conflicts/check');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ searchName: 'Benali' });
    req.flush({ searchName: 'Benali', hasConflict: false, matches: [] });
  });

  it('getHistory should GET /conflicts/history with pagination', () => {
    service.getHistory().subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/conflicts/history'));
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0 });
  });

  it('clearCheck should POST to /conflicts/history/:id/clear', () => {
    service.clearCheck(1, { clearedNote: 'Verified: different person' }).subscribe();

    const req = httpMock.expectOne('/api/conflicts/history/1/clear');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ clearedNote: 'Verified: different person' });
    req.flush({ id: 1 });
  });

  it('getPartiesByCase should GET /conflicts/cases/:id/parties', () => {
    service.getPartiesByCase(10).subscribe();

    const req = httpMock.expectOne('/api/conflicts/cases/10/parties');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('addParty should POST to /conflicts/cases/:id/parties', () => {
    const payload = { partyType: 'OPPOSING', name: 'Corp XYZ' };
    service.addParty(10, payload as any).subscribe();

    const req = httpMock.expectOne('/api/conflicts/cases/10/parties');
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1, ...payload });
  });

  it('deleteParty should DELETE /conflicts/parties/:id', () => {
    service.deleteParty(1).subscribe();

    const req = httpMock.expectOne('/api/conflicts/parties/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
