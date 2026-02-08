export interface LawyerResponse {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  taxId?: string;
  email?: string;
  phone?: string;
  active: boolean;
}

export interface CreateLawyerRequest {
  firstName: string;
  lastName: string;
  taxId?: string;
  email?: string;
  phone?: string;
}

export interface UpdateLawyerRequest {
  firstName?: string;
  lastName?: string;
  taxId?: string;
  email?: string;
  phone?: string;
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

export interface LawyerSearchParams {
  page?: number;
  size?: number;
  search?: string;
}
