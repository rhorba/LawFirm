# Law Firm Management System

Comprehensive law firm management application featuring a decoupled monorepo architecture with Spring Boot 3.4 (Java 21) backend and Angular 18 frontend.

## Overview

Enterprise-grade legal practice management system designed to streamline case management, client relationships, financial tracking, and law firm operations. Built on a secure, scalable architecture with role-based access control and comprehensive audit trails.

## 🚀 Implementation Status

### ✅ Completed Features (Production Ready)

**Foundation & Infrastructure:**
- ✅ JWT Authentication with refresh tokens (15min access, 30-day refresh)
- ✅ Role-Based Access Control (RBAC) with custom permissions
- ✅ User Management (CRUD operations with validation)
- ✅ Group/Role Management (dynamic permission assignment)
- ✅ Audit Logging (request/response tracking)
- ✅ JPA Auditing (createdAt, updatedAt, version tracking)
- ✅ Soft delete with active flag
- ✅ Flyway database migrations (31 migrations)
- ✅ H2 (dev) and PostgreSQL (prod) support
- ✅ Docker containerization (dev & prod)
- ✅ API documentation (Swagger/OpenAPI)

**Case & Dossier Management:**
- ✅ Full CRUD operations with advanced search
- ✅ Case number auto-generation (Type/Tribunal/Year/Sequence)
- ✅ 7 case statuses with workflow validation
- ✅ 8 advanced filters (year, type, category, tribunal, lawyer, status, date range, search)
- ✅ Cascading category dropdowns (filtered by case type)
- ✅ Financial summary per case (payments, expenses, balance)
- ✅ Case detail view with audit information
- ✅ Create/Edit forms with validation
- ✅ Change status modal with reason tracking
- ✅ Permission-based UI (CASE_READ, CASE_CREATE, CASE_UPDATE, CASE_DELETE)
- ✅ Pagination and sorting
- ✅ Bulk delete operations
- ✅ Dark mode support

**Lawyer Management:**
- ✅ Full CRUD operations
- ✅ Lawyer profiles (name, tax ID, email, phone)
- ✅ Active/Inactive status tracking
- ✅ Search and pagination
- ✅ Create/Edit modal forms
- ✅ Bulk deactivation
- ✅ Case count tracking per lawyer
- ✅ Permission-based UI (LAWYER_READ, LAWYER_CREATE, LAWYER_UPDATE, LAWYER_DELETE)

**Reference Data Management:**
- ✅ Bilingual tribunals (French/Arabic) - 9 seeded tribunals
- ✅ Case types with number format templates - 5 types
- ✅ Case categories linked to types - 17 categories
- ✅ Case statuses with terminal flags - 7 statuses
- ✅ Status workflow validation (allowed transitions per case type)
- ✅ Global reference data caching (APP_INITIALIZER)
- ✅ Active/Inactive management

**Financial Infrastructure:**
- ✅ Financial transaction entity and repository
- ✅ Financial summary aggregation per case
- ✅ Payment/Expense tracking structure
- ⏳ UI for transaction management (planned)

### 🔄 In Progress

**Next Phase: Client Management**
- Client profiles (Individual, Corporate, Government)
- Client-case relationships
- Contact information management
- Conflict checking
- Client history tracking

### 📋 Planned Features

See full feature roadmap below for upcoming implementations.

---

## Core Features (Full Roadmap)

### 1. Case/Dossier Management
- Full case lifecycle management (New → Active → Pending → Closed → Archived)
- Unique case numbering system with fiscal year tracking
- Multi-category support (Civil, Criminal, Commercial, Administrative)
- Multi-lawyer assignment with workload tracking
- Case priority levels (Urgent, High, Normal, Low)
- Opposing party tracking and case outcome recording
- Linked cases (appeals, related matters)
- Case templates for common case types
- Advanced search and filtering (number, client, tribunal, date range, status)
- Audit trail with modification history

### 2. Client Management
- Complete client profiles (Individual, Corporate, Government)
- CIN/Tax ID, contact details, demographics
- Client-case relationship tracking
- Conflict of interest checking
- Client portal access (planned)
- Client intake workflow with questionnaires
- Emergency contacts and relationship mapping
- Document folder association

