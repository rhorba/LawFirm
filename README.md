# Law Firm Management System

Comprehensive law firm management application featuring a decoupled monorepo architecture with Spring Boot 3.4 (Java 21) backend and Angular 18 frontend.

## Overview

Enterprise-grade legal practice management system designed to streamline case management, client relationships, financial tracking, and law firm operations. Built on a secure, scalable architecture with role-based access control and comprehensive audit trails. Designed specifically for Moroccan law firms with bilingual support (French/Arabic).

---

## Implementation Status

### ✅ Completed Features (Production Ready)

**Foundation & Infrastructure**
- JWT Authentication with refresh tokens (15min access, 30-day refresh, 90-day with remember-me)
- Role-Based Access Control (RBAC) with 40+ granular permissions
- User Management (full CRUD with validation, username/email uniqueness)
- User Profiles (extended profile information)
- Group Management (user grouping with role assignment, bulk user assignment)
- Audit Logging (complete request/response tracking, audit trail per entity)
- JPA Auditing (createdAt, updatedAt, version on all entities)
- Soft delete support
- 77 Flyway migrations (V1–V77)
- H2 (dev) and PostgreSQL (prod) support
- Docker containerization (dev & prod configurations)
- API documentation (Swagger/OpenAPI at `/swagger-ui.html`)
- Logback structured logging (JSON for prod, console for dev)
- Code quality tools (Checkstyle, SpotBugs, JaCoCo 70%, ESLint, Prettier)

**Case & Dossier Management**
- Full CRUD with advanced search and filtering
- Case number auto-generation: `TYPE/TRIBUNAL/YEAR/SEQUENCE`
- 7 case statuses with workflow validation
- Status transition rules enforced per case type
- 8 advanced filters (year, type, category, tribunal, lawyer, status, date range, text search)
- Cascading category dropdowns (filtered by case type)
- Debounced search (300ms) with pagination
- Case detail view with 8 embedded tabs (see below)
- Change status modal with optional reason
- Case templates
- Lawyer-to-case and Client-to-case assignment

**Lawyer Management**
- Full CRUD (create, read, update, deactivate, reactivate)
- Lawyer profiles (firstName, lastName, unique taxId, email, phone)
- Active/Inactive status tracking
- Search with pagination and bulk deactivation

**Client Management**
- Full CRUD with CIN uniqueness validation
- Client types: Individual, Corporate, Government
- Client-to-case linking

**Reference Data**
- 125 bilingual tribunals (full Moroccan court system, French/Arabic)
- 4 case types (CIVIL, PENAL, COMMERCIAL, SOCIAL)
- 426 seeded case categories, linked to types
- 7 case statuses with terminal flags and transition mapping

**Financial Management**
- Transaction ledger with types (fees, expenses, etc.) and payment method tracking
- Financial summary per case (payments, expenses, balance)
- Financial Ledger UI (`/financial/ledger`): paginated list, filters, summary cards, Excel export
- Invoice Management (`/financial/invoices`): list, detail, create form, payment modal
- Invoice→transaction auto-sync on PAID status change
- Per-case financial tab embedded in case detail

**Time Tracking & Billing**
- Per-case time entries with hourly rate, billable/billed flags, and invoice link
- 4-card summary widget: total hours, billable, billed, unbilled (hours + MAD amounts)
- Inline entry form: lawyer selector, date, hours (step 0.25), rate (step 50), description, billable toggle
- Mark-as-billed with optional invoice link
- Permissions: TIME_READ / TIME_CREATE / TIME_UPDATE / TIME_DELETE / TIME_MANAGE

**Deadline & Task Management**
- Per-case tasks with priority, due date, assigned lawyer
- Task comments with author tracking
- Status workflow: TODO → IN_PROGRESS → DONE / CANCELLED
- Upcoming tasks dashboard widget (next 14 days)
- Permissions: TASK_READ / TASK_CREATE / TASK_UPDATE / TASK_DELETE / TASK_MANAGE

**Calendar & Scheduling**
- Month-grid calendar view (Monday-first, `/calendar`)
- Three event types: HEARING (red), APPOINTMENT (blue), REMINDER (purple)
- Task due dates merged into calendar as TASK chips (amber)
- Click day to pre-fill event date; click event chip to view/edit/delete
- Event form: title, type, start/end datetime, all-day toggle, optional case link
- Permissions: CALENDAR_READ / CALENDAR_CREATE / CALENDAR_UPDATE / CALENDAR_DELETE

