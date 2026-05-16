export interface ReportSummaryResponse {
  totalCases: number;
  openCases: number;
  overdueTasks: number;
  totalRevenue: number;
  unbilledHours: number;
  unbilledAmount: number;
  totalDocuments: number;
  pendingInvoices: number;
}

export interface ChartDataResponse {
  labels: string[];
  values: number[];
  secondaryValues: number[];
}

export interface LawyerWorkloadEntry {
  lawyerId: number;
  lawyerName: string;
  caseCount: number;
  totalHours: number;
  billableHours: number;
  billableAmount: number;
}

export interface UnpaidInvoiceEntry {
  invoiceId: number;
  invoiceNumber: string;
  caseId: number;
  caseNumber: string;
  totalAmount: number;
  dueDate?: string;
  status: string;
}
