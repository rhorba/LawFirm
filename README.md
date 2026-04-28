# Law Firm Management System

Comprehensive law firm management application featuring a decoupled monorepo architecture with Spring Boot 3.4 (Java 21) backend and Angular 18 frontend.

## Overview

Enterprise-grade legal practice management system designed to streamline case management, client relationships, financial tracking, and law firm operations. Built on a secure, scalable architecture with role-based access control and comprehensive audit trails. Designed specifically for Moroccan law firms with bilingual support (French/Arabic).

---

## Implementation Status

### ✅ Completed Features (Production Ready)

**Foundation & Infrastructure:**
- ✅ JWT Authentication with refresh tokens (15min access, 30-day refresh, 90-day with remember-me)
- ✅ Role-Based Access Control (RBAC) with 20+ granular permissions
- ✅ User Management (full CRUD with validation, username/email uniqueness)
- ✅ User Profiles (extended profile information)
- ✅ Group Management (user grouping with role assignment, bulk user assignment)
- ✅ Audit Logging (complete request/response tracking, audit trail per entity)
- ✅ JPA Auditing (createdAt, updatedAt, version tracking on all entities)
- ✅ Soft delete with active flag
- ✅ Flyway database migrations (45 migrations, V1–V45)
- ✅ H2 (dev) and PostgreSQL (prod) support
- ✅ Docker containerization (dev & prod configurations)
- ✅ API documentation (Swagger/OpenAPI at `/swagger-ui.html`)
- ✅ Rate limiting filter
- ✅ Logback structured logging (JSON for prod, console for dev)
- ✅ Code quality tools (Checkstyle, SpotBugs, JaCoCo 70% threshold, ESLint, Prettier)

**Case & Dossier Management:**
- ✅ Full CRUD operations with advanced search and filtering
- ✅ Case number auto-generation format: `TYPE/TRIBUNAL/YEAR/SEQUENCE`
- ✅ 7 case statuses with workflow validation (DRAFT → ACTIVE → PENDING → CLOSED → ARCHIVED)
- ✅ Status transition rules enforced per case type
- ✅ 8 advanced filters (year, type, category, tribunal, lawyer, status, date range, text search)
- ✅ Cascading category dropdowns (filtered by selected case type)
- ✅ Debounced search (300ms) with pagination (20 items/page)
- ✅ Financial summary per case (payments, expenses, balance)
- ✅ Case detail view with audit information (created/updated/version)
- ✅ Unified create/edit form with real-time validation
- ✅ Change status modal with optional reason tracking (500 char limit)
- ✅ Case templates (backend + template management UI)
- ✅ Lawyer-to-case assignment (many-to-many)
- ✅ Permission-based UI (CASE_READ, CASE_CREATE, CASE_UPDATE, CASE_DELETE)
- ✅ Bulk delete operations
- ✅ Dark mode support

**Lawyer Management:**
- ✅ Full CRUD operations (create, read, update, deactivate, reactivate)
- ✅ Lawyer profiles (firstName, lastName, unique taxId, email, phone)
- ✅ Active/Inactive status tracking (soft delete via active flag)
- ✅ Search with pagination (configurable: 10, 20, 50, 100 per page)
- ✅ Modal-based create/edit forms
- ✅ Bulk deactivation
- ✅ Case count tracking per lawyer
- ✅ Permission-based UI (LAWYER_READ, LAWYER_CREATE, LAWYER_UPDATE, LAWYER_DELETE)

**Client Management:**
- ✅ Full CRUD operations (create, read, update, delete)
- ✅ Client profiles: CIN (unique), gender, client type, phone, email, address, age
- ✅ Client types: Individual, Corporate, Government
- ✅ CIN uniqueness validation
- ✅ Search with pagination
- ✅ Client-to-case linking
- ✅ Permission-based UI (CLIENT_READ, CLIENT_CREATE, CLIENT_UPDATE, CLIENT_DELETE)

**Reference Data Management:**
- ✅ Tribunals: 125 bilingual entries (full Moroccan court system, French/Arabic names)
- ✅ Case types: 4 types (CIVIL, PENAL, COMMERCIAL, SOCIAL)
- ✅ Case categories: 426 seeded, linked to case types
- ✅ Case statuses: 7 statuses with terminal flags
- ✅ Status workflow mapping (allowed transitions per case type)
- ✅ Global reference data caching (loaded at app initialization)

**Financial Management (Full Stack):**
- ✅ FinancialTransaction entity and repository
- ✅ Transaction types: opening fees, procedure fees, expert fees, etc.
- ✅ Payment methods tracking
- ✅ Financial summary aggregation per case (payments, expenses, balance)
- ✅ Financial Ledger UI (`/financial/ledger`): paginated transaction list, filters, summary cards, Excel export
- ✅ Invoice Management UI (`/financial/invoices`): list, detail, create form, payment modal
- ✅ Invoice→transaction auto-sync on PAID status change
- ✅ Financial tab embedded in case detail view (per-case transactions + summary)
- ✅ FINANCE_* and INVOICE_* permissions seeded and enforced

