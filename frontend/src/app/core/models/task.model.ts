export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface TaskResponse {
  id: number;
  title: string;
  description?: string;
  dueDate: string;
  status: TaskStatus;
  priority: TaskPriority;
  caseId: number;
  caseNumber: string;
  assignedLawyerId?: number;
  assignedLawyerName?: string;
  createdByUserId: number;
  createdByUsername: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpcomingTaskResponse {
  id: number;
  title: string;
  dueDate: string;
  status: TaskStatus;
  priority: TaskPriority;
  caseId: number;
  caseNumber: string;
  assignedLawyerId?: number;
  assignedLawyerName?: string;
}

export interface TaskCommentResponse {
  id: number;
  taskId: number;
  authorUserId: number;
  authorUsername: string;
  content: string;
  createdAt: string;
}

export interface TaskRequest {
  title: string;
  description?: string;
  dueDate: string;
  status: TaskStatus;
  priority: TaskPriority;
  assignedLawyerId?: number;
}

export interface TaskCommentRequest {
  content: string;
}
