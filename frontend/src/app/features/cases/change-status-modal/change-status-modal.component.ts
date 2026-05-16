import {
  Component,
  EventEmitter,
  HostListener,
  inject,
  Input,
  OnInit,
  Output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CaseService } from '../../../services/case.service';
import { CaseStatusService } from '../../../services/case-status.service';
import { CaseResponse, ChangeStatusRequest } from '../../../core/models/case.model';

@Component({
  selector: 'app-change-status-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-status-modal.component.html',
})
export class ChangeStatusModalComponent implements OnInit {
  private fb = inject(FormBuilder);
  private caseService = inject(CaseService);
  private caseStatusService = inject(CaseStatusService);

  @Input() case!: CaseResponse;
  @Output() close = new EventEmitter<void>();
  @Output() statusChanged = new EventEmitter<void>();

  loading = signal(false);
  error = signal<string | null>(null);
  availableStatuses = signal<any[]>([]);

  statusForm = this.fb.nonNullable.group({
    statusCode: ['', [Validators.required]],
    reason: ['', [Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    this.loadAvailableStatuses();
  }

  loadAvailableStatuses(): void {
    // Load statuses for the case type
    this.caseStatusService.getStatusesForCaseType(this.case.caseType.code).subscribe({
      next: (statuses) => {
        // Filter out current status
        const available = statuses.filter((s) => s.code !== this.case.status.code);
        this.availableStatuses.set(available);
      },
      error: (err: unknown) => {
        this.error.set(this.extractErrorMessage(err, 'Failed to load available statuses'));
      },
    });
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    this.close.emit();
  }

  onSubmit(): void {
    if (this.statusForm.invalid) {
      this.statusForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const formValue = this.statusForm.getRawValue();
    const request: ChangeStatusRequest = {
      statusCode: formValue.statusCode,
      reason: formValue.reason || undefined,
    };

    this.caseService.changeStatus(this.case.id, request).subscribe({
      next: () => {
        this.loading.set(false);
        this.statusChanged.emit();
      },
      error: (err: unknown) => {
        this.error.set(this.extractErrorMessage(err, 'Failed to change status'));
        this.loading.set(false);
      },
    });
  }

  onCancel(): void {
    this.close.emit();
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