---

### ⏳ Planned / Not Yet Implemented

The following features are **defined in roadmap but have no implementation yet**:

| Feature | Backend | Frontend | Priority |
|---------|---------|----------|----------|
| Time Tracking & Billing | ❌ | ❌ | High |
| Document Management | ❌ | ❌ | High |
| Deadline & Task Management | ❌ | ❌ | High |
| Calendar & Scheduling | ❌ | ❌ | Medium |
| Reporting & Analytics | ❌ | ❌ | Medium |
| Communication Management (email, SMS) | ❌ | ❌ | Medium |
| Client Conflict Checking | ❌ | ❌ | Medium |
| Client Portal | ❌ | ❌ | Low |
| 2FA / Advanced Security | ❌ | ❌ | Low |
| Mobile App | ❌ | ❌ | Low |
| E-signature Integration | ❌ | ❌ | Low |
| RTL (Arabic UI) | ❌ | ❌ | Low |
| Data Import/Export (bulk CSV/PDF) | ❌ | ❌ | Low |

---

## Technical Stack

- **Backend**: Spring Boot 3.4, Java 21, PostgreSQL/H2, JWT Authentication
- **Frontend**: Angular 18 (standalone components), TanStack Query, Tailwind CSS, Signals
- **Security**: Role-Based Access Control (RBAC) with 20+ granular permissions
- **Authentication**: JWT with refresh tokens
- **Architecture**: Hexagonal/Clean Architecture (backend), layer-based (frontend)
- **Database**: Flyway migrations (62), seeded reference data, optimized indexing
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Observability**: Logback JSON logging, Spring Actuator endpoints
- **Code Quality**: Checkstyle, SpotBugs, JaCoCo (70%), ESLint, Prettier
- **Testing**: JUnit, Mockito, Testcontainers
- **Docker**: Multi-stage builds, Docker Compose for dev and prod

---

## Prerequisites

- **Java**: JDK 21 (set `JAVA_HOME`)
- **Node.js**: v20+ with pnpm installed globally
- **Git**: Run `git config --global core.autocrlf input` (prevents line-ending issues)
- **Docker**: Optional, for containerized development

---

## Quick Start

### Development Mode (H2 Database)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd LawFirm
   ```

2. **Start Backend**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   Backend runs at `http://localhost:8080`

3. **Start Frontend** (new terminal)
   ```bash
   cd frontend
   pnpm install
   pnpm dev
   ```
   Frontend runs at `http://localhost:4200`

4. **Login** using one of the pre-seeded accounts:

   | Username     | Password    | Role / Access Level           |
   |--------------|-------------|-------------------------------|
   | `admin`      | `admin123`  | System Administrator (all)    |
   | `test_user`  | `test123`   | Standard user (USER_READ)     |
   | `test_viewer`| `viewer123` | View-only access              |

### Docker Development Mode (H2 Database)

```bash
# Start all services with hot-reload
docker-compose -f docker-compose.dev.yml up --build

# View logs
docker-compose -f docker-compose.dev.yml logs -f

# Stop all services
docker-compose -f docker-compose.dev.yml down
```

Access:
- Frontend: `http://localhost:4200`
- Backend API: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:lawfirm`, username: `sa`, no password)

### Production Mode (Docker Compose + PostgreSQL)

```bash
docker-compose -f docker-compose.prod.yml up --build
```

Access:
- Frontend: `http://localhost`
- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Project Structure

