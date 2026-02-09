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
- **Frontend**: React 18+ (Vite) OR Angular 18+, Tailwind CSS, TanStack Query.
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

### 🚀 Foundation Phase - Complete

Core infrastructure implementation completed. Authentication, authorization, user management, and audit logging are production-ready.

### 📋 Law Firm Domain - In Active Development

**✅ COMPLETED (Production Ready):**
- Case/Dossier Management (full CRUD, advanced search, status workflows)
- Lawyer Management (full CRUD, search, active/inactive tracking)
- Reference Data (tribunals, case types, categories, statuses - all bilingual)
- Financial Infrastructure (entities, repositories, summary aggregation)

**🔄 NEXT UP:**
- Client Management (profiles, relationships, conflict checking)

See detailed implementation status and feature roadmap below.

### 🎯 Law Firm Feature Roadmap

**Priority 1: Core Legal Operations**
1. Case/Dossier Management - Full lifecycle tracking with unique numbering
2. Client Management - Comprehensive client profiles with conflict checking
3. Lawyer Management - Attorney profiles with specializations and workload tracking
4. Reference Data - Tribunals (bilingual), Case Types, Court rules

**Priority 2: Financial Management**
5. Financial Ledger - Case-based expense/revenue tracking
6. Time Tracking & Billing - Billable hours, invoicing, retainer management
7. Payment Processing - Multiple payment methods, trust accounting

**Priority 3: Document & Task Management**
8. Document Management - Secure storage, versioning, templates, OCR
9. Deadline & Task Management - Court dates, filing deadlines, task assignment
10. Calendar & Scheduling - Multi-lawyer scheduling, conflict detection

**Priority 4: Communication & Reporting**
11. Communication Management - Email integration, client portal, SMS notifications
12. Reporting & Analytics - Client statistics, lawyer performance, financial reports, KPI dashboard
13. Notifications & Alerts - Deadline reminders, payment alerts, task notifications

**Priority 5: Advanced Features**
14. Data Import/Export - CSV/PDF export, bulk import, automated backups
15. Internationalization - Multi-language (French/Arabic/English), RTL support
16. Mobile Access - Responsive design, mobile app (planned), offline capability
17. Audit & Compliance - Complete modification history, regulatory compliance tracking
18. Conflict Management - Conflict checking engine, waiver documentation
19. Advanced Security - 2FA, IP whitelisting, data encryption
20. Performance Optimization - Caching, lazy loading, query optimization
21. Technical Integrations - Calendar sync, e-signature, third-party APIs

### ✅ RECENTLY COMPLETED - Case & Lawyer Management

**Backend (62 Java files, 15 migrations):**
- 8 Domain entities (Case, Lawyer, Tribunal, CaseType, CaseCategory, CaseStatus, CaseSequence, FinancialTransaction)
- 13 DTOs (5 request, 8 response) with Bean Validation
- 6 MapStruct mappers for entity-DTO conversion
- 5 services with business logic (CaseService, LawyerService, TribunalService, CaseSequenceService, CaseNumberGenerator)
- 3 REST controllers with 14 endpoints (CaseController, LawyerController, TribunalController)
- 10 JPA repositories with custom queries and specifications
- 2 custom exceptions (InvalidCaseNumberFormatException, InvalidStatusTransitionException)
- 15 Flyway migrations (V17-V31)
  - Tribunals table + 9 seeded tribunals (bilingual FR/AR)
  - Case types table + 5 seeded types (CIVIL, PENAL, COMMERCIAL, SOCIAL, ADMIN)
  - Case categories table + 17 seeded categories
  - Case statuses table + 7 seeded statuses (DRAFT → CLOSED)
  - Case type-status mapping (allowed transitions)
  - Lawyers table with tax ID and contact info
  - Case sequences table for auto-numbering
  - Cases table with all relationships
  - Financial transactions table
  - 8 new permissions (CASE_*, LAWYER_*)
- Case number auto-generation: {TYPE}/{TRIBUNAL}/{YEAR}/{SEQUENCE}
- Status workflow validation (allowed transitions per case type)
- Financial summary aggregation (payments, expenses, balance)

