## Project Overview
Law Firm Management System - Comprehensive legal practice management application.
Enterprise-grade monorepo architecture with decoupled backend and frontend.
Backend: Spring Boot 3.4 (Java 21). Frontend: Angular 18.
Domain: Legal case management, client relations, financial tracking, document management.
Focus: Type-safety, audit trails, bilingual support (French/Arabic), RBAC security.

## Rules
- Concise interactions. Priority: clarity over grammar.
- Architecture: Strict Layered Hexagonal. No business logic in Controllers/Entities.
- Mapping: MapStruct only. Manual mapping is prohibited.
- Migrations: Flyway for all schema changes. No hibernate ddl-auto=update.
- Commit messages: Follow Conventional Commits.
- **Line Endings**: Force LF for all `.sql` and `.java` files to prevent Flyway checksum errors on Windows.

## Tech Stack
- **Backend**: Java 21, Spring Boot 3.4, Spring Data JPA, Flyway, MapStruct, Lombok.
- **Frontend**: Angular 18 (Standalone Components, Signals), Tailwind CSS, TanStack Query.
- **Database**: PostgreSQL.
- **Build**: Maven (Global), PNPM (Global).

## Development Commands (Windows Native)
- `mvn spring-boot:run` - Start Backend.
- `pnpm dev` - Start Frontend.
- `mvn clean verify` - Full Backend build & test.
- `pnpm lint` - Frontend linting and formatting.

## Windows Requirements
- **Java Version**: Ensure `%JAVA_HOME%` points to JDK 21.
- **Git Config**: Run `git config --global core.autocrlf input` to handle cross-platform line endings.
- **Database**: PostgreSQL should be running as a local service or via Docker.

---

## Implementation Status

### ✅ Phase 1 — Foundation (Complete)

Core infrastructure: authentication, authorization, user management, group management, audit logging.

### ✅ Phase 2 — Core Legal Domain (Complete)

All core legal operations (cases, lawyers, clients, reference data) are production-ready.

### ✅ Phase 3 — Financial Ledger (Complete)

Full-stack financial management: transactions ledger, invoice management, per-case financial tab, Excel export, invoice→transaction auto-sync.

### ✅ Phase 4 — Extended Features (Complete)

Time Tracking → Documents → Tasks → Calendar → Reporting → Communication — all complete. Only RTL/Arabic UI and 2FA remain.

---

### 🎯 Feature Status

| Feature | Backend | Frontend | Notes |
|---------|---------|----------|-------|
| Authentication (JWT) | ✅ | ✅ | 15min/30day tokens, remember-me |
| User Management | ✅ | ✅ | Full CRUD, pagination |
| User Profiles | ✅ | ✅ | Extended profile info |
| Group Management | ✅ | ✅ | Role assignment, bulk user ops |
| Audit Logging | ✅ | ✅ | Request/response + entity trail |
| Case Management | ✅ | ✅ | Full CRUD, status workflows, filters |
| Lawyer Management | ✅ | ✅ | Full CRUD, pagination, bulk deactivate |
| Client Management | ✅ | ✅ | Full CRUD, CIN uniqueness, case linking |
| Reference Data | ✅ | ✅ | Bilingual tribunals, types, statuses |
| Financial Infrastructure | ✅ | ✅ | Ledger, invoices, per-case tab, Excel export |
| Invoice Management | ✅ | ✅ | Full CRUD, PAID sync to transactions |
| Time Tracking & Billing | ✅ | ✅ | Per-case entries, hourly rate, billable/billed flags, summary widget |
| Document Management | ✅ | ✅ | Per-case upload, 6 categories, MIME whitelist, inline preview, 20MB limit |
| Deadline & Task Management | ✅ | ✅ | Tasks + comments + upcoming dashboard widget |
| Calendar & Scheduling | ✅ | ✅ | Month grid, HEARING/APPOINTMENT/REMINDER events, task deadlines merged |
| Reporting & Analytics | ✅ | ✅ | KPI summary, Chart.js charts, lawyer workload, unpaid invoices, date presets |
| Communication Management | ✅ | ✅ | NOTE/EMAIL/CALL/SMS log, send email (JavaMail, disabled in dev), per-case timeline |
| Client Conflict Checking | ✅ | ✅ | Cross-entity conflict search, case parties, cleared-with-note workflow |
| RTL / Arabic UI | ❌ | ❌ | Planned |
| 2FA / Advanced Security | ❌ | ❌ | Planned |