### 3. Lawyer Management
- Lawyer profiles with bar association credentials
- Specialization areas and practice permissions
- Workload and availability tracking
- Performance metrics (win rate, client satisfaction)
- CLE (Continuing Legal Education) tracking
- Multi-tribunal practice authorization

### 4. Financial Management
- Case-based financial ledger (expenses/revenues)
- Multiple operation types (fees, taxes, expert costs, etc.)
- Payment tracking (Check, Transfer, Cash, Credit Card)
- Invoice generation and client statements
- Retainer management and trust accounting
- Payment plans and overdue tracking
- Financial reporting and tax preparation

### 5. Time Tracking & Billing
- Billable/non-billable hours tracking
- Automatic timer with task descriptions
- Rate management (per lawyer, per case type)
- Time entry approval workflow
- Utilization and profitability reports
- Time-based invoicing

### 6. Document Management
- Secure document storage with versioning
- Document templates and OCR capability
- Full-text search and metadata tagging
- Access control and e-signature integration
- Document retention policies
- Folder organization by case/client

### 7. Deadline & Task Management
- Court date tracking with automated reminders
- Filing deadlines and statute of limitations calculator
- Task assignment and prioritization
- Recurring tasks and dependencies
- Critical path analysis for case milestones
- Escalation rules

### 8. Calendar & Scheduling
- Integrated calendar with court appearances
- Multi-lawyer scheduling with conflict detection
- Client meeting and conference room booking
- External calendar sync (Google, Outlook)
- Reminder notifications (email/SMS)

### 9. Reporting & Analytics
- Client statistics (demographics, top clients)
- Lawyer performance metrics and productivity
- Case analytics (outcomes, resolution time, trends)
- Financial reports (revenue, profitability, receivables)
- Custom report builder
- Executive dashboard with KPIs

### 10. Communication Management
- Email integration and communication logging
- Client portal and messaging
- SMS notifications
- Communication templates
- Meeting notes storage

### 11. Security & Compliance
- Role-based access control (Admin, Lawyer, Staff, Accountant, Client)
- JWT authentication with 2FA support
- Activity logging and audit trails
- Data encryption (at rest and in transit)
- GDPR compliance features
- Session management and IP whitelisting

### 12. Reference Data Management
- Bilingual tribunals/courts (French/Arabic)
- Case type categorization (Nature Affaire)
- Court rules repository
- Legal precedents database
- Fee schedules and standard forms

### 13. Notifications & Alerts
- Individual and bulk notifications
- Configurable deadline reminders
- Payment and task alerts
- Multi-channel delivery (email, SMS, in-app)
- User notification preferences

### 14. Data Import/Export
- CSV/PDF export for all entities
- Bulk import with validation
- Automated database backups
- Cloud backup options
- API for third-party integrations

### 15. Internationalization
- Multi-language support (French, Arabic, English)
- RTL (Right-to-Left) support for Arabic
- Bilingual reference data
- Localized date/time and currency formats

## Technical Stack

- **Backend**: Spring Boot 3.4, Java 21, PostgreSQL/H2, JWT Authentication
- **Frontend**: Angular 18, TanStack Query, Tailwind CSS, Signals
- **Security**: Role-Based Access Control (RBAC) with permission-level granularity
- **Authentication**: JWT with refresh tokens and 2FA support
- **Architecture**: Hexagonal/Clean Architecture on backend, layer-based on frontend
- **Database**: Flyway migrations, seeded data, optimized indexing
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Observability**: Logback JSON logging, Spring Actuator endpoints
- **Code Quality**: Checkstyle, SpotBugs, JaCoCo (70% coverage), ESLint, Prettier
- **Testing**: JUnit, Mockito, Testcontainers
- **Docker**: Multi-stage builds, Docker Compose for dev and prod

## Prerequisites

- **Java**: JDK 21 (set `JAVA_HOME` environment variable)
- **Node.js**: v20+ with pnpm installed globally
- **Git**: Configure `git config --global core.autocrlf input`
- **Docker**: (Optional) For containerized development

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

