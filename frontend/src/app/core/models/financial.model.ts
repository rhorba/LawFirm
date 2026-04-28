export type TransactionDirection = 'REVENUE' | 'EXPENSE';

export type OperationType =
  | 'OPENING_FEE'
  | 'PROCEDURE_FEE'
  | 'INTERVENTION_FEE'
  | 'EXPERT_FEE'
  | 'DOCUMENT_FEE'
  | 'NOTIFICATION_FEE'
  | 'JUDICIAL_TAX'
  | 'OTHER';

export type PaymentMode = 'CHECK' | 'TRANSFER' | 'CASH' | 'CREDIT_CARD' | 'MONEY_ORDER';

export type InvoiceStatus = 'DRAFT' | 'SENT' | 'PAID' | 'CANCELLED';

export interface TransactionResponse {
  id: number;
  caseId: number;
  caseNumber: string;
  invoiceId?: number;
  direction: TransactionDirection;
  operationType: OperationType;
  paymentMode?: PaymentMode;
  amount: number;
  paymentDate?: string;
  paymentReference?: string;
  accountNumber?: string;
  description?: string;
  createdAt: string;
}

export interface FinancialSummary {
  totalRevenue: number;
  totalExpenses: number;
  balance: number;
  transactionCount: number;
}

export interface TransactionRequest {
  caseId: number;
  direction: TransactionDirection;
  operationType: OperationType;
  paymentMode?: PaymentMode;
  amount: number;
  paymentDate?: string;
  paymentReference?: string;
  accountNumber?: string;
  description?: string;
}

export interface FinancialFilter {
  caseId?: number;
  clientId?: number;
  direction?: TransactionDirection;
  operationType?: OperationType;
  dateFrom?: string;
  dateTo?: string;
}

export interface InvoiceItemResponse {
  id: number;
  description: string;
  operationType: OperationType;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface InvoiceResponse {
  id: number;
  caseId: number;
  caseNumber: string;
  invoiceNumber: string;
  issueDate: string;
  dueDate?: string;
  status: InvoiceStatus;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  notes?: string;
  items: InvoiceItemResponse[];
  createdAt: string;
}

export interface InvoiceItemRequest {
  description: string;
  operationType: OperationType;
  quantity: number;
  unitPrice: number;
}

export interface InvoiceRequest {
  caseId: number;
  issueDate: string;
  dueDate?: string;
  taxAmount?: number;
  notes?: string;
  items: InvoiceItemRequest[];
}

export interface InvoiceStatusRequest {
  status: InvoiceStatus;
  paymentMode?: PaymentMode;
  paymentDate?: string;
  paymentReference?: string;
}
