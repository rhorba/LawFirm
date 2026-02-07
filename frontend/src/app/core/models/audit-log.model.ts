export interface AuditLog {
  id: number;
  userId: number;
  username: string;
  action: string;
  resource: string;
  resourceId: string;
  metadata: string;
  ipAddress: string;
  createdAt: string;
}