4. **Login**
   - Username: `admin`
   - Password: `admin123`
   - Role: System Administrator

### Docker Development Mode (H2 Database)

Run both backend and frontend in Docker with hot-reload enabled:

```bash
# Start all services
docker-compose -f docker-compose.dev.yml up --build

# Or run in detached mode
docker-compose -f docker-compose.dev.yml up --build -d

# View logs
docker-compose -f docker-compose.dev.yml logs -f

# Stop all services
docker-compose -f docker-compose.dev.yml down
```

Access:
- Frontend: `http://localhost:4200`
- Backend API: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:boilerplate`, username: `sa`, password: empty)

**Features**:
- Hot-reload for both frontend and backend
- H2 in-memory database (data resets on restart)
- Source code mounted as volumes for live development
- Automatic Flyway migrations and seed data on startup

### Production Mode (Docker Compose + PostgreSQL)

```bash
docker-compose -f docker-compose.prod.yml up --build
```

Access:
- Frontend: `http://localhost`
- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Project Structure

```
/backend                          # Spring Boot backend
├── src/main/java/com/lawfirm/
│   ├── domain/                   # Entities, repositories, specifications
│   │   ├── case/                 # Case/Dossier entities
│   │   ├── client/               # Client entities
│   │   ├── lawyer/               # Lawyer entities
│   │   ├── financial/            # Financial ledger entities
│   │   ├── document/             # Document management
│   │   ├── task/                 # Task and deadline entities
│   │   └── user/                 # User management entities
│   ├── application/              # DTOs, mappers, service implementations
│   │   ├── case/                 # Case management services
│   │   ├── client/               # Client management services
│   │   ├── lawyer/               # Lawyer management services
│   │   ├── financial/            # Financial services
│   │   └── reporting/            # Reporting and analytics services
│   ├── infrastructure/           # Security, persistence, configs
│   │   ├── security/             # JWT, RBAC, audit logging
│   │   ├── persistence/          # JPA configurations
│   │   └── integration/          # Email, SMS, calendar integrations
│   └── presentation/             # REST controllers, exception handlers
│       ├── case/                 # Case management endpoints
│       ├── client/               # Client management endpoints
│       ├── lawyer/               # Lawyer management endpoints
│       └── financial/            # Financial endpoints
├── src/main/resources/
│   ├── db/migration/             # Flyway SQL migrations
│   └── application*.yml          # Configuration files
└── pom.xml

/frontend                         # Angular 18 frontend
├── src/app/
│   ├── core/                     # Services, guards, interceptors, models
│   │   ├── services/             # Auth, token, API services
│   │   ├── guards/               # Route guards
│   │   ├── interceptors/         # HTTP interceptors
│   │   └── models/               # TypeScript interfaces
│   ├── features/                 # Feature modules
│   │   ├── auth/                 # Login, registration
│   │   ├── dashboard/            # KPI dashboard
│   │   ├── cases/                # Case management
│   │   ├── clients/              # Client management
│   │   ├── lawyers/              # Lawyer management
│   │   ├── financial/            # Financial tracking
│   │   ├── documents/            # Document management
│   │   ├── tasks/                # Task and deadline management
│   │   ├── calendar/             # Calendar and scheduling
│   │   ├── reports/              # Reporting and analytics
│   │   └── admin/                # System administration
│   └── shared/                   # Shared components, directives, pipes
└── package.json

/docs
├── plans/                        # Implementation plans
├── api/                          # API documentation
└── user-guides/                  # User manuals (planned)
```

## API Documentation

Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

## Default Credentials

| Username | Password   | Role                    |
|----------|------------|-------------------------|
| admin    | admin123   | System Administrator    |

## System Roles

### Admin Roles
- **System Administrator**: Full system access, user management, system configuration
- **Office Manager**: Firm-wide administration, reporting, resource allocation

### Legal Roles
- **Senior Partner**: All case access, financial oversight, lawyer management
- **Partner**: Full case management, client management, financial access
- **Associate Lawyer**: Assigned case management, client interaction, time tracking
- **Junior Lawyer**: Limited case access, task execution, time tracking

