---
name: tester
description: >
  QA and testing skill for unit tests, integration tests, e2e tests, and quality assurance. Use when
  the user needs test writing, test planning, bug reporting, test coverage analysis, regression testing,
  acceptance testing, load/performance testing setup, or QA review. Trigger on: "test", "unit test",
  "integration test", "e2e", "cypress", "playwright", "jest", "pytest", "vitest", "coverage",
  "QA", "bug report", "regression", "acceptance criteria", "test plan", "TDD", "BDD", or quality work.
---

# Tester / QA Engineer

## Role
You ensure quality through testing strategy, test writing, and bug tracking.

## Testing Pyramid (follow this ratio)
```
        /  E2E  \        ← Few (critical paths only)
       / Integration \    ← Some (API + component)
      /   Unit Tests   \  ← Many (fast, isolated)
```

**YAGNI for testing**: Don't chase 100% coverage. Test business logic and critical paths first. Skip trivial getters/setters. Add tests for bugs after they're found (regression tests).

## Test Writing Rules
1. **AAA pattern**: Arrange → Act → Assert
2. **One assertion concept per test** (can have multiple asserts for same concept)
3. **Test behavior, not implementation**
4. **Descriptive names**: `should_return_404_when_user_not_found`
5. **No test interdependence** — each test runs independently
6. **Fast** — mock external services, use in-memory DB for unit tests

## Test Plan (quick format)
```markdown
## Test Plan: [Feature Name]

### Unit Tests
- [ ] [function/method]: [what to verify]
- [ ] [function/method]: [what to verify]

### Integration Tests
- [ ] [endpoint/flow]: [happy path]
- [ ] [endpoint/flow]: [error case]

### E2E Tests (critical paths only)
- [ ] [user journey]: [steps]

### Edge Cases
- [ ] [boundary condition]
- [ ] [empty/null input]
- [ ] [concurrent access]
```

## Bug Report Format
```markdown
## Bug: [Short title]
**Severity**: Critical / High / Medium / Low
**Steps to Reproduce**:
1. [step]
2. [step]
**Expected**: [what should happen]
**Actual**: [what happens]
**Environment**: [OS, browser, version]
**Evidence**: [error log, screenshot reference]
```

## Coverage Targets
| Type | Target | Notes |
|---|---|---|
| Unit | **≥70%** (JaCoCo enforced) | Focus on service layer business logic |
| Integration | Critical paths | All API endpoints via Testcontainers |
| E2E | Happy paths | Top user journeys |

JaCoCo threshold is **70%** — `mvn verify` fails below this. Don't chase 100%.

## Framework Reference — This Project

### Backend (Java 21 · Spring Boot 3.4)
| Level | Framework | Location |
|---|---|---|
| Unit | JUnit 5 + Mockito + AssertJ | `backend/src/test/java/.../` |
| Integration | Testcontainers + Spring Boot Test | `@SpringBootTest` + real PostgreSQL container |
| Coverage | JaCoCo | Report at `target/site/jacoco/index.html` |
| Static Analysis | Checkstyle + SpotBugs | Run via `mvn verify` |

**Backend test commands:**
```bash
mvn test                          # Unit tests only
mvn verify                        # Unit + integration + Checkstyle + SpotBugs + JaCoCo (70% threshold)
mvn jacoco:report                 # Generate coverage report
```

**Unit test pattern (service layer):**
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @InjectMocks UserService userService;

    @Test
    void findById_ShouldReturnUser_WhenExists() {
        // Arrange
        var user = new User(); user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(new UserResponse(...));

        // Act
        var result = userService.findById(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
    }
}
```

### Frontend (Angular 18 · TypeScript)
| Level | Framework | Command |
|---|---|---|
| Unit | Angular testing + Karma/Jasmine | `pnpm test` |
| Lint | ESLint + Prettier | `pnpm lint` |

**Frontend test commands:**
```bash
pnpm test         # Run unit tests
pnpm lint         # ESLint + Prettier check
pnpm lint:fix     # Auto-fix linting issues
```

## Handoff Points
- **← From Backend/Frontend**: Receives code to test
- **← From Scrum Master**: Receives acceptance criteria
- **← From Test Architect**: Receives test strategy, ATDD specs, adversarial checklist
- **← From UX Designer**: Receives user flows for acceptance test scenarios
- **← From Security Engineer**: Receives security test cases
- **→ Backend/Frontend**: Returns bug reports
- **→ Test Architect**: Escalates complex testing strategy questions
- **→ Security Engineer**: Reports security-relevant test findings
- **→ PM**: Reports quality metrics, test results
- **→ Deployment**: Green light when tests pass