**Client Conflict Checking**
- Dedicated conflict search across clients, case parties, and case numbers
- Case parties table: OPPOSING_PARTY, OPPOSING_COUNSEL, WITNESS, RELATED_ENTITY, CLIENT types
- Conflict check audit trail with performed-by and cleared-by tracking
- Cleared-with-note workflow for documented resolutions
- Parties tab embedded in case detail
- Permissions: CONFLICT_READ / CONFLICT_CREATE / CONFLICT_MANAGE

**Document Management**
- Per-case document upload with filesystem storage (`~/lawfirm-documents`)
- 6 legal categories: CONTRAT, JUGEMENT, ASSIGNATION, COURRIER, REQUETE, AUTRE
- MIME whitelist: PDF, Word, Excel, images (JPEG/PNG/GIF/WebP), plain text
- 20 MB file size limit (configurable)
- Inline preview for PDF and images (`?inline=true`); attachment download for all others
- Search by title/filename; filter by category
- Documents tab embedded in case detail
- Permissions: DOCUMENT_READ / DOCUMENT_CREATE / DOCUMENT_DELETE / DOCUMENT_MANAGE

**Reporting & Analytics**
- KPI summary: total cases, open cases, revenue, unbilled hours/amount, overdue tasks, pending invoices
- Three Chart.js charts: cases by status (doughnut), cases by month (bar), revenue vs expenses per month (line)
- Lawyer workload table: total hours, billable hours, billable amount per active lawyer
- Top 10 unpaid invoices table with invoice number, case, amount, and due date
- Date range presets: this month / last 3 months / last 12 months / custom from–to
- Print support (non-chart elements visible in print view)
- Permissions: REPORT_READ (seeded to ADMIN + MODERATOR)

**Communication Management**
- Per-case communication timeline with log form and email send form
- Five communication types: NOTE, EMAIL_SENT, EMAIL_RECEIVED, CALL, SMS
- Three directions: INBOUND, OUTBOUND, INTERNAL
- Outbound email via JavaMailSender (disabled by default in dev; set `app.mail.enabled=true` + SMTP config for prod)
- Timeline shows type/direction badges, recipient email/phone, author, timestamp
- Communications tab embedded in case detail (9th tab)
- Permissions: COMMUNICATION_READ / COMMUNICATION_CREATE / COMMUNICATION_DELETE / COMMUNICATION_MANAGE

---

### 🗂 Case Detail Tabs

Each case detail page has 9 permission-gated tabs:

| Tab | Permission | Description |
|-----|-----------|-------------|
| Détails | — | Core case fields, audit info |
| Enfants | — | Linked child cases |
| Historique | — | Full audit change history |
| Finances | FINANCIAL_READ | Per-case ledger tab |
| Tâches | TASK_READ | Tasks + comments |
| Parties | CONFLICT_READ | Case parties (opposing counsel, witnesses, etc.) |
| Temps | TIME_READ | Time entries + billing summary |
| Documents | DOCUMENT_READ | File upload, preview, download |
| Communications | COMMUNICATION_READ | Communication log + email sender |

---

### ⏳ Planned / Not Yet Implemented

| Feature | Backend | Frontend |
|---------|---------|----------|
| RTL / Arabic UI | ❌ | ❌ |
| 2FA / Advanced Security | ❌ | ❌ |
| Client Portal | ❌ | ❌ |
| Mobile App | ❌ | ❌ |

---

## Technical Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.4, Java 21 |
| Frontend | Angular 18 (standalone components, Signals) |
| Database | PostgreSQL (prod), H2 (dev) |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway (V1–V74) |
| Mapping | MapStruct |
| Security | Spring Security, JWT (jjwt 0.12.6) |
| Styling | Tailwind CSS 3.4 |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Testing | JUnit 5, Mockito, Testcontainers |
| Quality | Checkstyle, SpotBugs, JaCoCo 70%, ESLint, Prettier |
| Build | Maven (backend), pnpm (frontend) |
| Deploy | Docker + Docker Compose |

---

## Prerequisites

- **Java**: JDK 21 (set `JAVA_HOME`)
- **Node.js**: v20+ with pnpm installed globally
- **Git**: Run `git config --global core.autocrlf input` (prevents line-ending issues on Windows)
- **Database**: PostgreSQL (for prod) or H2 included (for dev)
- **Docker**: Optional, for containerized development

---

## Quick Start

### Development Mode (H2 Database)