### Support Roles
- **Legal Secretary**: Case administration, document management, scheduling
- **Paralegal**: Research, document preparation, case support
- **Accountant**: Financial management, billing, trust accounting
- **Receptionist**: Client intake, basic scheduling, communication

### Client Roles
- **Client Portal User**: View assigned cases, documents, invoices, communicate with lawyers (planned)

## Available Permissions (Planned)

### Case Management
- `CASE_READ` - View cases
- `CASE_CREATE` - Create new cases
- `CASE_UPDATE` - Modify cases
- `CASE_DELETE` - Delete/archive cases
- `CASE_ASSIGN` - Assign lawyers to cases
- `CASE_OUTCOME` - Record case outcomes
- `CASE_MANAGE` - Full case management

### Client Management
- `CLIENT_READ` - View clients
- `CLIENT_CREATE` - Create new clients
- `CLIENT_UPDATE` - Modify client information
- `CLIENT_DELETE` - Delete clients
- `CLIENT_CONFLICT_CHECK` - Run conflict of interest checks
- `CLIENT_MANAGE` - Full client management

### Lawyer Management
- `LAWYER_READ` - View lawyer profiles
- `LAWYER_CREATE` - Add new lawyers
- `LAWYER_UPDATE` - Modify lawyer information
- `LAWYER_PERFORMANCE` - View performance metrics
- `LAWYER_MANAGE` - Full lawyer management

### Financial Management
- `FINANCE_READ` - View financial records
- `FINANCE_CREATE` - Record transactions
- `FINANCE_UPDATE` - Modify financial entries
- `FINANCE_APPROVE` - Approve expenses
- `FINANCE_INVOICE` - Generate invoices
- `FINANCE_TRUST` - Manage trust accounts
- `FINANCE_REPORT` - Generate financial reports
- `FINANCE_MANAGE` - Full financial management

### Document Management
- `DOCUMENT_READ` - View documents
- `DOCUMENT_UPLOAD` - Upload documents
- `DOCUMENT_UPDATE` - Modify/version documents
- `DOCUMENT_DELETE` - Delete documents
- `DOCUMENT_SHARE` - Share documents externally
- `DOCUMENT_MANAGE` - Full document management

### Time & Billing
- `TIME_ENTRY` - Enter time
- `TIME_APPROVE` - Approve time entries
- `BILLING_CREATE` - Create invoices
- `BILLING_MANAGE` - Full billing management

### Calendar & Tasks
- `CALENDAR_READ` - View calendar
- `CALENDAR_MANAGE` - Manage appointments
- `TASK_READ` - View tasks
- `TASK_CREATE` - Create tasks
- `TASK_ASSIGN` - Assign tasks
- `TASK_MANAGE` - Full task management

### Reporting
- `REPORT_VIEW` - View reports
- `REPORT_EXPORT` - Export data
- `REPORT_CUSTOM` - Create custom reports
- `REPORT_ANALYTICS` - Access analytics dashboard

### Communication
- `COMMUNICATION_SEND` - Send messages
- `COMMUNICATION_BULK` - Bulk notifications
- `COMMUNICATION_MANAGE` - Full communication management

### User & System Management
- `USER_READ` - View users
- `USER_CREATE` - Create new users
- `USER_UPDATE` - Modify users
- `USER_DELETE` - Delete users
- `USER_MANAGE` - Full user management
- `ROLE_MANAGE` - Role management
- `PERMISSION_MANAGE` - Permission management
- `SYSTEM_MANAGE` - Full system administration
- `AUDIT_VIEW` - View audit logs
- `BACKUP_MANAGE` - Database backup/restore

## Development Commands

### Backend
```bash
mvn clean install          # Build project
mvn spring-boot:run        # Run application
mvn test                   # Run tests
mvn verify                 # Run tests + quality checks
mvn flyway:migrate         # Run database migrations
```

