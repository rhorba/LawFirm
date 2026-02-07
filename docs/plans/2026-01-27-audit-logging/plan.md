# Implementation Plan - Audit Logging

## Backend Implementation

### Step 1: Database & Entity
- [x] **Create Migration**: `backend/src/main/resources/db/migration/V10__create_audit_logs_table.sql`
- [x] **Create Entity**: `backend/src/main/java/com/boilerplate/domain/model/AuditLog.java`
- [x] **Create Repository**: `backend/src/main/java/com/boilerplate/domain/repository/AuditLogRepository.java`

### Step 2: Event Infrastructure
- [x] **Create Event Record**: `backend/src/main/java/com/boilerplate/application/event/AuditEvent.java`
- [x] **Create Listener**: `backend/src/main/java/com/boilerplate/application/listener/AuditEventListener.java`
- [x] **Create Publisher**: `backend/src/main/java/com/boilerplate/application/service/AuditPublisher.java`

### Step 3: Service Integration (Auth)
- [x] **Modify AuthService**: `backend/src/main/java/com/boilerplate/application/service/AuthService.java` (Inject Publisher, Log Login/Register)

### Step 4: Service Integration (User)
- [x] **Modify UserService**: `backend/src/main/java/com/boilerplate/application/service/UserService.java` (Log CRUD)

### Step 5: Presentation Layer
- [x] **Create DTO**: `backend/src/main/java/com/boilerplate/application/dto/response/AuditLogResponse.java`
- [x] **Create Mapper**: `backend/src/main/java/com/boilerplate/application/mapper/AuditLogMapper.java`
- [x] **Create Service Read Method**: Add `findAll` to `AuditService` (or create `AuditLogService`).
- [x] **Create Controller**: `backend/src/main/java/com/boilerplate/presentation/controller/AuditLogController.java`

## Frontend Implementation

### Step 6: Core Setup
- [x] **Create Model**: `frontend/src/app/core/models/audit-log.model.ts`
- [x] **Create Service**: `frontend/src/app/services/audit-log.service.ts`

### Step 7: UI Components
- [x] **Create Component**: `frontend/src/app/features/audit-logs/audit-log-list/audit-log-list.component.ts` (and `.html`)
- [x] **Configure Routes**: `frontend/src/app/app.routes.ts` (Add lazy route)

### Step 8: Integration & Polish
- [x] **Update Sidebar**: `frontend/src/app/features/layout/sidebar/sidebar.component.ts` (Add menu item)
- [x] **Verify**: Run full stack, generate events, view in UI.