**Frontend (17 Angular components, 9 services):**
- Case List component (378 lines TS + 380 lines HTML)
  - Advanced filtering: year, type, category, tribunal, lawyer, status, date range, search
  - Cascading category dropdown (filtered by selected case type)
  - Debounced search (300ms), pagination (20/page), bulk operations
  - Color-coded status badges (7 statuses)
  - Permission-based UI (CASE_READ, CASE_CREATE, CASE_UPDATE, CASE_DELETE)
- Case Detail component (120 lines TS + 220 lines HTML)
  - Full case information display
  - Financial summary card (payments, expenses, balance)
  - Audit information (created/updated timestamps, version)
  - Quick actions sidebar (change status, edit, delete)
- Case Form component (178 lines TS + 222 lines HTML)
  - Unified create/edit form
  - Cascading dropdowns (type filters categories)
  - Form validation with real-time feedback
  - Initial status selection (create mode)
  - Textarea inputs with character limits
- Change Status Modal component (102 lines TS + 96 lines HTML)
  - Load available statuses for case type
  - Optional reason field (500 char limit)
  - Escape key to close
- Lawyer List component (205 lines TS + 281 lines HTML)
  - Search, pagination, bulk operations
  - Active/Inactive status tracking
  - Create/Edit modal form
- Lawyer Form component (106 lines TS + 171 lines HTML)
  - Modal form with validation
  - Required: firstName, lastName, taxId
  - Optional: email, phone
- 9 TypeScript services (Case, Lawyer, Tribunal, CaseType, CaseCategory, CaseStatus, ReferenceData)
- Global reference data caching (APP_INITIALIZER loads on startup)
- 3 new routes: /cases, /cases/:id, /cases/:id/edit, /cases/new, /lawyers
- Dark mode support across all components
- Lazy-loaded routes (5.06 kB compressed per route)

**Build Metrics:**
- Production bundle: 354.45 kB initial / 97.28 kB compressed
- Case list chunk: 20.53 kB raw / 4.81 kB compressed
- Case detail chunk: 18.40 kB raw / 4.77 kB compressed
- Case form chunk: 15.74 kB raw / 3.64 kB compressed
- Lawyer list chunk: 25.72 kB raw / 5.64 kB compressed

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

### 📊 Default Permissions

The system includes 13 pre-configured permissions:
- USER_READ, USER_CREATE, USER_UPDATE, USER_DELETE, USER_MANAGE
- ROLE_READ, ROLE_CREATE, ROLE_UPDATE, ROLE_DELETE, ROLE_MANAGE
- PERMISSION_READ, PERMISSION_MANAGE
- SYSTEM_MANAGE

**Role Assignments:**
- ADMIN: All permissions
- MODERATOR: USER_* + ROLE_READ + PERMISSION_READ
- USER: USER_READ only

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
│   │   ├── db/migration/              # Flyway migrations (V1-V16+)
│   │   ├── templates/                 # Email/document templates
│   │   └── application*.yml           # Configuration
│   └── pom.xml
├── frontend/                          # Angular 18 Standalone
│   ├── src/app/
│   │   ├── core/                      # Services, Guards, Interceptors
│   │   ├── features/                  # Feature modules
│   │   │   ├── auth/                  # Login, registration
│   │   │   ├── dashboard/             # KPI dashboard
│   │   │   ├── cases/                 # Case management
│   │   │   ├── clients/               # Client management
│   │   │   ├── lawyers/               # Lawyer management
│   │   │   ├── financial/             # Financial tracking
│   │   │   ├── documents/             # Document management
│   │   │   ├── tasks/                 # Task & deadline management
│   │   │   ├── calendar/              # Calendar & scheduling
│   │   │   ├── reports/               # Reporting & analytics
│   │   │   └── admin/                 # System administration
│   │   └── shared/                    # Shared components, pipes, directives
│   ├── angular.json
│   ├── tailwind.config.js
│   └── package.json
├── docker-compose.dev.yml             # H2 development setup
├── docker-compose.prod.yml            # PostgreSQL production setup
├── .gitattributes                     # LF enforcement
├── README.md                          # Full documentation
├── CONTRIBUTING.md                    # Contribution guidelines
└── CLAUDE.md                          # AI assistant instructions