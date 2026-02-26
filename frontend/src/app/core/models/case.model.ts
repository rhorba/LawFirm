import { LawyerResponse } from './lawyer.model';

export type CasePriority = 'URGENT' | 'HIGH' | 'NORMAL' | 'LOW';
export type CaseOutcome = 'WON' | 'LOST' | 'SETTLED' | 'DISMISSED';

export interface CaseSummaryResponse {
  id: number;
  fullCaseNumber: string;
}

export interface CaseResponse {
  id: number;
  version: number;
  createdAt: string;
  updatedAt: string;
  year: number;
  sequenceNumber: number;
  fullCaseNumber: string;
  registrationDate: string;
  caseDescription: string;
  matterDescription?: string;
  tribunal: TribunalResponse;
  caseType: CaseTypeResponse;
  caseCategory?: CaseCategoryResponse;
  lawyers: LawyerResponse[];
  status: CaseStatusResponse;
  financialSummary: FinancialSummary;
  priority: CasePriority;
  opposingParty?: string;
  outcome?: CaseOutcome;
  outcomeNotes?: string;
  initialPaymentDate?: string;
  fiscalYear?: number;
  parentCase?: CaseSummaryResponse;
}

export interface CaseSummary {
  id: number;
  fullCaseNumber: string;
  caseDescription: string;
  tribunalNameFr: string;
  caseTypeNameFr: string;
  lawyerName: string;
  statusNameFr: string;
  registrationDate: string;
  priority: CasePriority;
}

export interface TribunalResponse {
  id: number;
  code: string;
  nameFr: string;
  nameAr: string;
  active: boolean;
}

export interface CaseTypeResponse {
  id: number;
  code: string;
  nameFr: string;
  nameAr: string;
  numberFormatTemplate: string;
  active: boolean;
  allowedStatuses: CaseStatusResponse[];
}

export interface CaseCategoryResponse {
  id: number;
  code: string;
  nameFr: string;
  nameAr: string;
  caseTypeCode: string;
}

export interface CaseStatusResponse {
  id: number;
  code: string;
  nameFr: string;
  nameAr: string;
  sortOrder: number;
  isTerminal: boolean;
}

export interface FinancialSummary {
  totalPayments: number;
  totalExpenses: number;
  balance: number;
  transactionCount: number;
}

export interface CaseSearchParams {
  year?: number;
  caseTypeCode?: string;
  categoryCode?: string;
  tribunalCode?: string;
  lawyerId?: number;
  statusCode?: string;
  priority?: CasePriority;
  dateFrom?: string;
  dateTo?: string;
  search?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface CreateCaseRequest {
  caseTypeCode: string;
  caseCategoryCode?: string;
  tribunalCode: string;
  lawyerIds: number[];
  registrationDate: string;
  caseDescription: string;
  matterDescription?: string;
  initialStatusCode?: string;
  priority?: CasePriority;
  opposingParty?: string;
  outcome?: CaseOutcome;
  outcomeNotes?: string;
  initialPaymentDate?: string;
  fiscalYear?: number;
  parentCaseId?: number;
}

export interface UpdateCaseRequest {
  caseTypeCode?: string;
  caseCategoryCode?: string;
  tribunalCode?: string;
  lawyerIds?: number[];
  registrationDate?: string;
  caseDescription?: string;
  matterDescription?: string;
  priority?: CasePriority;
  opposingParty?: string;
  outcome?: CaseOutcome;
  outcomeNotes?: string;
  initialPaymentDate?: string;
  fiscalYear?: number;
  parentCaseId?: number;
}

export interface ChangeStatusRequest {
  statusCode: string;
  reason?: string;
}

export interface CaseTemplateRequest {
  name: string;
  caseTypeCode: string;
  caseCategoryCode: string;
}

export interface CaseTemplateResponse {
  id: number;
  name: string;
  caseTypeCode: string;
  caseCategoryCode: string;
}

export interface AuditLogResponse {
  id: number;
  action: string;
  resource: string;
  resourceId: string;
  username: string;
  metadata: string;
  createdAt: string;
}
