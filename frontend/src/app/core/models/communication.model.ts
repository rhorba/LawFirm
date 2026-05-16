export type CommunicationType = 'NOTE' | 'EMAIL_SENT' | 'EMAIL_RECEIVED' | 'CALL' | 'SMS';
export type CommunicationDirection = 'INBOUND' | 'OUTBOUND' | 'INTERNAL';

export const COMM_TYPE_LABELS: Record<CommunicationType, string> = {
  NOTE: 'Note',
  EMAIL_SENT: 'Email envoyé',
  EMAIL_RECEIVED: 'Email reçu',
  CALL: 'Appel',
  SMS: 'SMS',
};

export interface CommunicationResponse {
  id: number;
  caseId: number;
  caseNumber: string;
  commType: CommunicationType;
  direction: CommunicationDirection;
  subject?: string;
  body?: string;
  recipientEmail?: string;
  recipientPhone?: string;
  sentByUsername: string;
  createdAt: string;
}

export interface CommunicationRequest {
  commType: CommunicationType;
  direction: CommunicationDirection;
  subject?: string;
  body?: string;
  recipientEmail?: string;
  recipientPhone?: string;
}

export interface EmailSendRequest {
  to: string;
  subject: string;
  body: string;
}
