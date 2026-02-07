# Audit Logging Feature Design

## 1. Overview
A system-wide activity logging mechanism to track "Who, What, When" for administrative and security auditing. This feature uses a decoupled Event-Driven architecture on the backend and a standalone administrative data table on the frontend.

## 2. Backend Design

### Architecture
**Strategy**: Option 2 (Spring Events)
- **Decoupling**: Business services trigger events; a separate listener handles persistence.
- **Context**: Captures SecurityContext (User, IP) automatically where possible.

### Data Model (`AuditLog`)
Extends `BaseEntity`.
- **`userId`** (`Long`): ID of the actor (nullable for system events).
- **`username`** (`String`): Snapshot of the username.
- **`action`** (`String`): Enum-like string (e.g., `USER_LOGIN`, `USER_CREATED`).
- **`resource`** (`String`): Target domain (e.g., `USER`, `AUTH`).
- **`resourceId`** (`String`): ID of the affected resource.
- **`metadata`** (`String`): JSON blob for additional details.
- **`ipAddress`** (`String`): Client IP.

### Components
1.  **`AuditEvent`**: Java Record carrying payload.
2.  **`AuditLogRepository`**: Standard JPA Repository.
3.  **`AuditListener`**: `@EventListener` component to save logs asynchronously/synchronously.
4.  **`AuditPublisher`**: Helper service to simplify event publication in business logic.
5.  **Integration**:
    - `AuthService`: Log Login/Register.
    - `UserService`: Log Create/Update/Delete.

### Database
- Migration: `V10__create_audit_logs_table.sql`.

## 3. Frontend Design

### Data Model
- **`AuditLog`** (Interface): Matches backend DTO.

### UI Components
- **`AuditLogListComponent`**:
    - Route: `/dashboard/audit-logs`.
    - Features: Paginated table, sortable columns, visual badges for actions.
    - Permissions: `SYSTEM_MANAGE` or specific `AUDIT_READ`.

### Integration
- **`AuditService`**: HTTP client for `/api/audit-logs`.
- **Sidebar**: New entry under "Administration".
