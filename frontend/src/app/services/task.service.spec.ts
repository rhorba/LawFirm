import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TaskService } from './task.service';
import { environment } from '../../environments/environment';

describe('TaskService', () => {
  let service: TaskService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TaskService],
    });
    service = TestBed.inject(TaskService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getTasksByCase should GET /cases/:id/tasks', () => {
    service.getTasksByCase(10).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/cases/10/tasks`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getById should GET /tasks/:id', () => {
    service.getById(1).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/tasks/1`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 1, title: 'Test Task' });
  });

  it('create should POST to /cases/:id/tasks', () => {
    const payload = { title: 'New Task', dueDate: '2026-12-01', priority: 'HIGH' };
    service.create(10, payload as any).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/cases/10/tasks`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: 2, ...payload });
  });

  it('update should PUT to /tasks/:id', () => {
    const payload = { title: 'Updated Task', status: 'IN_PROGRESS' };
    service.update(1, payload as any).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/tasks/1`);
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 1, ...payload });
  });

  it('delete should DELETE /tasks/:id', () => {
    service.delete(1).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/tasks/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('getUpcoming should GET /tasks/upcoming with days param', () => {
    service.getUpcoming(7).subscribe();

    const req = httpMock.expectOne((r) => r.url.includes('/tasks/upcoming'));
    expect(req.request.params.get('days')).toBe('7');
    req.flush([]);
  });

  it('addComment should POST to /tasks/:id/comments', () => {
    service.addComment(1, { content: 'Review needed' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/tasks/1/comments`);
    expect(req.request.method).toBe('POST');
    req.flush({ id: 1, content: 'Review needed' });
  });
});