```bash
# 1. Start backend
cd backend
mvn spring-boot:run
# API available at http://localhost:8080

# 2. Start frontend (new terminal)
cd frontend
pnpm install
pnpm dev
# UI available at http://localhost:4200
```

**Default credentials:**

| Username      | Password    | Role                        |
|---------------|-------------|-----------------------------|
| `admin`       | `admin123`  | System Administrator (all)  |
| `test_user`   | `test123`   | Standard user (USER_READ)   |
| `test_viewer` | `viewer123` | View-only access            |

### Docker Development Mode

```bash
docker-compose -f docker-compose.dev.yml up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console |

### Production Mode (PostgreSQL)

```bash
docker-compose -f docker-compose.prod.yml up --build
```

---

## Project Structure

```
LawFirm/
├── backend/
│   ├── src/main/java/com/lawfirm/
│   │   ├── domain/model/              # 30 JPA entities
│   │   ├── domain/repository/         # 25 repositories
│   │   ├── application/dto/           # 70+ DTOs (request + response)
│   │   ├── application/mapper/        # 22 MapStruct mappers
│   │   ├── application/service/       # 24 services
│   │   ├── infrastructure/security/   # JWT, RBAC, filters
│   │   └── presentation/controller/  # 24 REST controllers
│   ├── src/main/resources/
│   │   ├── db/migration/              # 77 Flyway migrations (V1–V77)
│   │   └── application*.yml
│   └── pom.xml
│
├── frontend/
│   ├── src/app/
│   │   ├── core/
│   │   │   ├── guards/                # authGuard
│   │   │   ├── interceptors/          # auth, error
│   │   │   ├── models/                # 12 TypeScript interfaces
│   │   │   └── services/              # AuthService, TokenService, ThemeService
│   │   ├── features/
│   │   │   ├── auth/                  # Login, Register ✅
│   │   │   ├── dashboard/             # KPI dashboard ✅
│   │   │   ├── layout/                # Layout, Header, Sidebar ✅
│   │   │   ├── cases/                 # Full case management + 9-tab detail ✅
│   │   │   ├── lawyers/               # Lawyer management ✅
│   │   │   ├── clients/               # Client management ✅
│   │   │   ├── users/                 # User management ✅
│   │   │   ├── groups/                # Group management ✅
│   │   │   ├── audit-logs/            # Audit log viewer ✅
│   │   │   ├── profile/               # User profile ✅
│   │   │   ├── settings/              # App settings ✅
│   │   │   ├── financial/             # Ledger + invoices ✅
│   │   │   ├── calendar/              # Month-grid calendar ✅
│   │   │   ├── conflicts/             # Conflict check page ✅
│   │   │   ├── documents/             # ⏳ (per-case tab done, standalone page planned)
│   │   │   ├── tasks/                 # ⏳ (per-case tab done, standalone page planned)
│   │   │   └── reports/               # Reporting & Analytics ✅
│   │   └── services/                  # 21 API integration services
│   └── package.json
│
├── docker-compose.dev.yml
├── docker-compose.prod.yml
├── .gitattributes                     # LF enforcement for .sql and .java
├── CLAUDE.md
└── README.md
```

---

## API Endpoints

### Authentication
```
POST   /api/auth/login              Login — returns access + refresh tokens
POST   /api/auth/refresh            Refresh access token
POST   /api/auth/register           Self-registration
```

### Users & Groups
```
GET    /api/users                   List users (paginated)
POST   /api/users                   Create user
GET    /api/users/:id               Get user
PUT    /api/users/:id               Update user
DELETE /api/users/:id               Soft-delete user

GET    /api/profile                 Current user profile
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
POST   /api/cases/:id/status        Change case status (with optional reason)
POST   /api/cases/:id/assign-client Assign/unassign client
GET    /api/cases/:id/children      Child cases
GET    /api/cases/:id/history       Audit log for case

GET    /api/case-templates          List case templates
POST   /api/case-templates          Create case template
```

### Lawyers & Clients
```
GET    /api/lawyers                 List lawyers (paginated, searchable)
POST   /api/lawyers                 Create lawyer
GET    /api/lawyers/:id             Get lawyer
PUT    /api/lawyers/:id             Update lawyer
DELETE /api/lawyers/:id             Deactivate lawyer
POST   /api/lawyers/:id/reactivate  Reactivate lawyer
POST   /api/lawyers/bulk-deactivate Bulk deactivate

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
GET    /api/financial/transactions                List transactions (paginated, filtered)
POST   /api/financial/transactions                Create transaction
DELETE /api/financial/transactions/:id            Delete transaction
GET    /api/financial/transactions/export/excel   Export as Excel
GET    /api/financial/cases/:caseId/transactions  Per-case transactions