### 📊 Codebase Metrics (Current)

**Backend:** ~200 Java files
- 30 domain entities, 25 repositories, 70+ DTOs, 22 MapStruct mappers
- 24 services, 24 controllers, 9 custom exceptions
- 77 Flyway migrations (V1–V77)

**Frontend:** Angular 18 standalone
- 40+ components, 21 API services, 14 TypeScript models
- 1 auth guard, 2 HTTP interceptors
- All routes lazy-loaded

**Database tables:** users, user_profiles, roles, permissions, user_roles, groups, user_groups, group_roles, group_permissions, role_permissions, audit_logs, cases, case_types, case_categories, case_statuses, case_type_statuses, case_sequences, case_templates, case_lawyers, lawyers, tribunals, clients, financial_transactions, invoices, invoice_items, tasks, task_comments, calendar_events, conflict_parties, conflict_checks, time_entries, documents, communications

---

### ✅ Completed Foundation (Inherited from LawFirm)

**Phase 1: Project Structure & Configuration**
- Monorepo structure (/backend, /frontend)
- .gitignore and .gitattributes (LF enforcement for cross-platform compatibility)
- Docker Compose configurations (dev with H2, prod with PostgreSQL)
- Environment configuration templates (.env.example)

**Phase 2: Backend Foundation**
- Maven pom.xml with all dependencies:
  - Spring Boot 3.4.1, Java 21
  - Spring Security, JWT (jjwt 0.12.6)
  - MapStruct 1.6.3, Lombok 1.18.34
  - Flyway, PostgreSQL, H2
  - SpringDoc OpenAPI 2.7.0
  - Testcontainers, JaCoCo, Checkstyle, SpotBugs
- Hexagonal architecture package structure
- Spring Boot application with profiles (dev/prod)
- Multi-stage Dockerfiles (prod & dev)

**Phase 3: Security & Authentication Layer**
- Domain entities with JPA Auditing:
  - BaseEntity (id, version, createdAt, updatedAt)
  - User, Role, Permission entities
- 8 Flyway migrations:
  - V1-V5: Table creation with indexes
  - V6: 13 permissions (USER_*, ROLE_*, PERMISSION_*, SYSTEM_MANAGE)
  - V7: 3 roles (ADMIN, USER, MODERATOR)
  - V8: Seed admin user (admin/admin123)
- JPA repositories with JOIN FETCH queries to prevent N+1
- Request/Response DTOs with Bean Validation
- MapStruct mappers (User, Role, Permission)

**Phase 4: Security Configuration**
- JWT implementation:
  - 15-minute access tokens
  - 30-day refresh tokens (90 days with remember me)
  - Secret from JWT_SECRET environment variable
- UserDetailsService & UserPrincipal
- JwtAuthenticationFilter (Bearer token extraction)
- CustomPermissionEvaluator for @PreAuthorize
- SecurityConfig with stateless session management
- CorsConfig with configurable origins
- LoggingFilter for request/response audit
- Logback configuration (JSON for prod, console for dev)
- Checkstyle configuration for code quality

**Phase 5: Service Layer & Controllers**
- Exception handling:
  - ResourceNotFoundException (404)
  - DuplicateResourceException (409)
  - GlobalExceptionHandler for all error cases
- Services:
  - AuthService (login, refresh token)
  - UserService (CRUD with duplicate checks)
