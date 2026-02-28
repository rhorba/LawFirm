export type ClientType = 'INDIVIDUAL' | 'CORPORATE' | 'GOVERNMENT';
export type Gender = 'MALE' | 'FEMALE';

export interface ClientSummary {
  id: number;
  fullName: string;
  clientType: ClientType;
  cin?: string;
  taxNumber?: string;
  phone?: string;
  email?: string;
  active: boolean;
  caseCount: number;
}

export interface ClientResponse {
  id: number;
  fullName: string;
  clientType: ClientType;
  firstName?: string;
  lastName?: string;
  phone?: string;
  email?: string;
  address?: string;
  notes?: string;
  cin?: string;
  gender?: Gender;
  dateOfBirth?: string;
  age?: number;
  companyName?: string;
  taxNumber?: string;
  active: boolean;
  caseCount: number;
  createdAt: string;
}

export interface CreateClientRequest {
  clientType: ClientType;
  firstName?: string;
  lastName?: string;
  phone?: string;
  email?: string;
  address?: string;
  notes?: string;
  cin?: string;
  gender?: Gender;
  dateOfBirth?: string;
  companyName?: string;
  taxNumber?: string;
}

export interface UpdateClientRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  email?: string;
  address?: string;
  notes?: string;
  cin?: string;
  gender?: Gender;
  dateOfBirth?: string;
  companyName?: string;
  taxNumber?: string;
}

export interface ClientSearchParams {
  search?: string;
  type?: ClientType;
  page?: number;
  size?: number;
}
