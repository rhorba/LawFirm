import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuditLogService } from '../../../services/audit-log.service';
import { AuditLog } from '../../../core/models/audit-log.model';

@Component({
  selector: 'app-audit-log-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './audit-log-list.component.html',
})
export class AuditLogListComponent {
  private auditLogService = inject(AuditLogService);

  currentPage = signal(0);
  pageSize = signal(10);

  // Data signals
  logs = signal<AuditLog[]>([]);
  totalElements = signal(0);
  totalPages = signal(0);

  // Define actions that should have specific colors
  private actionColors: Record<string, string> = {
    LOGIN_SUCCESS: 'bg-green-100 text-green-800',
    USER_CREATE: 'bg-blue-100 text-blue-800',
    USER_UPDATE: 'bg-yellow-100 text-yellow-800',
    USER_DELETE: 'bg-red-100 text-red-800',
    USER_REGISTER: 'bg-purple-100 text-purple-800',
    USER_RESTORE: 'bg-teal-100 text-teal-800',
    USER_PURGE: 'bg-red-200 text-red-900',
  };

  constructor() {
    this.fetchLogs();
  }

  fetchLogs() {
    this.auditLogService.getAuditLogs(this.currentPage(), this.pageSize()).subscribe((page) => {
      this.logs.set(page.content);
      this.totalElements.set(page.totalElements);
      this.totalPages.set(page.totalPages);
    });
  }

  onPageChange(newPage: number) {
    if (newPage >= 0 && newPage < this.totalPages()) {
      this.currentPage.set(newPage);
      this.fetchLogs();
    }
  }

  getActionClass(action: string): string {
    return this.actionColors[action] || 'bg-gray-100 text-gray-800';
  }
}