- Controllers with OpenAPI annotations:
  - AuthController (/api/auth/*)
  - UserController (/api/users/*) with permission-based access
- Unit tests:
  - UserServiceTest with Mockito and AssertJ
- Documentation:
  - README.md (setup, architecture, features)
  - CONTRIBUTING.md (code standards, workflow)

**Phase 6: Frontend (Angular 18)**
- Project configuration:
  - package.json with Angular 18.2.0, @tanstack/angular-query-experimental
  - Tailwind CSS 3.4.13 with PostCSS
  - ESLint & Prettier with strict rules
  - TypeScript 5.5.2 with strict mode
- Core services:
  - TokenService (localStorage management)
  - AuthService with signals (currentUser, isAuthenticated)
  - UserService (API integration)
- HTTP interceptors:
  - authInterceptor (Bearer token injection)
  - errorInterceptor (401 logout, error logging)
- Guards:
  - authGuard (route protection with returnUrl)
- Components:
  - LoginComponent (reactive form with rememberMe)
  - DashboardComponent (user info, role badges)
  - UserListComponent (pagination, CRUD operations)
- Routing:
  - Lazy-loaded routes with guards
  - Default redirect to dashboard
- Docker configuration:
  - Dockerfile (multi-stage: node build → nginx serve)
  - Dockerfile.dev (pnpm dev server)
  - nginx.conf (SPA routing, /api proxy to backend)

### 📋 Key Features Implemented

**Authentication & Authorization:**
- JWT-based stateless authentication
- Remember me functionality (90-day tokens)
- Role-based access control (RBAC)
- Permission-based authorization at endpoint level
- Automatic token refresh mechanism

**User Management:**
- Complete CRUD operations with validation
- Username/email uniqueness checks
- BCrypt password hashing
- Role assignment and management
- Pagination support

**Security Features:**
- CORS configuration
- CSRF protection
- SQL injection prevention (JPA)
- XSS protection (Spring Security defaults)
- Request/response logging for audit

**Code Quality:**
- Checkstyle enforcement
- SpotBugs static analysis
- JaCoCo code coverage (70% threshold)
- Prettier & ESLint for frontend
- Strict TypeScript compilation

**Database:**
- Flyway migrations with version control
- Seed data for development
- H2 for local development
- PostgreSQL for production
- Optimized queries with JOIN FETCH

**API Documentation:**
- SpringDoc OpenAPI integration
- Swagger UI at /swagger-ui.html
- Bearer token authentication in Swagger

**Monitoring:**
- Spring Boot Actuator endpoints
- Health checks for Docker Compose
- Prometheus-compatible metrics

### 🎯 Quick Start

**Development Mode:**
```bash
# Backend (H2 database)
cd backend
mvn spring-boot:run

# Frontend (separate terminal)
cd frontend
pnpm install
pnpm dev
```

**Production Mode:**
```bash
# Docker Compose with PostgreSQL
docker-compose -f docker-compose.prod.yml up --build
```

**Access Points:**
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console (dev): http://localhost:8080/h2-console

**Default Credentials:**
- Username: `admin`
- Password: `admin123`

### 📊 Seeded Permissions (37+)

**User & Role Management (13):**
USER_READ, USER_CREATE, USER_UPDATE, USER_DELETE, USER_MANAGE, ROLE_READ, ROLE_CREATE, ROLE_UPDATE, ROLE_DELETE, ROLE_MANAGE, PERMISSION_READ, PERMISSION_MANAGE, SYSTEM_MANAGE

**Legal Domain (12):**
CASE_READ, CASE_CREATE, CASE_UPDATE, CASE_DELETE, LAWYER_READ, LAWYER_CREATE, LAWYER_UPDATE, LAWYER_DELETE, CLIENT_READ, CLIENT_CREATE, CLIENT_UPDATE, CLIENT_DELETE

**Financial (11):**
FINANCIAL_READ, FINANCIAL_CREATE, FINANCIAL_UPDATE, FINANCIAL_DELETE, FINANCIAL_MANAGE, INVOICE_READ, INVOICE_CREATE, INVOICE_UPDATE, INVOICE_DELETE, INVOICE_MANAGE, FINANCIAL_EXPORT

**Other (1+):** USER_ROLE_READ

**Role Assignments:**
- ADMIN: All permissions
- MODERATOR: USER_* + ROLE_READ + PERMISSION_READ
- USER: USER_READ only

**Seeded test accounts:**
- `admin` / `admin123` — System Administrator
- `test_user` / `test123` — Standard user
- `test_viewer` / `viewer123` — View-only user

### 🔧 Environment Variables

Required for production:
```bash
# Database
POSTGRES_USER=lawfirm
POSTGRES_PASSWORD=your_secure_password
POSTGRES_DB=lawfirm

# JWT
JWT_SECRET=your_256_bit_secret_key_here

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:4200,https://yourdomain.com

# Email (for notifications)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password

# Optional
SMS_PROVIDER_API_KEY=your_sms_api_key
STORAGE_PATH=/var/lawfirm/documents
BACKUP_PATH=/var/lawfirm/backups
DEFAULT_LANGUAGE=fr
```

### 📦 Project Structure

```
LawFirm/
├── backend/                           # Spring Boot 3.4 + Java 21
│   ├── src/main/java/com/lawfirm/
│   │   ├── domain/                    # Entities & Repositories
│   │   │   ├── case/                  # Case/Dossier entities
│   │   │   ├── client/                # Client entities
│   │   │   ├── lawyer/                # Lawyer entities
│   │   │   ├── financial/             # Financial ledger entities
│   │   │   ├── document/              # Document management entities
│   │   │   ├── task/                  # Task and deadline entities
│   │   │   └── user/                  # User management entities
│   │   ├── application/               # DTOs, Mappers, Services
│   │   │   ├── case/                  # Case management services
│   │   │   ├── client/                # Client management services
│   │   │   ├── lawyer/                # Lawyer management services
│   │   │   ├── financial/             # Financial services
│   │   │   └── reporting/             # Reporting & analytics
│   │   ├── infrastructure/            # Security, Config, Integrations
│   │   │   ├── security/              # JWT, RBAC, audit
│   │   │   └── integration/           # Email, SMS, calendar sync
│   │   └── presentation/              # Controllers, Exception Handling
│   │       ├── case/                  # Case management endpoints
│   │       ├── client/                # Client management endpoints
│   │       ├── lawyer/                # Lawyer management endpoints
│   │       └── financial/             # Financial endpoints
│   ├── src/main/resources/
│   │   ├── db/migration/              # Flyway migrations (V1-V62)
│   │   └── application*.yml           # Configuration
│   └── pom.xml
├── frontend/                          # Angular 18 Standalone
│   ├── src/app/
│   │   ├── core/                      # Services, Guards, Interceptors
│   │   ├── features/                  # Feature modules
│   │   │   ├── auth/                  # Login, registration ✅
│   │   │   ├── dashboard/             # Dashboard ✅
│   │   │   ├── layout/                # Layout, Header, Sidebar ✅
│   │   │   ├── cases/                 # Case management ✅
│   │   │   ├── clients/               # Client management ✅
│   │   │   ├── lawyers/               # Lawyer management ✅
│   │   │   ├── users/                 # User management ✅
│   │   │   ├── groups/                # Group management ✅
│   │   │   ├── audit-logs/            # Audit log viewer ✅
│   │   │   ├── profile/               # User profile ✅
│   │   │   ├── settings/              # App settings ✅
│   │   │   ├── financial/             # Financial ledger + invoices ✅
│   │   │   ├── documents/             # ⏳ Not implemented
│   │   │   ├── tasks/                 # ⏳ Not implemented
│   │   │   ├── calendar/              # ⏳ Not implemented
│   │   │   └── reports/               # ⏳ Not implemented
│   │   └── services/                  # 16 API integration services
│   ├── angular.json
│   ├── tailwind.config.js
│   └── package.json
├── docker-compose.dev.yml             # H2 development setup
├── docker-compose.prod.yml            # PostgreSQL production setup
├── .gitattributes                     # LF enforcement
├── README.md                          # Full documentation
├── CONTRIBUTING.md                    # Contribution guidelines
└── CLAUDE.md                          # AI assistant instructions