GET    /api/financial/invoices                    List invoices
POST   /api/financial/invoices                    Create invoice with line items
GET    /api/financial/invoices/:id                Get invoice detail
PATCH  /api/financial/invoices/:id/status         Transition invoice status
DELETE /api/financial/invoices/:id                Delete invoice
```

### Time Tracking
```
GET    /api/cases/:caseId/time-entries            List entries for case
GET    /api/cases/:caseId/time-entries/summary    Hours + amounts summary
POST   /api/cases/:caseId/time-entries            Create entry
PUT    /api/time-entries/:id                      Update entry
DELETE /api/time-entries/:id                      Delete entry
POST   /api/time-entries/:id/bill                 Mark as billed (optional ?invoiceId=)
```

### Tasks
```
GET    /api/cases/:caseId/tasks                   List tasks for case
POST   /api/cases/:caseId/tasks                   Create task
GET    /api/tasks/upcoming                        Upcoming tasks (next 14 days)
PUT    /api/tasks/:id                             Update task
DELETE /api/tasks/:id                             Delete task
GET    /api/tasks/:id/comments                    Task comments
POST   /api/tasks/:id/comments                    Add comment
DELETE /api/task-comments/:id                     Delete comment
```

### Calendar
```
GET    /api/calendar/month               Month events (merged calendar + task deadlines)
GET    /api/calendar/cases/:caseId       Events for a case
POST   /api/calendar                     Create calendar event
GET    /api/calendar/:id                 Get event
PUT    /api/calendar/:id                 Update event
DELETE /api/calendar/:id                 Delete event
```

### Conflict Checking
```
POST   /api/conflicts/check              Run a conflict check (searches across all entities)
GET    /api/conflicts/history            Paginated check history
POST   /api/conflicts/:id/clear          Clear a conflict with note
GET    /api/cases/:caseId/parties        List case parties
POST   /api/cases/:caseId/parties        Add party to case
DELETE /api/conflict-parties/:id         Remove party
```

### Documents
```
GET    /api/cases/:caseId/documents      List documents for case (optional ?q=search)
POST   /api/cases/:caseId/documents      Upload document (multipart/form-data)
GET    /api/documents/:id                Get document metadata
GET    /api/documents/:id/download       Download file (add ?inline=true for preview)
DELETE /api/documents/:id                Delete document + file
```

### Reporting & Analytics
```
GET    /api/reports/summary              KPI summary (total cases, revenue, unbilled, alerts)
GET    /api/reports/cases-by-status      Cases grouped by status (chart data)
GET    /api/reports/cases-by-month       Cases opened per month (chart data)
GET    /api/reports/financial-by-month   Revenue vs expenses per month (chart data)
GET    /api/reports/lawyer-workload      Hours + billable amounts per lawyer
GET    /api/reports/unpaid-invoices      Top 10 unpaid invoices by amount
```
All report endpoints accept optional `?from=YYYY-MM-DD&to=YYYY-MM-DD` query params.

### Communications
```
GET    /api/cases/:caseId/communications            List communications for a case
POST   /api/cases/:caseId/communications            Log a communication (NOTE, CALL, EMAIL, SMS)
POST   /api/cases/:caseId/communications/send-email Send email and auto-log as EMAIL_SENT
DELETE /api/communications/:id                      Delete a communication entry
```

### Audit, Roles & Permissions
```
GET    /api/audit-logs              List audit logs (paginated, filtered)
GET    /api/audit-logs/:id          Get audit log entry
GET    /api/roles                   List roles
GET    /api/permissions             List all permissions
```

---

## Permissions Reference

| Domain | Permissions |
|--------|------------|
| User & Role Management | USER_READ, USER_CREATE, USER_UPDATE, USER_DELETE, USER_MANAGE, ROLE_READ, ROLE_CREATE, ROLE_UPDATE, ROLE_DELETE, ROLE_MANAGE, PERMISSION_READ, PERMISSION_MANAGE, SYSTEM_MANAGE |
| Cases | CASE_READ, CASE_CREATE, CASE_UPDATE, CASE_DELETE |
| Lawyers | LAWYER_READ, LAWYER_CREATE, LAWYER_UPDATE, LAWYER_DELETE |
| Clients | CLIENT_READ, CLIENT_CREATE, CLIENT_UPDATE, CLIENT_DELETE |
| Financial | FINANCIAL_READ, FINANCIAL_CREATE, FINANCIAL_UPDATE, FINANCIAL_DELETE, FINANCIAL_MANAGE, FINANCIAL_EXPORT, INVOICE_READ, INVOICE_CREATE, INVOICE_UPDATE, INVOICE_DELETE, INVOICE_MANAGE |
| Time Tracking | TIME_READ, TIME_CREATE, TIME_UPDATE, TIME_DELETE, TIME_MANAGE |
| Tasks | TASK_READ, TASK_CREATE, TASK_UPDATE, TASK_DELETE, TASK_MANAGE |
| Calendar | CALENDAR_READ, CALENDAR_CREATE, CALENDAR_UPDATE, CALENDAR_DELETE |
| Conflicts | CONFLICT_READ, CONFLICT_CREATE, CONFLICT_MANAGE |
| Documents | DOCUMENT_READ, DOCUMENT_CREATE, DOCUMENT_DELETE, DOCUMENT_MANAGE |
| Reporting | REPORT_READ |
| Communications | COMMUNICATION_READ, COMMUNICATION_CREATE, COMMUNICATION_DELETE, COMMUNICATION_MANAGE |

**Role defaults:**
- **ADMIN** — all permissions
- **MODERATOR** — read + create across most domains
- **USER** — USER_READ only

---

## Environment Variables

### Backend
| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | No | `dev` | Active profile (dev/prod) |
| `JWT_SECRET` | Prod only | — | JWT signing secret (256-bit minimum) |
| `DB_HOST` | Prod only | — | PostgreSQL host |
| `DB_PORT` | Prod only | `5432` | PostgreSQL port |
| `DB_NAME` | Prod only | `lawfirm` | Database name |
| `DB_USER` | Prod only | — | Database username |
| `DB_PASSWORD` | Prod only | — | Database password |
| `CORS_ALLOWED_ORIGINS` | Prod only | — | Allowed CORS origins |
| `APP_STORAGE_DOCUMENTS_PATH` | No | `~/lawfirm-documents` | Document file storage path |
| `APP_STORAGE_MAX_FILE_SIZE_MB` | No | `20` | Max upload size in MB |
| `APP_MAIL_ENABLED` | No | `false` | Enable outbound email sending |
| `SPRING_MAIL_HOST` | Prod only | — | SMTP host (e.g. `smtp.gmail.com`) |
| `SPRING_MAIL_PORT` | Prod only | `587` | SMTP port |
| `SPRING_MAIL_USERNAME` | Prod only | — | SMTP username / from address |
| `SPRING_MAIL_PASSWORD` | Prod only | — | SMTP password or app password |

---

## Development Commands

### Backend
```bash
mvn spring-boot:run        # Start dev server (H2)
mvn clean test             # Unit tests only
mvn clean verify           # Full build + tests + quality checks
mvn jacoco:report          # Coverage report → target/site/jacoco/index.html
```

### Frontend
```bash
pnpm dev                   # Start dev server
pnpm build                 # Production build
pnpm lint                  # ESLint check
```

---

## Architecture

### Backend — Hexagonal (Clean) Architecture

```
domain/model/       → JPA entities (no framework dependencies)
domain/repository/  → Repository interfaces
application/dto/    → Request/Response DTOs
application/mapper/ → MapStruct mappers (no manual mapping)
application/service → Business logic
infrastructure/     → Security (JWT, RBAC), config, filters
presentation/       → REST controllers, global exception handler
```

**Key constraints:**
- No business logic in controllers or entities
- MapStruct only for DTO mapping — manual mapping is prohibited
- Flyway for all schema changes — `ddl-auto=validate` only
- All endpoints protected with `@PreAuthorize("hasPermission(null, 'PERM')")`

### Frontend — Feature-Based Architecture

```
core/               → Auth, interceptors, guards, shared models
features/           → Feature-specific standalone components
services/           → API integration (one service per domain)
shared/             → Reusable UI components
```

**Key patterns:**
- Standalone components (no NgModules)
- Signals for reactive local state
- Observable/subscribe for HTTP calls
- Lazy-loaded routes
- Permission-gated UI elements via `authService.hasPermission()`

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for coding standards and contribution workflow.

---

## License

MIT License

---

*Built with Spring Boot 3.4 and Angular 18. Designed for Moroccan law firm practice management with bilingual (French/Arabic) support.*
