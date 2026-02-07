# Self-Registration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: use executing-plans skill to implement this plan task-by-task.

**Goal:** Allow unauthenticated users to create an account via a public registration form, auto-login with JWT tokens, and land on the dashboard.

**Architecture:** New `POST /api/auth/register` endpoint in the existing `AuthController` (Spring Boot 3.4, Java 21). New `RegisterComponent` in the Angular 18 frontend matching the existing `LoginComponent` patterns. Bucket4j in-memory rate limiting to prevent abuse.

**Tech Stack:** Spring Boot 3.4 (Java 21), Angular 18, Flyway, MapStruct, Tailwind CSS.

---

## Batch 1: Backend — DTO, Service, Controller

### Task 1: Create `RegisterRequest` DTO -- COMPLETED

**Files:**
- Create: `backend/src/main/java/com/boilerplate/application/dto/request/RegisterRequest.java`

**Step 1: Create the DTO**

Create the file with this exact content:

```java
package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

**Notes:** This mirrors the validation rules from `CreateUserRequest.java` but without `roleIds` (self-registration always gets the USER role).

---

### Task 2: Add `register()` method to `AuthService` -- COMPLETED

**Files:**
- Modify: `backend/src/main/java/com/boilerplate/application/service/AuthService.java`

**Step 1: Add new imports and dependencies**

Add these imports to the existing import block:

```java
import com.boilerplate.application.dto.request.RegisterRequest;
import com.boilerplate.domain.model.Role;
import com.boilerplate.domain.model.User;
import com.boilerplate.domain.repository.RoleRepository;
import com.boilerplate.presentation.exception.DuplicateResourceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Set;
```

Add two new constructor-injected fields to the class (Lombok `@RequiredArgsConstructor` handles injection):

```java
private final RoleRepository roleRepository;
private final PasswordEncoder passwordEncoder;
```

The full field list after the change:

```java
private final AuthenticationManager authenticationManager;
private final UserDetailsService userDetailsService;
private final UserRepository userRepository;
private final RoleRepository roleRepository;
private final PasswordEncoder passwordEncoder;
private final JwtService jwtService;
private final UserMapper userMapper;
```

**Step 2: Add the `register()` method**

Add this method after the existing `refreshToken()` method:

```java
@Transactional
public AuthResponse register(RegisterRequest request) {
    log.debug("Registration attempt for user: {}", request.getUsername());

    if (userRepository.existsByUsername(request.getUsername())) {
        throw new DuplicateResourceException("Username already exists");
    }

    if (userRepository.existsByEmail(request.getEmail())) {
        throw new DuplicateResourceException("Email already exists");
    }

    Role userRole = roleRepository.findByName("USER")
        .orElseThrow(() -> new RuntimeException("Default USER role not found"));

    User user = User.builder()
        .username(request.getUsername())
        .email(request.getEmail())
        .password(passwordEncoder.encode(request.getPassword()))
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .roles(Set.of(userRole))
        .build();

    userRepository.save(user);

    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
    );

    UserDetails userDetails = (UserDetails) authentication.getPrincipal();

    String accessToken = jwtService.generateAccessToken(userDetails);
    String refreshToken = jwtService.generateRefreshToken(userDetails, false);

    UserResponse userResponse = userRepository.findByUsernameWithRolesAndPermissions(request.getUsername())
        .map(userMapper::toResponse)
        .orElseThrow(() -> new RuntimeException("User not found after registration"));

    log.info("User registered successfully: {}", request.getUsername());

    return AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(15 * 60L)
        .user(userResponse)
        .build();
}
```

**Notes:**
- `@Transactional` (not `readOnly = true`) because we write to the database.
- After saving, we authenticate through `AuthenticationManager` so Spring Security loads the `UserDetails` correctly (same pattern as `login()`).
- `rememberMe` is `false` for registration (30-day refresh token).

---

### Task 3: Add `/register` endpoint to `AuthController` -- COMPLETED

**Files:**
- Modify: `backend/src/main/java/com/boilerplate/presentation/controller/AuthController.java`

**Step 1: Add import**

Add to the existing imports:

```java
import com.boilerplate.application.dto.request.RegisterRequest;
import org.springframework.http.HttpStatus;
```

**Step 2: Add the register endpoint**

Add this method after the existing `refreshToken()` method:

```java
@PostMapping("/register")
@ResponseStatus(HttpStatus.CREATED)
@Operation(summary = "Register new user", description = "Create a new account and return JWT tokens")
public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
}
```

**Notes:** No security changes needed — `SecurityConfig` already has `.requestMatchers("/api/auth/**").permitAll()`, which covers `/api/auth/register`.

**Step 3: Verify compilation**

```bash
cd backend && mvn clean compile -q
```

Expected: BUILD SUCCESS with no errors.

---

## Batch 2: Backend — Rate Limiting

### Task 4: Add Bucket4j dependency to `pom.xml` -- COMPLETED

**Files:**
- Modify: `backend/pom.xml`

**Step 1: Add the dependency**

Add in the `<dependencies>` section, after the JWT dependencies block:

```xml
<!-- Rate Limiting -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
```

---

### Task 5: Create `RateLimitExceededException` -- COMPLETED

**Files:**
- Create: `backend/src/main/java/com/boilerplate/presentation/exception/RateLimitExceededException.java`

**Step 1: Create the exception class**

```java
package com.boilerplate.presentation.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
```

**Notes:** Follows the exact same pattern as `DuplicateResourceException.java`.

---

### Task 6: Add 429 handler to `GlobalExceptionHandler` -- COMPLETED

**Files:**
- Modify: `backend/src/main/java/com/boilerplate/presentation/exception/GlobalExceptionHandler.java`

**Step 1: Add the handler method**

Add this method after the existing `handleAccessDeniedException()` method and before the `handleGlobalException()` catch-all:

```java
@ExceptionHandler(RateLimitExceededException.class)
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public ErrorResponse handleRateLimitExceededException(
    RateLimitExceededException ex,
    HttpServletRequest request
) {
    log.warn("Rate limit exceeded: {}", ex.getMessage());
    return buildErrorResponse(
        HttpStatus.TOO_MANY_REQUESTS,
        ex.getMessage(),
        request.getRequestURI()
    );
}
```

---

### Task 7: Create `RateLimitFilter` -- COMPLETED

**Files:**
- Create: `backend/src/main/java/com/boilerplate/infrastructure/config/RateLimitFilter.java`

**Step 1: Create the filter**

```java
package com.boilerplate.infrastructure.config;

