import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DocumentCategory, DocumentResponse } from '../core/models/document.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private http = inject(HttpClient);

  getByCase(caseId: number, q?: string): Observable<DocumentResponse[]> {
    const params: Record<string, string> = {};
    if (q && q.trim()) params['q'] = q.trim();
    return this.http.get<DocumentResponse[]>(`/api/cases/${caseId}/documents`, { params });
  }

  upload(
    caseId: number,
    file: File,
    title: string,
    description: string,
    category: DocumentCategory
  ): Observable<DocumentResponse> {
    const form = new FormData();
    form.append('file', file);
    form.append('title', title);
    if (description.trim()) form.append('description', description.trim());
    form.append('category', category);
    return this.http.post<DocumentResponse>(`/api/cases/${caseId}/documents`, form);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/documents/${id}`);
  }

  getDownloadUrl(id: number): string {
    return `/api/documents/${id}/download`;
  }

  getPreviewUrl(id: number): string {
    return `/api/documents/${id}/download?inline=true`;
  }
}