```
LawFirm/
├── backend/                           # Spring Boot 3.4 + Java 21
│   ├── src/main/java/com/lawfirm/
│   │   ├── domain/model/              # 23 JPA entities
│   │   ├── domain/repository/         # 17 repositories (+ 2 JPA Specifications)
│   │   ├── application/dto/           # 49 DTOs (23 request, 26 response)
│   │   ├── application/mapper/        # 16 MapStruct mappers
│   │   ├── application/service/       # 17 services with business logic
│   │   ├── infrastructure/security/   # JWT, RBAC, filters
│   │   ├── infrastructure/config/     # CORS, Security, Rate limiting
│   │   └── presentation/controller/  # 17 REST controllers
│   ├── src/main/resources/
│   │   ├── db/migration/              # 62 Flyway SQL migrations (V1–V62)
│   │   └── application*.yml
│   └── pom.xml
│
├── frontend/                          # Angular 18 Standalone
│   ├── src/app/
│   │   ├── core/
│   │   │   ├── guards/                # authGuard
│   │   │   ├── interceptors/          # auth, error interceptors
│   │   │   ├── models/                # 8 TypeScript interfaces
│   │   │   └── services/              # AuthService, TokenService, ThemeService, GroupService
│   │   ├── features/
│   │   │   ├── auth/                  # Login, Register (✅)
│   │   │   ├── dashboard/             # KPI dashboard (✅)
│   │   │   ├── layout/                # Layout, Header, Sidebar (✅)
│   │   │   ├── cases/                 # Full case management (✅)
│   │   │   ├── lawyers/               # Lawyer management (✅)
│   │   │   ├── clients/               # Client management (✅)
│   │   │   ├── users/                 # User management (✅)
│   │   │   ├── groups/                # Group management (✅)
│   │   │   ├── audit-logs/            # Audit log viewer (✅)
│   │   │   ├── profile/               # User profile (✅)
│   │   │   ├── settings/              # App settings (✅)
│   │   │   ├── financial/             # Financial ledger + invoices (✅)
│   │   │   ├── documents/             # ⏳ Not yet implemented
│   │   │   ├── tasks/                 # ⏳ Not yet implemented
│   │   │   ├── calendar/              # ⏳ Not yet implemented
│   │   │   └── reports/               # ⏳ Not yet implemented
│   │   └── services/                  # 16 API integration services
│   └── package.json
│
├── docs/plans/                        # Implementation plans (historical)
├── docker-compose.dev.yml
├── docker-compose.prod.yml
├── .gitattributes                     # LF enforcement for .sql and .java files
├── CONTRIBUTING.md
└── CLAUDE.md
```

---

## API Endpoints

### Authentication
```
POST   /api/auth/login              Login and get JWT tokens
POST   /api/auth/refresh            Refresh access token
POST   /api/auth/register           Self-registration
```

### Users & Groups
```
GET    /api/users                   List users (paginated)
POST   /api/users                   Create user
GET    /api/users/:id               Get user
PUT    /api/users/:id               Update user
DELETE /api/users/:id               Delete user

GET    /api/profile                 Get current user profile
PUT    /api/profile                 Update profile

GET    /api/groups                  List groups
POST   /api/groups                  Create group
PUT    /api/groups/:id              Update group
DELETE /api/groups/:id              Delete group
POST   /api/groups/:id/users        Assign users to group
```

### Cases
```
GET    /api/cases                   List cases (advanced filtering + pagination)
POST   /api/cases                   Create case
GET    /api/cases/:id               Get case details
PUT    /api/cases/:id               Update case
DELETE /api/cases/:id               Delete case
POST   /api/cases/:id/status        Change case status (with reason)
POST   /api/cases/export            Export cases
GET    /api/case-templates          List case templates
POST   /api/case-templates          Create case template
```

### Lawyers
```
GET    /api/lawyers                 List lawyers (paginated, searchable)
POST   /api/lawyers                 Create lawyer
GET    /api/lawyers/:id             Get lawyer
PUT    /api/lawyers/:id             Update lawyer
DELETE /api/lawyers/:id             Deactivate lawyer
POST   /api/lawyers/:id/reactivate  Reactivate lawyer
POST   /api/lawyers/bulk-deactivate Bulk deactivate
```

### Clients
```
GET    /api/clients                 List clients (paginated, searchable)
POST   /api/clients                 Create client
GET    /api/clients/:id             Get client
PUT    /api/clients/:id             Update client
DELETE /api/clients/:id             Delete client
```

### Reference Data
```
GET    /api/tribunals               All tribunals (bilingual)
GET    /api/case-types              All case types
GET    /api/case-categories         All categories (filterable by type)
GET    /api/case-statuses           All statuses
```

### Financial
```
GET    /api/financial/transactions               List transactions (paginated, filtered)
POST   /api/financial/transactions               Create a transaction
DELETE /api/financial/transactions/:id           Soft-delete a transaction
GET    /api/financial/transactions/export/excel  Export transactions as Excel
GET    /api/financial/cases/:caseId/transactions All transactions for a case

GET    /api/financial/invoices                   List invoices (paginated)
POST   /api/financial/invoices                   Create invoice with line items
GET    /api/financial/invoices/:id               Get invoice detail
PATCH  /api/financial/invoices/:id/status        Transition invoice status
DELETE /api/financial/invoices/:id               Soft-delete invoice
```

### Audit, Roles & Permissions
```
GET    /api/audit-logs              List audit logs (paginated, filtered)
GET    /api/audit-logs/:id          Get audit log entry
GET    /api/roles                   List roles
GET    /api/permissions             List all permissions
```

---

## Implemented Permissions

The following permissions are **seeded in the database** and enforced at the API level:

