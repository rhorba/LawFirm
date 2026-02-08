import { Component, EventEmitter, HostListener, inject, Input, OnInit, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LawyerService } from '../../../services/lawyer.service';
import { CreateLawyerRequest, LawyerResponse, UpdateLawyerRequest } from '../../../core/models/lawyer.model';

@Component({
  selector: 'app-lawyer-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './lawyer-form.component.html',
})
export class LawyerFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private lawyerService = inject(LawyerService);

  @Input() lawyer: LawyerResponse | null = null;
  @Output() close = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  loading = signal(false);
  error = signal<string | null>(null);

  lawyerForm = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    taxId: ['', [Validators.required, Validators.maxLength(50)]],
    email: ['', [Validators.email, Validators.maxLength(255)]],
    phone: ['', [Validators.maxLength(20)]],
  });

  ngOnInit(): void {
    if (this.lawyer) {
      this.lawyerForm.patchValue({
        firstName: this.lawyer.firstName,
        lastName: this.lawyer.lastName,
        taxId: this.lawyer.taxId,
        email: this.lawyer.email || '',
        phone: this.lawyer.phone || '',
      });
    }
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    this.close.emit();
  }

  onSubmit(): void {
    if (this.lawyerForm.invalid) {
      this.lawyerForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const formValue = this.lawyerForm.getRawValue();

    if (this.lawyer) {
      // Update existing lawyer
      const request: UpdateLawyerRequest = {
        firstName: formValue.firstName,
        lastName: formValue.lastName,
        taxId: formValue.taxId,
        email: formValue.email || undefined,
        phone: formValue.phone || undefined,
      };

      this.lawyerService.updateLawyer(this.lawyer.id, request).subscribe({
        next: () => {
          this.loading.set(false);
          this.saved.emit();
        },
        error: (err: unknown) => {
          this.error.set(this.extractErrorMessage(err, 'Failed to update lawyer'));
          this.loading.set(false);
        },
      });
    } else {
      // Create new lawyer
      const request: CreateLawyerRequest = {
        firstName: formValue.firstName,
        lastName: formValue.lastName,
        taxId: formValue.taxId,
        email: formValue.email || undefined,
        phone: formValue.phone || undefined,
      };

      this.lawyerService.createLawyer(request).subscribe({
        next: () => {
          this.loading.set(false);
          this.saved.emit();
        },
        error: (err: unknown) => {
          this.error.set(this.extractErrorMessage(err, 'Failed to create lawyer'));
          this.loading.set(false);
        },
      });
    }
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
