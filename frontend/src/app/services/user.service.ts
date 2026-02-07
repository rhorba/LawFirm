import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  UserResponse,
  RoleResponse,
  PageResponse,
  CreateUserRequest,
  UpdateUserRequest,
  BulkActionRequest,
  BulkStatusRequest,
  BulkActionResponse,
  UserSearchParams,
} from '../core/models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/users`;

  searchUsers(params: UserSearchParams): Observable<PageResponse<UserResponse>> {
    let httpParams = new HttpParams()
      .set('page', (params.page ?? 0).toString())
      .set('size', (params.size ?? 10).toString());

    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.role) httpParams = httpParams.set('role', params.role);
    if (params.enabled !== undefined)
      httpParams = httpParams.set('enabled', params.enabled.toString());
    if (params.showDeleted) httpParams = httpParams.set('showDeleted', 'true');
    if (params.sort) httpParams = httpParams.set('sort', params.sort);

    return this.http.get<PageResponse<UserResponse>>(this.apiUrl, { params: httpParams });
  }

  getUserById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/${id}`);
  }

  createUser(user: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.apiUrl, user);
  }

  updateUser(id: number, user: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}`, user);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  restoreUser(id: number): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.apiUrl}/${id}/restore`, {});
  }

  purgeUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/purge`);
  }

  bulkDelete(request: BulkActionRequest): Observable<BulkActionResponse> {
    return this.http.post<BulkActionResponse>(`${this.apiUrl}/bulk/delete`, request);
  }

  bulkUpdateStatus(request: BulkStatusRequest): Observable<BulkActionResponse> {
    return this.http.post<BulkActionResponse>(`${this.apiUrl}/bulk/status`, request);
  }

  getRoles(): Observable<RoleResponse[]> {
    return this.http.get<RoleResponse[]>(`${environment.apiUrl}/roles`);
  }
}
