import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CaseService } from '../../../services/case.service';
import { ReferenceDataService } from '../../../services/reference-data.service';
import {
  CaseResponse,
  CaseTemplateResponse,
  CreateCaseRequest,
  UpdateCaseRequest,
} from '../../../core/models/case.model';
import { CaseTemplatesComponent } from '../components/case-templates/case-templates.component';
import { SearchableSelectComponent } from '../../../shared/searchable-select/searchable-select.component';

@Component({
  selector: 'app-case-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, CaseTemplatesComponent, SearchableSelectComponent],
  templateUrl: './case-form.component.html',
})
export class CaseFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private caseService = inject(CaseService);
  public refDataService = inject(ReferenceDataService);

  // State
  caseData = signal<CaseResponse | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  isEditMode = signal(false);
  showTemplateModal = signal(false);

  caseForm = this.fb.nonNullable.group({
    caseTypeCode: ['', [Validators.required]],
    caseCategoryCode: [''],
    tribunalCode: ['', [Validators.required]],
    lawyerIds: [[] as number[]],
    registrationDate: [new Date().toISOString().split('T')[0], [Validators.required]],
    caseDescription: ['', [Validators.required, Validators.maxLength(500)]],
    matterDescription: ['', [Validators.maxLength(1000)]],
    initialStatusCode: [''],
    priority: ['NORMAL'],
    opposingParty: ['', [Validators.maxLength(255)]],
    outcome: [''],
    outcomeNotes: ['', [Validators.maxLength(1000)]],
    initialPaymentDate: [''],
    fiscalYear: [null as number | null],
    clientId: [null as number | null],
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');

    if (id) {
      this.isEditMode.set(true);
      this.loadCase(parseInt(id, 10));
    }

    // Reset category when case type changes
    this.caseForm.get('caseTypeCode')?.valueChanges.subscribe(() => {
      this.caseForm.patchValue({ caseCategoryCode: '' });
    });
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
      lawyerIds: caseData.lawyers.map((l) => l.id),
      registrationDate: caseData.registrationDate,
      caseDescription: caseData.caseDescription,
      matterDescription: caseData.matterDescription || '',
      priority: caseData.priority || 'NORMAL',
      opposingParty: caseData.opposingParty || '',
      outcome: caseData.outcome || '',
      outcomeNotes: caseData.outcomeNotes || '',
      initialPaymentDate: caseData.initialPaymentDate || '',
      fiscalYear: caseData.fiscalYear ?? null,
      clientId: caseData.clientId ?? null,
    });
  }

  applyTemplate(template: CaseTemplateResponse): void {
    this.caseForm.patchValue({
      caseTypeCode: template.caseTypeCode,
      caseCategoryCode: template.caseCategoryCode,
    });
    this.showTemplateModal.set(false);
  }

  saveAsTemplate(): void {
    const name = prompt('Enter template name:');
    if (!name?.trim()) return;
    const formValue = this.caseForm.getRawValue();
    if (!formValue.caseTypeCode || !formValue.caseCategoryCode) {
      alert('Please select a Case Type and Category before saving a template.');
      return;
    }
    this.caseService
      .createTemplate({
        name: name.trim(),
        caseTypeCode: formValue.caseTypeCode,
        caseCategoryCode: formValue.caseCategoryCode,
      })
      .subscribe({
        next: () => alert('Template saved successfully!'),
        error: (err: unknown) =>
          alert('Failed to save template: ' + this.extractErrorMessage(err, 'Unknown error')),
      });
  }

  onSubmit(): void {
    if (this.caseForm.invalid) {
      this.caseForm.markAllAsTouched();
      return;
    }

    const formValue = this.caseForm.getRawValue();
    const lawyerIds = (formValue.lawyerIds as number[]) ?? [];

    if (lawyerIds.length === 0) {
      this.error.set('At least one lawyer must be selected.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    if (this.isEditMode()) {
      const request: UpdateCaseRequest = {
        caseTypeCode: formValue.caseTypeCode,
        caseCategoryCode: formValue.caseCategoryCode || undefined,
        tribunalCode: formValue.tribunalCode,
        lawyerIds,
        registrationDate: formValue.registrationDate,
        caseDescription: formValue.caseDescription,
        matterDescription: formValue.matterDescription || undefined,
        priority: (formValue.priority as any) || undefined,
        opposingParty: formValue.opposingParty || undefined,
        outcome: (formValue.outcome as any) || undefined,
        outcomeNotes: formValue.outcomeNotes || undefined,
        initialPaymentDate: formValue.initialPaymentDate || undefined,
        fiscalYear: formValue.fiscalYear ?? undefined,
        clientId: formValue.clientId ?? undefined,
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
      const request: CreateCaseRequest = {
        caseTypeCode: formValue.caseTypeCode,
        caseCategoryCode: formValue.caseCategoryCode || undefined,
        tribunalCode: formValue.tribunalCode,
        lawyerIds,
        registrationDate: formValue.registrationDate,
        caseDescription: formValue.caseDescription,
        matterDescription: formValue.matterDescription || undefined,
        initialStatusCode: formValue.initialStatusCode || undefined,
        priority: (formValue.priority as any) || undefined,
        opposingParty: formValue.opposingParty || undefined,
        outcome: (formValue.outcome as any) || undefined,
        outcomeNotes: formValue.outcomeNotes || undefined,
        initialPaymentDate: formValue.initialPaymentDate || undefined,
        fiscalYear: formValue.fiscalYear ?? undefined,
        clientId: formValue.clientId ?? undefined,
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