### Frontend
```bash
pnpm dev                   # Start dev server
pnpm build                 # Build for production
pnpm lint                  # Run linter
pnpm lint:fix              # Fix linting issues
pnpm test                  # Run tests
```

## Environment Variables

### Backend
- `SPRING_PROFILES_ACTIVE` - Active profile (dev/prod)
- `DB_HOST` - Database host (prod only)
- `DB_PORT` - Database port (prod only)
- `DB_NAME` - Database name (default: lawfirm)
- `DB_USER` - Database username (prod only)
- `DB_PASSWORD` - Database password (prod only)
- `JWT_SECRET` - JWT signing secret (required for prod)
- `CORS_ALLOWED_ORIGINS` - Allowed CORS origins (prod only)
- `SMTP_HOST` - Email server host (for notifications)
- `SMTP_PORT` - Email server port
- `SMTP_USERNAME` - Email account username
- `SMTP_PASSWORD` - Email account password
- `SMS_PROVIDER_API_KEY` - SMS service API key (optional)
- `STORAGE_PATH` - Document storage path (default: ./storage)
- `BACKUP_PATH` - Database backup path (default: ./backups)
- `DEFAULT_LANGUAGE` - Default system language (fr/ar/en, default: fr)

### Frontend
- `API_BASE_URL` - Backend API URL (default: http://localhost:8080/api)
- `ENABLE_CLIENT_PORTAL` - Enable client portal features (true/false)
- `ENABLE_RTL` - Enable RTL support for Arabic (true/false)

## Testing

### Backend Testing
```bash
# Unit tests
mvn test

# Integration tests with Testcontainers
mvn verify

# Coverage report (target/site/jacoco/index.html)
mvn jacoco:report
```

## Code Quality

### Backend
- **Checkstyle**: Code style enforcement (`/backend/checkstyle.xml`)
- **SpotBugs**: Bug detection
- **JaCoCo**: Code coverage (minimum 70%)

### Frontend
- **ESLint**: Code linting
- **Prettier**: Code formatting

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

## Architecture

### Backend - Hexagonal Architecture

**Layers:**
1. **Domain**: Business entities (Case, Client, Lawyer, Financial, etc.) and repository interfaces
2. **Application**: Use cases, DTOs, mappers, business logic (case workflows, billing, reporting)
3. **Infrastructure**: Security, database, external services (email, SMS, calendar sync)
4. **Presentation**: REST API controllers for all modules

**Key Patterns:**
- Dependency inversion (domain doesn't depend on infrastructure)
- MapStruct for DTO mapping (no manual mapping)
- Flyway for database versioning and migrations
- Repository pattern with JPA
- JPA Specifications for dynamic search/filtering
- Soft-delete pattern with restore/purge
- Event-driven architecture for notifications and audit logging
- Multi-tenancy support for law firm branches (planned)

### Frontend - Layer-Based Architecture

**Structure:**
- **/core**: Authentication, guards, interceptors, shared models
- **/features**: Feature-specific components (cases, clients, lawyers, financial, etc.)
- **/services**: API communication services for all modules
- **/shared**: Reusable components, directives, pipes

**Key Patterns:**
- Standalone components (no NgModules)
- Signals for reactive state management
- TanStack Query for server state and caching
- Functional guards and interceptors
- Shared TypeScript model interfaces
- Lazy loading for performance optimization
- Responsive design with mobile-first approach
- RTL (Right-to-Left) support for Arabic

## Implementation Status

**Current Phase**: Foundation & Core Infrastructure (Completed)
- User authentication and authorization
- Role-based access control
- Audit logging
- User management with groups

**Next Phase**: Law Firm Domain Implementation (In Planning)
- Case/Dossier management
- Client management
- Lawyer management
- Reference data (Tribunals, Case Types)

**Planned Features**: See feature list above for complete roadmap

## License

MIT License

## Support

For issues and questions, please open a GitHub issue or contact the development team.

## Acknowledgments

Built on enterprise-grade boilerplate architecture with Spring Boot 3.4 and Angular 18. Designed specifically for legal practice management with bilingual support (French/Arabic) for Moroccan law firms.
"# LawFirm" 
