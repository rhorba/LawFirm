# Test Results — Law Firm Management System

**Date:** 2026-05-22

---

## Backend — JUnit + JaCoCo

| Metric | Result |
|--------|--------|
| Unit tests | 13 service tests |
| Integration tests | 16 controller ITs |
| Line coverage | **82.8%** ✅ (threshold: 80%) |
| Instruction coverage | 78% |
| Branch coverage | 56% |

**Report:** `docs/coverage/backend/index.html`

---

## Frontend Unit Tests — Karma / Jasmine (Chrome Headless)

| Metric | Result |
|--------|--------|
| Total specs | **55 / 55 PASSED** ✅ |
| Statement coverage | 70.44% |
| Function coverage | 70.58% |
| Line coverage | **73.41%** |
| Browser | Chrome Headless 148 |

Service layer tested: AuthService, CaseService, TaskService, CalendarService,
ConflictService, TimeEntryService, ReportService, FinancialService.

**Report:** `docs/coverage/frontend/index.html`

---

## E2E Browser Tests — Playwright (Chromium)

| Metric | Result |
|--------|--------|
| Total tests | **25 / 25 PASSED** ✅ |
| Browser | Chromium (headless) |
| Videos recorded | 25 `.webm` files |

### Test coverage by module

| Module | Tests |
|--------|-------|
| Authentication | 4 (login, invalid creds, redirect, logout) |
| Dashboard | 4 (load, sidebar, header, KPI cards) |
| Case Management | 4 (navigate, table, search, create button) |
| Client Management | 2 (navigate, no error) |
| Financial Module | 3 (ledger, crash check, invoices) |
| Calendar Module | 2 (navigate, grid) |
| Reporting Module | 2 (navigate, KPI cards) |
| Conflict Checking | 2 (navigate, search) |
| User Management | 2 (navigate, records) |

**Videos:** `docs/test-videos/` — 25 `.webm` recordings (one per test)
**HTML Report:** `docs/coverage/e2e/index.html`

---

## How to Re-run

```bash
# Backend (JaCoCo report → target/site/jacoco/)
cd backend && mvn clean verify

# Frontend unit tests (coverage → docs/coverage/frontend/)
cd frontend
CHROME_BIN="<path-to-chrome>" pnpm test:coverage

# E2E browser tests with video recording (videos → docs/test-videos/)
# Requires backend on :8080 and frontend on :4200
cd frontend && npx playwright test

# Run with visible browser
cd frontend && pnpm e2e:headed
```
