export type DocumentCategory =
  | 'CONTRAT'
  | 'JUGEMENT'
  | 'ASSIGNATION'
  | 'COURRIER'
  | 'REQUETE'
  | 'AUTRE';

export const DOCUMENT_CATEGORY_LABELS: Record<DocumentCategory, string> = {
  CONTRAT: 'Contrat',
  JUGEMENT: 'Jugement',
  ASSIGNATION: 'Assignation',
  COURRIER: 'Courrier',
  REQUETE: 'Requête',
  AUTRE: 'Autre',
};

export interface DocumentResponse {
  id: number;
  caseId: number;
  caseNumber: string;
  title: string;
  description?: string;
  category: DocumentCategory;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  uploadedByUsername: string;
  createdAt: string;
}