import com.boilerplate.presentation.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        if ("POST".equalsIgnoreCase(request.getMethod())
                && "/api/auth/register".equals(request.getRequestURI())) {

            String clientIp = getClientIp(request);
            Bucket bucket = buckets.computeIfAbsent(clientIp, k -> createBucket());

            if (!bucket.tryConsume(1)) {
                throw new RateLimitExceededException("Too many registration attempts. Please try again later.");
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
            5,
            Refill.intervally(5, Duration.ofHours(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

**Notes:**
- `OncePerRequestFilter` guarantees one execution per request.
- Only intercepts `POST /api/auth/register` — all other requests pass through.
- 5 requests per IP per hour, refills all 5 tokens after each hour.
- `X-Forwarded-For` support for proxied environments (Docker, nginx).

**Step 2: Verify compilation**

```bash
cd backend && mvn clean compile -q
```

Expected: BUILD SUCCESS.

---

## Batch 3: Backend — Unit Tests

### Task 8: Create `AuthServiceTest` -- COMPLETED

**Files:**
- Create: `backend/src/test/java/com/boilerplate/application/service/AuthServiceTest.java`

**Step 1: Create the test class**

Follow the same pattern as `UserServiceTest.java`:

```java
package com.boilerplate.application.service;

import com.boilerplate.application.dto.request.RegisterRequest;
import com.boilerplate.application.dto.response.AuthResponse;
import com.boilerplate.application.dto.response.UserResponse;
import com.boilerplate.application.mapper.UserMapper;
import com.boilerplate.domain.model.Role;
import com.boilerplate.domain.model.User;
import com.boilerplate.domain.repository.RoleRepository;
import com.boilerplate.domain.repository.UserRepository;
import com.boilerplate.infrastructure.security.JwtService;
import com.boilerplate.presentation.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User savedUser;
    private UserResponse userResponse;
    private Role userRole;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
            .username("newuser")
            .email("new@example.com")
            .password("password123")
            .build();

        userRole = new Role();
        userRole.setId(2L);
        userRole.setName("USER");

        savedUser = User.builder()
            .id(2L)
            .username("newuser")
            .email("new@example.com")
            .password("encodedPassword")
            .enabled(true)
            .build();

        userResponse = UserResponse.builder()
            .id(2L)
            .username("newuser")
            .email("new@example.com")
            .enabled(true)
            .build();
    }

    @Test
    void register_Success() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(jwtService.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(userDetails, false)).thenReturn("refresh-token");
        when(userRepository.findByUsernameWithRolesAndPermissions("newuser"))
            .thenReturn(Optional.of(savedUser));
        when(userMapper.toResponse(savedUser)).thenReturn(userResponse);

        // Act
        AuthResponse result = authService.register(registerRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(900L);
        assertThat(result.getUser().getUsername()).isEqualTo("newuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }
}
```

**Step 2: Run tests**

```bash
cd backend && mvn test -q
```

Expected: All tests pass (existing `UserServiceTest` + new `AuthServiceTest`).

---

## Batch 4: Frontend — AuthService & Routing

### Task 9: Add `RegisterRequest` interface and `register()` method to `AuthService` -- COMPLETED

**Files:**
- Modify: `frontend/src/app/core/services/auth.service.ts`

**Step 1: Add `RegisterRequest` interface**

Add after the existing `LoginRequest` interface:

```typescript
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}
```

**Step 2: Add `register()` method**

Add after the existing `login()` method in the `AuthService` class:

```typescript
register(data: RegisterRequest): Observable<AuthResponse> {
  return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, data)
    .pipe(tap(response => {
      this.tokenService.setAccessToken(response.accessToken);
      this.tokenService.setRefreshToken(response.refreshToken);
      this.currentUser.set(response.user);
      this.isAuthenticated.set(true);
    }));
}
```

**Notes:** Identical token-handling logic to `login()` — tokens stored, signals updated.

---

### Task 10: Add `/register` route to `app.routes.ts` -- COMPLETED

**Files:**
- Modify: `frontend/src/app/app.routes.ts`

**Step 1: Add the register route**

Add this route after the existing `login` route entry:

```typescript
{
  path: 'register',
  loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
},
```

---

## Batch 5: Frontend — RegisterComponent

### Task 11: Create `RegisterComponent` TypeScript file -- COMPLETED

**Files:**
- Create: `frontend/src/app/features/auth/register/register.component.ts`

**Step 1: Create the component**

```typescript
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loading = signal(false);
  error = signal<string | null>(null);
  showPassword = signal(false);
  showConfirmPassword = signal(false);

  registerForm = this.fb.nonNullable.group(
    {
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: [this.passwordMatchValidator] }
  );

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;
    if (password && confirmPassword && password !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  togglePasswordVisibility(): void {
    this.showPassword.update((v) => !v);
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword.update((v) => !v);
  }

  onSubmit(): void {
    if (this.registerForm.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const { confirmPassword, ...registerData } = this.registerForm.getRawValue();

    this.authService.register(registerData).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 409) {
          this.error.set('Username or email already taken.');
        } else if (err.status === 429) {
          this.error.set('Too many attempts. Please try again later.');
        } else {
          this.error.set(err.error?.message || 'Registration failed. Please try again.');
        }
        this.loading.set(false);
      },
    });
  }
}
```

**Notes:**
- `confirmPassword` is stripped before sending to the API (backend doesn't expect it).
- Cross-field `passwordMatchValidator` applied at the form group level.
- Password visibility toggled via signals.
- Error handling maps 409/429 to user-friendly messages.

---

### Task 12: Create `RegisterComponent` template -- COMPLETED

**Files:**
- Create: `frontend/src/app/features/auth/register/register.component.html`

**Step 1: Create the template**

Follows the exact same Tailwind card layout as `login.component.html`:

```html
<div class="min-h-screen flex items-center justify-center bg-gray-100">
  <div class="max-w-md w-full bg-white rounded-lg shadow-md p-8">
    <h2 class="text-2xl font-bold text-center mb-6">Create Account</h2>

    @if (error()) {
      <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
        {{ error() }}
      </div>
    }

    <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" class="space-y-4">
      <!-- Username -->
      <div>
        <label for="username" class="block text-sm font-medium text-gray-700 mb-1">
          Username
        </label>
        <input
          id="username"
          type="text"
          formControlName="username"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="Enter username"
        />
        @if (registerForm.get('username')?.touched && registerForm.get('username')?.errors) {
          <p class="mt-1 text-sm text-red-600">
            @if (registerForm.get('username')?.errors?.['required']) {
              Username is required.
            } @else if (registerForm.get('username')?.errors?.['minlength']) {
              Username must be at least 3 characters.
            } @else if (registerForm.get('username')?.errors?.['maxlength']) {
              Username must be at most 50 characters.
            }
          </p>
        }
      </div>

      <!-- Email -->
      <div>
        <label for="email" class="block text-sm font-medium text-gray-700 mb-1">
          Email
        </label>
        <input
          id="email"
          type="email"
          formControlName="email"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="Enter email"
        />
        @if (registerForm.get('email')?.touched && registerForm.get('email')?.errors) {
          <p class="mt-1 text-sm text-red-600">
            @if (registerForm.get('email')?.errors?.['required']) {
              Email is required.
            } @else if (registerForm.get('email')?.errors?.['email']) {
              Please enter a valid email address.
            }
          </p>
        }
      </div>

      <!-- Password -->
      <div>
        <label for="password" class="block text-sm font-medium text-gray-700 mb-1">
          Password
        </label>
        <div class="relative">
          <input
            id="password"
            [type]="showPassword() ? 'text' : 'password'"
            formControlName="password"
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 pr-10"
            placeholder="Enter password"
          />
          <button
            type="button"
            (click)="togglePasswordVisibility()"
            class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-500 hover:text-gray-700"
          >
            @if (showPassword()) {
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M3.707 2.293a1 1 0 00-1.414 1.414l14 14a1 1 0 001.414-1.414l-1.473-1.473A10.014 10.014 0 0019.542 10C18.268 5.943 14.478 3 10 3a9.958 9.958 0 00-4.512 1.074l-1.78-1.781zm4.261 4.26l1.514 1.515a2.003 2.003 0 012.45 2.45l1.514 1.514a4 4 0 00-5.478-5.478z" clip-rule="evenodd" />
                <path d="M12.454 16.697L9.75 13.992a4 4 0 01-3.742-3.741L2.335 6.578A9.98 9.98 0 00.458 10c1.274 4.057 5.065 7 9.542 7 .847 0 1.669-.105 2.454-.303z" />
              </svg>
            } @else {
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd" />
              </svg>
            }
          </button>
        </div>
        @if (registerForm.get('password')?.touched && registerForm.get('password')?.errors) {
          <p class="mt-1 text-sm text-red-600">
            @if (registerForm.get('password')?.errors?.['required']) {
              Password is required.
            } @else if (registerForm.get('password')?.errors?.['minlength']) {
              Password must be at least 8 characters.
            }
          </p>
        }
      </div>

      <!-- Confirm Password -->
      <div>
        <label for="confirmPassword" class="block text-sm font-medium text-gray-700 mb-1">
          Confirm Password
        </label>
        <div class="relative">
          <input
            id="confirmPassword"
            [type]="showConfirmPassword() ? 'text' : 'password'"
            formControlName="confirmPassword"
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 pr-10"
            placeholder="Confirm password"
          />
          <button
            type="button"
            (click)="toggleConfirmPasswordVisibility()"
            class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-500 hover:text-gray-700"
          >
            @if (showConfirmPassword()) {
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M3.707 2.293a1 1 0 00-1.414 1.414l14 14a1 1 0 001.414-1.414l-1.473-1.473A10.014 10.014 0 0019.542 10C18.268 5.943 14.478 3 10 3a9.958 9.958 0 00-4.512 1.074l-1.78-1.781zm4.261 4.26l1.514 1.515a2.003 2.003 0 012.45 2.45l1.514 1.514a4 4 0 00-5.478-5.478z" clip-rule="evenodd" />
                <path d="M12.454 16.697L9.75 13.992a4 4 0 01-3.742-3.741L2.335 6.578A9.98 9.98 0 00.458 10c1.274 4.057 5.065 7 9.542 7 .847 0 1.669-.105 2.454-.303z" />
              </svg>
            } @else {
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd" />
              </svg>
            }
          </button>
        </div>
        @if (registerForm.get('confirmPassword')?.touched &&
             (registerForm.get('confirmPassword')?.errors?.['required'] || registerForm.errors?.['passwordMismatch'])) {
          <p class="mt-1 text-sm text-red-600">
            @if (registerForm.get('confirmPassword')?.errors?.['required']) {
              Please confirm your password.
            } @else if (registerForm.errors?.['passwordMismatch']) {
              Passwords do not match.
            }
          </p>
        }
      </div>

      <!-- Submit Button -->
      <button
        type="submit"
        [disabled]="registerForm.invalid || loading()"
        class="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        @if (loading()) {
          <span>Creating account...</span>
        } @else {
          <span>Create Account</span>
        }
      </button>
    </form>

    <div class="mt-4 text-center text-sm text-gray-600">
      <p>Already have an account?
        <a routerLink="/login" class="text-blue-600 hover:text-blue-800 font-medium">Log in</a>
      </p>
    </div>
  </div>
</div>
```

---

### Task 13: Add "Sign up" link to `LoginComponent` -- COMPLETED

**Files:**
- Modify: `frontend/src/app/features/auth/login/login.component.html`
- Modify: `frontend/src/app/features/auth/login/login.component.ts`

**Step 1: Add `RouterLink` import to `login.component.ts`**

Update the imports array to include `RouterLink`:

```typescript
import { RouterLink } from '@angular/router';
```

And update the component decorator:

```typescript
imports: [CommonModule, ReactiveFormsModule, RouterLink],
```

**Step 2: Replace the default credentials text in `login.component.html`**

Replace the bottom `<div>` section:

```html
<div class="mt-4 text-center text-sm text-gray-600">
  <p>Default credentials: admin / admin123</p>
</div>
```

With:

```html
<div class="mt-4 text-center text-sm text-gray-600">
  <p>Don't have an account?
    <a routerLink="/register" class="text-blue-600 hover:text-blue-800 font-medium">Sign up</a>
  </p>
  <p class="mt-2">Default credentials: admin / admin123</p>
</div>
```

**Step 3: Verify frontend compilation**

```bash
cd frontend && pnpm lint
```

Expected: No linting errors.

---

## Batch 6: Full-Stack Verification

### Task 14: Verify full-stack compilation and tests -- COMPLETED

**Step 1: Backend — compile and test**

```bash
cd backend && mvn clean compile -q && mvn test -q
```

Expected: BUILD SUCCESS, all tests pass.

**Step 2: Frontend — lint**

```bash
cd frontend && pnpm lint
```

Expected: No errors.

**Step 3: Docker dev smoke test (if Docker is available)**

```bash
cd <project-root> && docker-compose -f docker-compose.dev.yml up --build -d
```

Wait for services to start, then verify:

1. Open `http://localhost:4200/login` — confirm "Sign up" link appears
2. Click "Sign up" — confirm registration form loads at `/register`
3. Fill in username/email/password/confirm — submit
4. Confirm redirect to `/dashboard` with the new user logged in
5. Open `http://localhost:8080/swagger-ui.html` — confirm `POST /api/auth/register` appears under Authentication tag

```bash
docker-compose -f docker-compose.dev.yml down
```

---

## Summary of All Files

| # | Layer | File | Action |
|---|-------|------|--------|
| 1 | Backend | `application/dto/request/RegisterRequest.java` | Create |
| 2 | Backend | `application/service/AuthService.java` | Modify |
| 3 | Backend | `presentation/controller/AuthController.java` | Modify |
| 4 | Backend | `pom.xml` | Modify |
| 5 | Backend | `presentation/exception/RateLimitExceededException.java` | Create |
| 6 | Backend | `presentation/exception/GlobalExceptionHandler.java` | Modify |
| 7 | Backend | `infrastructure/config/RateLimitFilter.java` | Create |
| 8 | Backend | `application/service/AuthServiceTest.java` (test) | Create |
| 9 | Frontend | `core/services/auth.service.ts` | Modify |
| 10 | Frontend | `app.routes.ts` | Modify |
| 11 | Frontend | `features/auth/register/register.component.ts` | Create |
| 12 | Frontend | `features/auth/register/register.component.html` | Create |
| 13 | Frontend | `features/auth/login/login.component.ts` | Modify |
| 14 | Frontend | `features/auth/login/login.component.html` | Modify |
