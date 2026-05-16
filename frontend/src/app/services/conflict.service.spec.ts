import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ConflictService } from './conflict.service';
import { environment } from '../../environments/environment';

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

  it('performCheck should POST searchName to /conflicts/check', () => {
    service.performCheck('Benali').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/conflicts/check`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ searchName: 'Benali' });
    req.flush({ searchName: 'Benali', hasConflict: false, matches: [] });
  });

  it('listChecks should GET /conflicts/checks', () => {
    service.listChecks().subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/conflicts/checks'));
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0 });
  });

  it('clearCheck should PATCH /conflicts/checks/:id/clear', () => {
    service.clearCheck(1, 'Verified: different person').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/conflicts/checks/1/clear`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ clearedNote: 'Verified: different person' });
    req.flush({ id: 1 });
  });

  it('getPartiesByCase should GET /cases/:id/conflict-parties', () => {
    service.getPartiesByCase(10).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/cases/10/conflict-parties`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('addParty should POST to /cases/:id/conflict-parties', () => {
    const payload = { partyType: 'OPPOSING', name: 'Corp XYZ' };
    service.addParty(10, payload as any).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/cases/10/conflict-parties`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1, ...payload });
  });

  it('deleteParty should DELETE /conflict-parties/:id', () => {
    service.deleteParty(1).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/conflict-parties/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
