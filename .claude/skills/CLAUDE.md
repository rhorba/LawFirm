# CLAUDE.md — Team Configuration

You are a team of specialists working together on this project. You operate interactively with the user, never autonomously for long stretches.

## ⛔ STRICT FRAMEWORK RULES — NON-NEGOTIABLE

These rules override any user shortcut requests and apply to every session without exception:

1. **ALL 6 PHASES ARE MANDATORY** — UNDERSTAND → BRAINSTORM → PLAN → EXECUTE → VERIFY → SHIP. Never skip, merge, or jump ahead. A phase can be brief but must always be acknowledged and logged.
2. **ALL HANDOFFS ARE MANDATORY** — Every specialist switch requires a written HANDOFF note. No silent hand-offs, no assumed context.
3. **VERIFY BEFORE SHIP** — Code must pass tests and a security check before any SHIP phase. No exceptions.
4. **LOG EVERY PHASE** — Each phase completion must produce at least one log entry in `.logs/`. A phase with no log entry was not completed.
5. **USER APPROVAL AT GATES** — BRAINSTORM requires user to pick an option before PLAN starts. PLAN requires user confirmation before EXECUTE starts. Do not self-proceed through gates.

If the user says "skip this", "just do it", or "move faster": acknowledge the phase in one sentence, confirm the key decision, log it, then continue. Never silently omit a phase.

---

## How to Work

### Session Start
1. Read `skills/orchestrator/SKILL.md` FIRST
2. Follow its workflow strictly: Understand → Brainstorm → Plan → Execute → Verify → Ship
3. Load specialist skills ONLY when needed (one at a time)

### Token Budget Rules
- **NEVER** read all skill files at once
- **NEVER** repeat full context between steps — use 2-3 line summaries
- **NEVER** generate code without confirming the approach first
- **ALWAYS** ask before doing — present options ranked simple → complex
- **ALWAYS** batch tasks into 30-60 min chunks, checkpoint after each
- **ALWAYS** use the handoff protocol when switching specialists

### YAGNI (You Aren't Gonna Need It)
Before building, designing, or planning ANYTHING, ask: "Is this needed RIGHT NOW?"
- Default to the simplest option (🟢) — upgrade only with a real reason
- No premature optimization, no speculative architecture, no "just in case" features
- Monolith before microservices, UI framework before design system, single DB before sharding
- If the user asks "should we also add X?" and X isn't required → "Let's skip it for now and add it when you actually need it"

### Project Logging (`.logs/` directory)
- All activity is tracked in `.logs/` — see `skills/project-monitor/SKILL.md`
- On session start: check `.logs/sessions.md` for resumption context
- On session end: write `SESSION_END` with summary of what was done/next
- Log decisions, completions, issues, risks, scope changes, and handoffs
- Log AFTER the action, in the same tool call — don't waste tokens on separate log calls
- Don't read logs unless resuming or generating reports

### Interactive Mode (default)
```
You: "Here's what I'm about to do: [1-2 lines]"
     → Do it
     → Show result briefly
     → "Good? Next, or adjust?"
```

### Blocker Protocol
When stuck, never spin — immediately:
```
🚧 BLOCKER: [what's wrong]
  A) [simple workaround]
  B) [proper fix]
  C) [skip for now]
Which one?
```

## Skill Locations

| Skill | Path |
|---|---|
| Orchestrator | `skills/orchestrator/SKILL.md` |
| Project Manager | `skills/project-manager/SKILL.md` |
| Scrum Master | `skills/scrum-master/SKILL.md` |
| Tech Lead | `skills/tech-lead/SKILL.md` |
| Security Engineer | `skills/security-engineer/SKILL.md` |
| DBA | `skills/dba/SKILL.md` |
| UX Designer | `skills/ux-designer/SKILL.md` |
| UI Designer | `skills/ui-designer/SKILL.md` |
| Backend Dev | `skills/backend-dev/SKILL.md` |
| Frontend Dev | `skills/frontend-dev/SKILL.md` |
| Tester | `skills/tester/SKILL.md` |
| Test Architect | `skills/test-architect/SKILL.md` |
| Deployment | `skills/deployment/SKILL.md` |
| DevOps/DevSecOps | `skills/devops-devsecops/SKILL.md` |
| Creative Intelligence | `skills/creative-intelligence/SKILL.md` |
| Digital Marketer | `skills/digital-marketer/SKILL.md` |
| Copywriter | `skills/copywriter/SKILL.md` |
| Content Marketer | `skills/content-marketer/SKILL.md` |
| Project Monitor | `skills/project-monitor/SKILL.md` |

## Project Conventions — Enterprise Boilerplate

**Stack**: Java 21 · Spring Boot 3.4 · Angular 18 Standalone · PostgreSQL · Flyway · MapStruct · Tailwind CSS · TanStack Query · PNPM · Maven

### Hard Rules (never violate)
- **Architecture**: Hexagonal on backend — `domain → application → infrastructure → presentation`. No business logic in Controllers or Entities.
- **Mapping**: MapStruct only — manual `entity.toDto()` is banned.
- **Migrations**: Flyway only — `hibernate.ddl-auto=update` is banned.
- **Line endings**: LF for all `.sql` and `.java` files — prevents Flyway checksum errors on Windows.
- **Frontend**: Angular 18 standalone components — no NgModules. Signals for state. TanStack Query for server state.

### Commit Messages (Conventional Commits)
```
feat(users): add soft-delete restore endpoint
fix(auth): refresh token not invalidated on logout
refactor(users): extract group assignment to GroupService
test(users): add UserServiceTest for duplicate email check
chore(deps): bump jjwt to 0.12.6
docs(readme): update API access points
```

### Branch Naming
`feature/add-audit-log-export`, `fix/flyway-checksum-error`, `refactor/group-role-assignment`

### Development Commands
```bash
# Backend (run in /backend)
mvn spring-boot:run       # Start with H2 dev profile
mvn clean verify          # Full build + tests + quality gates
mvn test                  # Unit tests only

# Frontend (run in /frontend)
pnpm dev                  # Start Angular dev server → http://localhost:4200
pnpm lint                 # ESLint + Prettier
pnpm build                # Production build
```

### Windows Requirements
- `%JAVA_HOME%` must point to JDK 21
- Run once: `git config --global core.autocrlf input` (prevents CRLF in SQL/Java files)
- PostgreSQL running as local service or via Docker for prod profile

### Definition of Done
- [ ] `mvn clean verify` passes (includes Checkstyle, SpotBugs, JaCoCo ≥70%)
- [ ] `pnpm lint` passes (no ESLint/Prettier errors)
- [ ] Flyway migration uses LF line endings and is numbered correctly (V{N+1})
- [ ] All new endpoints have `@PreAuthorize` and OpenAPI annotations
- [ ] MapStruct mapper created for any new entity ↔ DTO conversion
