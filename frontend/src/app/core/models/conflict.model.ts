export type PartyType =
  | 'CLIENT'
  | 'OPPOSING_PARTY'
  | 'OPPOSING_COUNSEL'
  | 'WITNESS'
  | 'RELATED_ENTITY';

export type CheckResult = 'CLEAR' | 'CONFLICT_FOUND';

export interface ConflictPartyResponse {
  id: number;
  caseId: number;
  caseNumber: string;
  partyType: PartyType;
  name: string;
  notes?: string;
  createdAt: string;
}

export interface ConflictMatchResponse {
  source: 'CLIENT' | 'CASE_PARTY' | 'CASE';
  matchedName: string;
  partyType?: PartyType;
  caseId?: number;
  caseNumber?: string;
  clientId?: number;
}

export interface ConflictCheckResultResponse {
  checkId: number;
  searchName: string;
  hasConflict: boolean;
  matches: ConflictMatchResponse[];
  performedAt: string;
}

export interface ConflictCheckResponse {
  id: number;
  searchName: string;
  result: CheckResult;
  clearedNote?: string;
  performedByUsername: string;
  clearedByUsername?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ConflictPartyRequest {
  partyType: PartyType;
  name: string;
  notes?: string;
}

export interface ConflictCheckRequest {
  searchName: string;
}

export interface ConflictClearRequest {
  clearedNote: string;
}
