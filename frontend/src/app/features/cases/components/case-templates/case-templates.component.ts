import { Component, EventEmitter, OnInit, Output, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CaseService } from '../../../../services/case.service';
import { CaseTemplateResponse } from '../../../../core/models/case.model';

@Component({
  selector: 'app-case-templates',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './case-templates.component.html',
})
export class CaseTemplatesComponent implements OnInit {
  @Output() templateSelected = new EventEmitter<CaseTemplateResponse>();
  @Output() closed = new EventEmitter<void>();

  private caseService = inject(CaseService);

  templates = signal<CaseTemplateResponse[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.caseService.getTemplates().subscribe({
      next: (t) => {
        this.templates.set(t);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load templates.');
        this.loading.set(false);
      },
    });
  }

  select(template: CaseTemplateResponse): void {
    this.templateSelected.emit(template);
  }
}
