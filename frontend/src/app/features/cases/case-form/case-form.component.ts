import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CaseService } from '../../../services/case.service';
import { LawyerService } from '../../../services/lawyer.service';
import { ReferenceDataService } from '../../../services/reference-data.service';
import { CaseResponse, CreateCaseRequest, UpdateCaseRequest } from '../../../core/models/case.model';

@Component({
  selector: 'app-case-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './case-form.component.html',
})
export class CaseFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private caseService = inject(CaseService);
  private lawyerService = inject(LawyerService);
  public refDataService = inject(ReferenceDataService);

  // State
  caseData = signal<CaseResponse | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  isEditMode = signal(false);
  lawyers = signal<any[]>([]);

  caseForm = this.fb.nonNullable.group({
    caseTypeCode: ['', [Validators.required]],
    caseCategoryCode: [''],
    tribunalCode: ['', [Validators.required]],
    lawyerId: [null as number | null, [Validators.required]],
    registrationDate: [new Date().toISOString().split('T')[0], [Validators.required]],
    caseDescription: ['', [Validators.required, Validators.maxLength(500)]],
    matterDescription: ['', [Validators.maxLength(1000)]],
    initialStatusCode: [''], // Only for create mode
  });

  // Convert form control valueChanges to a signal for reactive computed
  private selectedCaseTypeCode = toSignal(
    this.caseForm.get('caseTypeCode')!.valueChanges,
    { initialValue: '' }
  );

  // Computed categories filtered by selected case type
  caseCategories = computed(() => {
    const typeCode = this.selectedCaseTypeCode();
    if (!typeCode) return [];
    return this.refDataService.categories().filter((c: any) => c.caseTypeCode === typeCode);
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');

    if (id) {
      // Edit mode
      this.isEditMode.set(true);
      this.loadCase(parseInt(id, 10));
    } else {
      // Create mode - load initial status options
      this.loadLawyers();
    }

    // Watch case type changes to reset category
    this.caseForm.get('caseTypeCode')?.valueChanges.subscribe(() => {
      this.caseForm.patchValue({ caseCategoryCode: '' });
    });

    // Load lawyers
    this.loadLawyers();
  }

  loadCase(id: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.caseService.getCaseById(id).subscribe({
      next: (response: CaseResponse) => {
        this.caseData.set(response);
        this.patchFormValues(response);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(this.extractErrorMessage(err, 'Failed to load case'));
        this.loading.set(false);
      },
    });
  }

  patchFormValues(caseData: CaseResponse): void {
    this.caseForm.patchValue({
      caseTypeCode: caseData.caseType.code,
      caseCategoryCode: caseData.caseCategory?.code || '',
      tribunalCode: caseData.tribunal.code,
      lawyerId: caseData.lawyer.id,
      registrationDate: caseData.registrationDate,
      caseDescription: caseData.caseDescription,
      matterDescription: caseData.matterDescription || '',
    });

    // Remove initialStatusCode in edit mode
    // this.caseForm.removeControl('initialStatusCode');
  }

  loadLawyers(): void {
    this.lawyerService.getAll().subscribe({
      next: (lawyers) => {
        this.lawyers.set(lawyers.filter(l => l.active));
      },
      error: (err: unknown) => {
        console.error('Failed to load lawyers:', err);
      },
    });
  }

  onSubmit(): void {
    if (this.caseForm.invalid) {
      this.caseForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const formValue = this.caseForm.getRawValue();

    if (this.isEditMode()) {
      // Update existing case
      const request: UpdateCaseRequest = {
        caseTypeCode: formValue.caseTypeCode,
        caseCategoryCode: formValue.caseCategoryCode || undefined,
        tribunalCode: formValue.tribunalCode,
        lawyerId: formValue.lawyerId!,
        registrationDate: formValue.registrationDate,
        caseDescription: formValue.caseDescription,
        matterDescription: formValue.matterDescription || undefined,
      };

      this.caseService.updateCase(this.caseData()!.id, request).subscribe({
        next: (response: CaseResponse) => {
          this.loading.set(false);
          this.router.navigate(['/cases', response.id]);
        },
        error: (err: unknown) => {
          this.error.set(this.extractErrorMessage(err, 'Failed to update case'));
          this.loading.set(false);
        },
      });
    } else {
      // Create new case
      const request: CreateCaseRequest = {
        caseTypeCode: formValue.caseTypeCode,
        caseCategoryCode: formValue.caseCategoryCode || undefined,
        tribunalCode: formValue.tribunalCode,
        lawyerId: formValue.lawyerId!,
        registrationDate: formValue.registrationDate,
        caseDescription: formValue.caseDescription,
        matterDescription: formValue.matterDescription || undefined,
        initialStatusCode: formValue.initialStatusCode || undefined,
      };

      this.caseService.createCase(request).subscribe({
        next: (response: CaseResponse) => {
          this.loading.set(false);
          this.router.navigate(['/cases', response.id]);
        },
        error: (err: unknown) => {
          this.error.set(this.extractErrorMessage(err, 'Failed to create case'));
          this.loading.set(false);
        },
      });
    }
  }

  onCancel(): void {
    if (this.isEditMode() && this.caseData()) {
      this.router.navigate(['/cases', this.caseData()!.id]);
    } else {
      this.router.navigate(['/cases']);
    }
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
