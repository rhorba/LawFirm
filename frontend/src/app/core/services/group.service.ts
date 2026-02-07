import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GroupResponse, GroupRequest, GroupAssignUsersRequest } from '../models/group.model';

@Injectable({
  providedIn: 'root',
})
export class GroupService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/groups`;

  getAllGroups(): Observable<GroupResponse[]> {
    return this.http.get<GroupResponse[]>(this.apiUrl);
  }

  getGroupById(id: number): Observable<GroupResponse> {
    return this.http.get<GroupResponse>(`${this.apiUrl}/${id}`);
  }

  createGroup(request: GroupRequest): Observable<GroupResponse> {
    return this.http.post<GroupResponse>(this.apiUrl, request);
  }

  updateGroup(id: number, request: GroupRequest): Observable<GroupResponse> {
    return this.http.put<GroupResponse>(`${this.apiUrl}/${id}`, request);
  }

  deleteGroup(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  assignUsers(groupId: number, request: GroupAssignUsersRequest): Observable<GroupResponse> {
    return this.http.post<GroupResponse>(`${this.apiUrl}/${groupId}/users`, request);
  }

  removeUser(groupId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${groupId}/users/${userId}`);
  }
}
