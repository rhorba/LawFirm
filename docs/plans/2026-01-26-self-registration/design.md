# Self-Registration Feature Design

## Summary

Public registration endpoint allowing new users to create accounts with auto-assigned USER role and immediate login (auto-issued JWT tokens). Includes in-memory rate limiting via Bucket4j.

## Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Scope | Self-registration (no email verification) | YAGNI — keep boilerplate simple |
| Fields | Username + Email + Password | Maps to existing schema, no migrations |
| Post-signup flow | Auto-login, redirect to dashboard | Best UX, single request |
| Endpoint location | `POST /api/auth/register` in AuthController | Registration is an auth concern |
| Rate limiting | Bucket4j in-memory per IP | Lightweight, no infrastructure |
| Password UX | Visibility toggle on password fields | Standard usability pattern |

## Backend Design

### New Endpoint

`POST /api/auth/register` (public — covered by existing `/api/auth/**` wildcard in SecurityConfig)

### Request — `RegisterRequest` (new DTO)

```java
@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
```

### Response — existing `AuthResponse`

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": 2,
    "username": "johndoe",
    "email": "john@example.com",
    "enabled": true,
    "roles": [{ "name": "USER" }]
  }
}
```

### Registration Flow

1. `AuthController.register()` receives `@Valid RegisterRequest`
2. `AuthService.register()`:
   - Checks username uniqueness via `UserRepository.existsByUsername()` → 409 if taken
   - Checks email uniqueness via `UserRepository.existsByEmail()` → 409 if taken
   - Fetches default USER role from `RoleRepository.findByName("USER")`
   - Creates `User` entity with BCrypt-encoded password and USER role
   - Saves via `UserRepository.save()`
   - Authenticates via `AuthenticationManager.authenticate()` (same as login)
   - Generates tokens via `JwtService`
   - Returns `AuthResponse`

### Rate Limiting

- **Library:** `bucket4j-core` added to `pom.xml`
- **Implementation:** `RateLimitFilter` (servlet filter)
- **Scope:** Only `POST /api/auth/register`
- **Limit:** 5 requests per IP per hour
- **Storage:** `ConcurrentHashMap<String, Bucket>` (in-memory, resets on restart)
- **Response on exceed:** HTTP 429 with `Retry-After` header
- **New exception:** `RateLimitExceededException` handled by `GlobalExceptionHandler`

### Error Handling

| Scenario | Exception | HTTP | Message |
|---|---|---|---|
| Username taken | `DuplicateResourceException` | 409 | "Username already exists" |
| Email taken | `DuplicateResourceException` | 409 | "Email already exists" |
| Validation fails | `MethodArgumentNotValidException` | 400 | Field-level errors |
| Rate limited | `RateLimitExceededException` | 429 | "Too many registration attempts" |

All handled by existing `GlobalExceptionHandler` (add 429 case).

### Backend Tests

- `AuthServiceTest.register_Success` — user saved with USER role, tokens returned
- `AuthServiceTest.register_DuplicateUsername` — 409 thrown
- `AuthServiceTest.register_DuplicateEmail` — 409 thrown
- `AuthController` MockMvc integration test — end-to-end register flow

## Frontend Design

### New Component: `RegisterComponent`

**Location:** `frontend/src/app/features/auth/register/`

**Form (Angular Reactive Forms):**

| Field | Validators |
|---|---|
| `username` | required, minLength(3), maxLength(50) |
| `email` | required, email |
| `password` | required, minLength(8) |
| `confirmPassword` | required, must match password (cross-field) |

**UI (Tailwind CSS):**

- Same card layout as LoginComponent for consistency
- Validation errors shown below each field
- Password visibility toggle (eye icon) on password and confirmPassword fields
- "Create Account" button with loading spinner
- Link: "Already have an account? Log in" → `/login`

### AuthService Changes

Add `register()` method:
```typescript
register(data: RegisterRequest): Observable<AuthResponse> {
  return this.http.post<AuthResponse>('/api/auth/register', data).pipe(
    tap(response => {
      this.tokenService.setAccessToken(response.accessToken);
      this.tokenService.setRefreshToken(response.refreshToken);
      this.currentUser.set(response.user);
      this.isAuthenticated.set(true);
    })
  );
}
```

### Routing Change

Add to `app.routes.ts`:
```typescript
{ path: 'register', loadComponent: () => import('./features/auth/register/register.component') }
```

No `authGuard` on this route.

### LoginComponent Change

Add link to registration:
```html
<p>Don't have an account? <a routerLink="/register">Sign up</a></p>
```

### Frontend Error Handling

| HTTP Status | UI Message |
|---|---|
| 409 | "Username or email already taken" |
| 429 | "Too many attempts. Please try again later." |
| Other | "Registration failed. Please try again." |

## Files Changed

| Layer | File | Change Type |
|---|---|---|
| Backend | `pom.xml` | Modified — add `bucket4j-core` |
| Backend | `RegisterRequest.java` | New |
| Backend | `RateLimitExceededException.java` | New |
| Backend | `RateLimitFilter.java` | New |
| Backend | `AuthService.java` | Modified — add `register()` |
| Backend | `AuthController.java` | Modified — add register endpoint |
| Backend | `GlobalExceptionHandler.java` | Modified — add 429 handler |
| Backend | `AuthServiceTest.java` | Modified — add registration tests |
| Frontend | `register.component.ts` | New |
| Frontend | `register.component.html` | New |
| Frontend | `auth.service.ts` | Modified — add `register()` |
| Frontend | `app.routes.ts` | Modified — add `/register` route |
| Frontend | `login.component.html` | Modified — add "Sign up" link |

## Not In Scope

- Email verification
- Password strength rules (uppercase, digits, special chars)
- Username availability check (async)
- CAPTCHA
- Social login (OAuth)

These can be added later without breaking this design.