| Permission | Description |
|-----------|-------------|
| `USER_READ` | View users |
| `USER_CREATE` | Create users |
| `USER_UPDATE` | Modify users |
| `USER_DELETE` | Delete users |
| `USER_MANAGE` | Full user management |
| `ROLE_READ` | View roles |
| `ROLE_CREATE` | Create roles |
| `ROLE_UPDATE` | Modify roles |
| `ROLE_DELETE` | Delete roles |
| `ROLE_MANAGE` | Full role management |
| `PERMISSION_READ` | View permissions |
| `PERMISSION_MANAGE` | Manage permissions |
| `SYSTEM_MANAGE` | Full system access |
| `CASE_READ` | View cases |
| `CASE_CREATE` | Create cases |
| `CASE_UPDATE` | Modify cases |
| `CASE_DELETE` | Delete cases |
| `LAWYER_READ` | View lawyers |
| `LAWYER_CREATE` | Create lawyers |
| `LAWYER_UPDATE` | Modify lawyers |
| `LAWYER_DELETE` | Deactivate lawyers |
| `CLIENT_READ` | View clients |
| `CLIENT_CREATE` | Create clients |
| `CLIENT_UPDATE` | Modify clients |
| `CLIENT_DELETE` | Delete clients |
| `USER_ROLE_READ` | View user-role assignments |
| `FINANCIAL_READ` | View financial transactions |
| `FINANCIAL_CREATE` | Create financial transactions |
| `FINANCIAL_UPDATE` | Modify/delete financial transactions |
| `FINANCIAL_DELETE` | Hard-delete financial transactions |
| `FINANCIAL_MANAGE` | Full financial management |
| `INVOICE_READ` | View invoices |
| `INVOICE_CREATE` | Create invoices |
| `INVOICE_MANAGE` | Update status and delete invoices |

**Planned (not yet seeded):** `DOCUMENT_*`, `TIME_*`, `BILLING_*`, `CALENDAR_*`, `TASK_*`, `REPORT_*`, `COMMUNICATION_*`, `AUDIT_VIEW`, `BACKUP_MANAGE`

---

## Environment Variables

### Backend
| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | No | `dev` | Active profile (dev/prod) |
| `JWT_SECRET` | Prod only | — | JWT signing secret (256-bit) |
| `DB_HOST` | Prod only | — | PostgreSQL host |
| `DB_PORT` | Prod only | `5432` | PostgreSQL port |
| `DB_NAME` | Prod only | `lawfirm` | Database name |
| `DB_USER` | Prod only | — | Database username |
| `DB_PASSWORD` | Prod only | — | Database password |
| `CORS_ALLOWED_ORIGINS` | Prod only | — | Allowed CORS origins |
| `SMTP_HOST` | No | — | Email server host |
| `SMTP_PORT` | No | — | Email server port |
| `SMTP_USERNAME` | No | — | Email username |
| `SMTP_PASSWORD` | No | — | Email password |

---

## Development Commands

### Backend
```bash
mvn spring-boot:run        # Start dev server (H2)
mvn clean verify           # Full build + tests + quality checks
mvn test                   # Unit tests only
mvn jacoco:report          # Coverage report (target/site/jacoco/index.html)
```

### Frontend
```bash
pnpm dev                   # Start dev server
pnpm build                 # Build for production
pnpm lint                  # Run ESLint
pnpm lint:fix              # Fix linting issues
```

---

## Architecture

### Backend — Hexagonal (Clean) Architecture

```
domain/model/       → JPA entities (no framework dependencies)
domain/repository/  → Repository interfaces + JPA Specifications
application/        → DTOs, MapStruct mappers, service implementations
infrastructure/     → Security (JWT/RBAC), config, filters
presentation/       → REST controllers, global exception handler
```

**Key constraints:**
- No business logic in controllers or entities
- MapStruct only for DTO mapping (manual mapping is prohibited)
- Flyway for all schema changes (no `ddl-auto=update`)
- All endpoints protected with `@PreAuthorize`

### Frontend — Feature-Based Architecture

```
core/               → Auth, token, interceptors, guards, shared models
features/           → Feature-specific standalone components
services/           → API integration services (one per domain)
```

**Key patterns:**
- Standalone components (no NgModules)
- Signals for reactive state
- TanStack Query for server state and caching
- Functional guards and interceptors
- Lazy-loaded routes for performance

---

## Code Quality

### Backend
- **Checkstyle**: Style enforcement (`/backend/checkstyle.xml`)
- **SpotBugs**: Static bug detection
- **JaCoCo**: Coverage threshold 70%

### Frontend
- **ESLint**: Linting with Angular-specific rules
- **Prettier**: Code formatting
- **TypeScript strict mode**: Enabled

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for coding standards and contribution workflow.

---

## License

MIT License

---

## Acknowledgments

Built on enterprise-grade Spring Boot 3.4 and Angular 18. Designed specifically for legal practice management with bilingual support (French/Arabic) for Moroccan law firms.
