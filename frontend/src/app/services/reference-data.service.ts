import { Injectable, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { TribunalService } from './tribunal.service';
import { CaseTypeService } from './case-type.service';
import { CaseStatusService } from './case-status.service';
import { CaseCategoryService } from './case-category.service';
import { LawyerService } from './lawyer.service';
import {
  TribunalResponse,
  CaseTypeResponse,
  CaseStatusResponse,
  CaseCategoryResponse,
} from '../core/models/case.model';
import { LawyerResponse } from '../core/models/lawyer.model';

@Injectable({ providedIn: 'root' })
export class ReferenceDataService {
  private tribunalService = inject(TribunalService);
  private caseTypeService = inject(CaseTypeService);
  private caseStatusService = inject(CaseStatusService);
  private caseCategoryService = inject(CaseCategoryService);
  private lawyerService = inject(LawyerService);

  // Signals for reactive access
  tribunals = signal<TribunalResponse[]>([]);
  caseTypes = signal<CaseTypeResponse[]>([]);
  categories = signal<CaseCategoryResponse[]>([]);
  statuses = signal<CaseStatusResponse[]>([]);
  lawyers = signal<LawyerResponse[]>([]);

  loading = signal(false);
  error = signal<string | null>(null);

  loadAll(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.loading.set(true);

      forkJoin({
        tribunals: this.tribunalService.getAll(),
        caseTypes: this.caseTypeService.getAll(),
        categories: this.caseCategoryService.getAll(),
        statuses: this.caseStatusService.getAll(),
        lawyers: this.lawyerService.getAll(),
      }).subscribe({
        next: (data) => {
          this.tribunals.set(data.tribunals);
          this.caseTypes.set(data.caseTypes);
          this.categories.set(data.categories);
          this.statuses.set(data.statuses);
          this.lawyers.set(data.lawyers);
          this.loading.set(false);
          resolve();
        },
        error: (err) => {
          console.error('Failed to load reference data:', err);
          this.error.set('Failed to load reference data');
          this.loading.set(false);
          reject(err);
        },
      });
    });
  }
}
