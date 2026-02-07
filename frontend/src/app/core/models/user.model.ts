import { GroupResponse } from './group.model';

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  enabled: boolean;
  roles: RoleResponse[]; // Keep for backward compatibility - computed from groups
  groups: GroupResponse[]; // New field - primary source
  deletedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RoleResponse {
  id: number;
  name: string;
  description: string;
  permissions: PermissionResponse[];
}

export interface PermissionResponse {
  id: number;
  name: string;
  description: string;
  resource: string;
  action: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
}

export interface UpdateUserRequest {
  username?: string;
  email?: string;
  password?: string;
  enabled?: boolean;
}

export interface BulkActionRequest {
  userIds: number[];
}

export interface BulkStatusRequest {
  userIds: number[];
  enabled: boolean;
}

export interface BulkActionResponse {
  affected: number;
  message: string;
}

export interface UserSearchParams {
  search?: string;
  role?: string;
  enabled?: boolean;
  showDeleted?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}
