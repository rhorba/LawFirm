# Contributing to Law Firm Management System

Thank you for considering contributing to this legal practice management project!

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Focus on what is best for the community

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported
2. Open a new issue with:
   - Clear title and description
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (OS, Java version, Node version)

### Suggesting Features

1. Open an issue with the "feature request" label
2. Describe the feature and its use case
3. Explain why it would be valuable

### Pull Requests

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Follow coding standards**
   - Backend: Follow Checkstyle rules
   - Frontend: Run `pnpm lint:fix`
   - Write tests for new features

4. **Commit messages**
   - Use Conventional Commits format
   - Examples:
     - `feat(cases): add case assignment to multiple lawyers`
     - `feat(clients): implement conflict of interest checking`
     - `feat(financial): add trust account management`
     - `fix(billing): resolve invoice calculation for multiple lawyers`
     - `fix(calendar): fix conflict detection for overlapping appointments`
     - `docs: update README with law firm features`
     - `refactor(documents): improve document versioning logic`

5. **Run tests**
   ```bash
   # Backend
   mvn verify

   # Frontend
   pnpm test
   ```

6. **Submit PR**
   - Reference related issues
   - Describe changes made
   - Include screenshots for UI changes

## Development Setup

See [README.md](README.md) for detailed setup instructions.

## Coding Standards

### Backend (Java)
- Follow hexagonal architecture patterns
- Use MapStruct for all DTO mappings (no manual mapping)
- Write Flyway migrations for all schema changes (no ddl-auto)
- Add JavaDoc for public APIs, especially legal domain logic
- Minimum 70% test coverage
- **Legal Domain**: Use proper terminology (Case/Dossier, Client, Lawyer, Tribunal)
- **Audit Trails**: All financial and case modifications must be logged
- **Soft Delete**: Use soft delete for legal entities (compliance requirement)
- **Validation**: Strict validation for CIN, tax IDs, dates, financial amounts
- **Permissions**: All endpoints must have proper @PreAuthorize annotations

### Frontend (TypeScript/Angular)
- Use standalone components (no NgModules)
- Signals for state management
- TanStack Query for server state and caching
- Tailwind CSS for styling
- Follow Angular style guide
- **Bilingual Support**: All UI text must support French/Arabic/English
- **RTL Support**: Components must work in both LTR and RTL modes
- **Responsive Design**: Mobile-first approach
- **Accessibility**: WCAG 2.1 Level AA compliance
- **Legal Terminology**: Consistent use of legal terms across UI

### Legal Domain Conventions
- **Case Numbering**: Follow format: `TYPE/TRIBUNAL/YEAR/SEQUENCE` (e.g., `CIVIL/CASA/2024/00123`)
- **Financial Precision**: Use BigDecimal for all monetary values
- **Date Handling**: Respect court deadlines and fiscal years
- **Confidentiality**: All client data must be protected
- **Audit Requirements**: Track who, what, when for all critical operations
- **Conflict Checking**: Mandatory before case acceptance
- **Billing Rules**: Clear separation of billable vs non-billable time

## Questions?

Open an issue or discussion for clarification.

Thank you for contributing!
