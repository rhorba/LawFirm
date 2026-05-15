export interface TimeEntryResponse {
  id: number;
  caseId: number;
  caseNumber: string;
  lawyerId: number;
  lawyerFullName: string;
  entryDate: string;
  hours: number;
  description: string;
  hourlyRate: number;
  billableAmount: number;
  billable: boolean;
  billed: boolean;
  invoiceId?: number;
  createdAt: string;
  updatedAt: string;
}

export interface TimeEntrySummaryResponse {
  totalHours: number;
  billableHours: number;
  billedHours: number;
  unbilledHours: number;
  billableAmount: number;
  billedAmount: number;
  unbilledAmount: number;
}

export interface TimeEntryRequest {
  lawyerId: number;
  entryDate: string;
  hours: number;
  description: string;
  hourlyRate: number;
  billable: boolean;
}
