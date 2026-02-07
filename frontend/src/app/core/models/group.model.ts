import { RoleResponse, UserResponse } from './user.model';

export interface GroupResponse {
  id: number;
  name: string;
  description: string;
  roles: RoleResponse[];
  users?: UserResponse[]; // Optional, only populated in detail view
  userCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface GroupRequest {
  name: string;
  description: string;
  roleIds: number[];
}

export interface GroupAssignUsersRequest {
  userIds: number[];
}
