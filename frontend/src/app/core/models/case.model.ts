import { LawyerResponse } from './lawyer.model';

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
  lawyer: LawyerResponse;
  status: CaseStatusResponse;
  financialSummary: FinancialSummary;
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
  dateFrom?: string;
  dateTo?: string;
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
  lawyerId: number;
  registrationDate: string;
  caseDescription: string;
  matterDescription?: string;
  initialStatusCode?: string;
}

export interface UpdateCaseRequest {
  caseTypeCode: string;
  caseCategoryCode?: string;
  tribunalCode: string;
  lawyerId: number;
  registrationDate: string;
  caseDescription: string;
  matterDescription?: string;
}

export interface ChangeStatusRequest {
  newStatusCode: string;
  reason?: string;
}
