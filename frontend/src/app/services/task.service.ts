import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TaskResponse,
  TaskRequest,
  TaskCommentResponse,
  TaskCommentRequest,
  UpcomingTaskResponse,
} from '../core/models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private http = inject(HttpClient);

  // ── Tasks ─────────────────────────────────────────────────────────────────

  getTasksByCase(caseId: number): Observable<TaskResponse[]> {
    return this.http.get<TaskResponse[]>(`/api/cases/${caseId}/tasks`);
  }

  getById(id: number): Observable<TaskResponse> {
    return this.http.get<TaskResponse>(`/api/tasks/${id}`);
  }

  getUpcoming(days = 7): Observable<UpcomingTaskResponse[]> {
    return this.http.get<UpcomingTaskResponse[]>(`/api/tasks/upcoming`, {
      params: { days: days.toString() },
    });
  }

  create(caseId: number, request: TaskRequest): Observable<TaskResponse> {
    return this.http.post<TaskResponse>(`/api/cases/${caseId}/tasks`, request);
  }

  update(id: number, request: TaskRequest): Observable<TaskResponse> {
    return this.http.put<TaskResponse>(`/api/tasks/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/tasks/${id}`);
  }

  // ── Comments ──────────────────────────────────────────────────────────────

  getComments(taskId: number): Observable<TaskCommentResponse[]> {
    return this.http.get<TaskCommentResponse[]>(`/api/tasks/${taskId}/comments`);
  }

  addComment(taskId: number, request: TaskCommentRequest): Observable<TaskCommentResponse> {
    return this.http.post<TaskCommentResponse>(`/api/tasks/${taskId}/comments`, request);
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`/api/tasks/comments/${commentId}`);
  }
}
