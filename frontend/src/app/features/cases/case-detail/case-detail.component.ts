import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CaseService } from '../../../services/case.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  AuditLogResponse,
  CasePriority,
  CaseResponse,
  CaseSummaryResponse,
} from '../../../core/models/case.model';
import { ChangeStatusModalComponent } from '../change-status-modal/change-status-modal.component';
import { FinancialTabComponent } from './financial-tab/financial-tab.component';
import { TasksTabComponent } from './tasks-tab/tasks-tab.component';
import { PartiesTabComponent } from './parties-tab/parties-tab.component';
import { TimeTabComponent } from './time-tab/time-tab.component';
import { DocumentsTabComponent } from './documents-tab/documents-tab.component';
import { CommunicationsTabComponent } from './communications-tab/communications-tab.component';
import { SearchableSelectComponent } from '../../../shared/searchable-select/searchable-select.component';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-case-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ChangeStatusModalComponent,
    DatePipe,
    FinancialTabComponent,
    TasksTabComponent,
    PartiesTabComponent,
    TimeTabComponent,
    DocumentsTabComponent,
    CommunicationsTabComponent,
    SearchableSelectComponent,
    FormsModule,
  ],
  templateUrl: './case-detail.component.html',
})
export class CaseDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private caseService = inject(CaseService);
  authService = inject(AuthService);

  // State
  caseData = signal<CaseResponse | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  showStatusModal = signal(false);

  // New: children, history, active tab
  children = signal<CaseSummaryResponse[]>([]);
  history = signal<AuditLogResponse[]>([]);
  activeTab = signal<
    | 'details'
    | 'children'
    | 'history'
    | 'financial'
    | 'tasks'
    | 'parties'
    | 'time'
    | 'documents'
    | 'communications'
  >('details');

  // Client assignment
  showClientAssign = signal(false);
  selectedClientId = signal<number | null>(null);

  // Permissions
  canUpdate = this.authService.hasPermission('CASE_UPDATE');
  canDelete = this.authService.hasPermission('CASE_DELETE');
  canViewFinancials = this.authService.hasPermission('FINANCIAL_READ');
  canViewTasks = this.authService.hasPermission('TASK_READ');
  canViewParties = this.authService.hasPermission('CONFLICT_READ');
  canViewTime = this.authService.hasPermission('TIME_READ');
  canViewDocuments = this.authService.hasPermission('DOCUMENT_READ');
  canViewCommunications = this.authService.hasPermission('COMMUNICATION_READ');

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      const caseId = parseInt(id, 10);
      this.loadCase(caseId);
      this.caseService.getCaseChildren(caseId).subscribe({
        next: (c) => this.children.set(c),
        error: () => {},
      });
      this.caseService.getCaseHistory(caseId).subscribe({
        next: (h) => this.history.set(h),
        error: () => {},
      });
    } else {
      this.error.set('Invalid case ID');
      this.loading.set(false);
    }
  }

  loadCase(id: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.caseService.getCaseById(id).subscribe({
      next: (response: CaseResponse) => {
        this.caseData.set(response);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(this.extractErrorMessage(err, 'Failed to load case details'));
        this.loading.set(false);
      },
    });
  }

  openStatusModal(): void {
    this.showStatusModal.set(true);
  }

  closeStatusModal(): void {
    this.showStatusModal.set(false);
  }

  onStatusChanged(): void {
    this.closeStatusModal();
    const id = this.caseData()?.id;
    if (id) {
      this.loadCase(id);
      // Refresh history after status change
      this.caseService.getCaseHistory(id).subscribe({
        next: (h) => this.history.set(h),
        error: () => {},
      });
    }
  }

  deleteCase(): void {
    const caseData = this.caseData();
    if (!caseData) return;

    if (!confirm(`Are you sure you want to delete case ${caseData.fullCaseNumber}?`)) {
      return;
    }

    this.caseService.deleteCase(caseData.id).subscribe({
      next: () => {
        this.router.navigate(['/cases']);
      },
      error: (err: unknown) => {
        alert(this.extractErrorMessage(err, 'Failed to delete case'));
      },
    });
  }

  assignClient(): void {
    const caseId = this.caseData()?.id;
    const clientId = this.selectedClientId();
    if (!caseId || clientId === null) return;

    this.caseService.assignClient(caseId, { clientId }).subscribe({
      next: (updated) => {
        this.caseData.set(updated);
        this.showClientAssign.set(false);
        this.selectedClientId.set(null);
      },
      error: (err: unknown) => alert(this.extractErrorMessage(err, 'Failed to assign client')),
    });
  }

  unassignClient(): void {
    const caseId = this.caseData()?.id;
    if (!caseId || !confirm('Remove client assignment?')) return;

    this.caseService.assignClient(caseId, { clientId: null }).subscribe({
      next: (updated) => this.caseData.set(updated),
      error: (err: unknown) => alert(this.extractErrorMessage(err, 'Failed to unassign client')),
    });
  }

  readonly fieldLabels: Record<string, string> = {
    fullCaseNumber: 'N° dossier',
    tribunal: 'Tribunal',
    caseType: "Type d'affaire",
    caseCategory: 'Catégorie',
    status: 'Statut',
    priority: 'Priorité',
    lawyers: 'Avocats',
    clientName: 'Client',
    registrationDate: "Date d'enregistrement",
    caseDescription: 'Description',
    matterDescription: 'Objet',
    opposingParty: 'Partie adverse',
    outcome: 'Résultat',
    outcomeNotes: 'Notes de résultat',
    fiscalYear: 'Exercice fiscal',
    initialPaymentDate: 'Date de paiement initial',
  };

  getDiffRows(
    log: AuditLogResponse
  ): { field: string; label: string; before: string; after: string }[] {
    if (!log.newValues) return [];
    const before = log.oldValues ?? {};
    const after = log.newValues;
    return Object.keys(after)
      .filter((k) => JSON.stringify(before[k]) !== JSON.stringify(after[k]))
      .map((k) => ({
        field: k,
        label: this.fieldLabels[k] ?? k,
        before: this.formatValue(before[k]),
        after: this.formatValue(after[k]),
      }));
  }

  private formatValue(val: unknown): string {
    if (val == null) return '—';
    if (typeof val === 'object') {
      const obj = val as Record<string, unknown>;
      return String(obj['nameFr'] ?? obj['fullName'] ?? obj['firstName'] ?? JSON.stringify(val));
    }
    return String(val);
  }

  // kept for backward compatibility with pre-snapshot audit entries
  getAuditDiffKeys(log: AuditLogResponse): string[] {
    const keys = new Set<string>();
    if (log.oldValues) Object.keys(log.oldValues).forEach((k) => keys.add(k));
    if (log.newValues) Object.keys(log.newValues).forEach((k) => keys.add(k));
    return Array.from(keys);
  }

  parseChangedFields(metadata: string): string[] {
    try {
      const parsed = JSON.parse(metadata);
      return parsed?.changedFields ?? [];
    } catch {
      return [];
    }
  }

  getPriorityBadgeClass(priority: CasePriority): string {
    const map: Record<CasePriority, string> = {
      URGENT: 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200',
      HIGH: 'bg-orange-100 dark:bg-orange-900 text-orange-800 dark:text-orange-200',
      NORMAL: 'bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200',
      LOW: 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300',
    };
    return map[priority] ?? '';
  }

  getStatusBadgeClass(statusCode: string): string {
    const classes: Record<string, string> = {
      DRAFT: 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200',
      OPEN: 'bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200',
      IN_PROGRESS: 'bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-200',
      HEARING: 'bg-purple-100 dark:bg-purple-900 text-purple-800 dark:text-purple-200',
      JUDGMENT: 'bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200',
      CLOSED: 'bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200',
      ARCHIVED: 'bg-gray-300 dark:bg-gray-600 text-gray-700 dark:text-gray-300',
    };
    return classes[statusCode] || 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200';
  }

  private extractErrorMessage(err: unknown, fallback: string): string {
    const httpErr = err as {
      error?: { message?: string; validationErrors?: Record<string, string> };
    };
    const body = httpErr.error;
    if (body?.validationErrors) {
      return Object.values(body.validationErrors).join('. ');
    }
    return body?.message || fallback;
  }
}
