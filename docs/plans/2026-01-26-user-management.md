# Implementation Plan: Comprehensive User Management Upgrade

## Overview
Four features delivered across **8 batches** with review checkpoints between each.

| Feature | Batches |
|---|---|
| Soft-Delete + Hard Purge | 1, 3, 7 |
| Server-Side Search & Filtering | 2, 5 |
| User Edit Slide-Out Panel | 6 |
| Bulk Operations | 3, 7 |
| Backend Tests | 8 |

**Dependency order**: Batch 1 is foundational (soft-delete changes the entity/repo). Batches 2-3 depend on 1. Batch 4 (frontend service) depends on 1-3. Batches 5-7 depend on 4. Batch 8 is independent but logically last.

---

## Batch 1: Soft-Delete Infrastructure (Backend)

### Task 1.1 — Flyway Migration V9: Add `deleted_at` Column

**File**: `backend/src/main/resources/db/migration/V9__add_soft_delete_to_users.sql` *(new file)*

**IMPORTANT**: File must use LF line endings (per `.gitattributes` rules).

```sql
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP NULL;

CREATE INDEX idx_users_deleted_at ON users(deleted_at);
```

**Verification**: Run `mvn spring-boot:run` with dev profile. Check H2 console at `/h2-console` — `users` table should have `deleted_at` column. Flyway schema_history should show V9 as success.

---

### Task 1.2 — Update User Entity with `deletedAt` Field

**File**: `backend/src/main/java/com/boilerplate/domain/model/User.java`

Add this field after `credentialsNonExpired`:

```java
@Column(name = "deleted_at")
private LocalDateTime deletedAt;
```

Add this import at the top:

```java
import java.time.LocalDateTime;
```

**Do NOT** add `@Where(clause = "deleted_at IS NULL")` — we handle filtering explicitly via Specification to support the "show deleted" toggle.

**Verification**: Application starts without Hibernate validation errors. Existing queries still work.

---

### Task 1.3 — Update UserRepository: Add Soft-Delete Aware Queries

**File**: `backend/src/main/java/com/boilerplate/domain/repository/UserRepository.java`

Replace the entire interface with:

```java
package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByUsernameAndDeletedAtIsNull(String username);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<User> findByUsernameWithRolesAndPermissions(String username);

    @Modifying
    @Query("UPDATE User u SET u.deletedAt = :deletedAt WHERE u.id = :id AND u.deletedAt IS NULL")
    int softDeleteById(Long id, LocalDateTime deletedAt);

    @Modifying
    @Query("UPDATE User u SET u.deletedAt = null WHERE u.id = :id AND u.deletedAt IS NOT NULL")
    int restoreById(Long id);

    @Modifying
    @Query("UPDATE User u SET u.deletedAt = :deletedAt WHERE u.id IN :ids AND u.deletedAt IS NULL")
    int softDeleteByIds(List<Long> ids, LocalDateTime deletedAt);

    long countByDeletedAtIsNullAndRolesName(String roleName);
}
```

**Key changes**:
- Extends `JpaSpecificationExecutor<User>` (needed for Batch 2)
- All `findBy`/`existsBy` methods now filter `deletedAt IS NULL`
- Old methods (`findByUsername`, `existsByUsername`, etc.) are removed — replaced by soft-delete-aware versions
- Added `softDeleteById`, `restoreById`, `softDeleteByIds` modifying queries
- Added `countByDeletedAtIsNullAndRolesName` to check if last admin

**Verification**: Application compiles. Existing login still works (findByUsernameWithRolesAndPermissions filters deleted).

---

### Task 1.4 — Update CustomUserDetailsService: Block Deleted Users

**File**: `backend/src/main/java/com/boilerplate/infrastructure/security/CustomUserDetailsService.java`

No change needed. The `findByUsernameWithRolesAndPermissions` query already filters `deletedAt IS NULL` after Task 1.3. Soft-deleted users will get `UsernameNotFoundException` on their next login attempt. Existing tokens expire naturally (15 min max).

**Verification**: Soft-delete a user via H2 console (`UPDATE users SET deleted_at = NOW() WHERE username = 'testuser'`). Confirm they cannot log in. Confirm admin can still log in.

---

### Task 1.5 — Update UserService: Soft-Delete Instead of Hard-Delete

**File**: `backend/src/main/java/com/boilerplate/application/service/UserService.java`

Replace all `existsByUsername`/`existsByEmail`/`findByUsername` calls with their new `*AndDeletedAtIsNull` counterparts.

Full replacement of the class:

```java
package com.boilerplate.application.service;

import com.boilerplate.application.dto.request.CreateUserRequest;
import com.boilerplate.application.dto.request.UpdateUserRequest;
import com.boilerplate.application.dto.response.UserResponse;
import com.boilerplate.application.mapper.UserMapper;
import com.boilerplate.domain.model.Role;
import com.boilerplate.domain.model.User;
import com.boilerplate.domain.repository.RoleRepository;
import com.boilerplate.domain.repository.UserRepository;
import com.boilerplate.presentation.exception.DuplicateResourceException;
import com.boilerplate.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> cb.isNull(root.get("deletedAt"));
        return userRepository.findAll(spec, pageable)
            .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
            .filter(u -> u.getDeletedAt() == null)
            .map(userMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
            .map(userMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.debug("Creating user: {}", request.getUsername());

        if (userRepository.existsByUsernameAndDeletedAtIsNull(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = roleRepository.findAllByIdWithPermissions(request.getRoleIds());
            user.setRoles(roles);
        } else {
            roleRepository.findByName("USER")
                .ifPresent(role -> user.setRoles(Set.of(role)));
        }

        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", savedUser.getUsername());

        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        log.debug("Updating user with id: {}", id);

        User user = userRepository.findById(id)
            .filter(u -> u.getDeletedAt() == null)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsernameAndDeletedAtIsNull(request.getUsername())) {
                throw new DuplicateResourceException("Username already exists: " + request.getUsername());
            }
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
                throw new DuplicateResourceException("Email already exists: " + request.getEmail());
            }
        }

        userMapper.updateEntity(user, request);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoleIds() != null) {
            Set<Role> roles = roleRepository.findAllByIdWithPermissions(request.getRoleIds());
            user.setRoles(roles);
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", updatedUser.getUsername());

        return userMapper.toResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        log.debug("Soft-deleting user with id: {}", id);

        int updated = userRepository.softDeleteById(id, LocalDateTime.now());
        if (updated == 0) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        log.info("User soft-deleted successfully with id: {}", id);
    }

    @Transactional
    public UserResponse restoreUser(Long id) {
        log.debug("Restoring user with id: {}", id);

        int updated = userRepository.restoreById(id);
        if (updated == 0) {
            throw new ResourceNotFoundException("Deleted user not found with id: " + id);
        }

        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        log.info("User restored successfully: {}", user.getUsername());
        return userMapper.toResponse(user);
    }

    @Transactional
    public void purgeUser(Long id) {
        log.debug("Permanently deleting user with id: {}", id);

        User user = userRepository.findById(id)
            .filter(u -> u.getDeletedAt() != null)
            .orElseThrow(() -> new ResourceNotFoundException("Deleted user not found with id: " + id));

        userRepository.delete(user);
        log.info("User permanently deleted: {}", user.getUsername());
    }

    @Transactional
    public int bulkSoftDelete(List<Long> ids) {
        log.debug("Bulk soft-deleting users: {}", ids);

        // Guard: prevent deleting the last admin
        for (Long id : ids) {
            userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .ifPresent(user -> {
                    boolean isAdmin = user.getRoles().stream()
                        .anyMatch(r -> "ADMIN".equals(r.getName()));
                    if (isAdmin) {
                        long adminCount = userRepository.countByDeletedAtIsNullAndRolesName("ADMIN");
                        long adminsBeingDeleted = ids.stream()
                            .map(userRepository::findById)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(u -> u.getDeletedAt() == null)
                            .filter(u -> u.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName())))
                            .count();
                        if (adminCount <= adminsBeingDeleted) {
                            throw new IllegalStateException("Cannot delete the last admin user");
                        }
                    }
                });
        }

        int deleted = userRepository.softDeleteByIds(ids, LocalDateTime.now());
        log.info("Bulk soft-deleted {} users", deleted);
        return deleted;
    }

    @Transactional
    public int bulkUpdateStatus(List<Long> ids, boolean enabled) {
        log.debug("Bulk updating status for users: {} to enabled={}", ids, enabled);

        int count = 0;
        for (Long id : ids) {
            User user = userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElse(null);
            if (user != null) {
                user.setEnabled(enabled);
                userRepository.save(user);
                count++;
            }
        }

        log.info("Bulk updated status for {} users to enabled={}", count, enabled);
        return count;
    }
}
```

**Note**: The `bulkSoftDelete` method has an import issue — add `import java.util.Optional;` at the top of the file.

**Verification**: Existing delete endpoint now soft-deletes. User disappears from list but remains in DB. Login as that user fails.

---

### Task 1.6 — Update AuthService: Use Soft-Delete Aware Methods

**File**: `backend/src/main/java/com/boilerplate/application/service/AuthService.java`

Replace these two calls in the `register` method:

```java
// OLD
if (userRepository.existsByUsername(request.getUsername())) {
// NEW
if (userRepository.existsByUsernameAndDeletedAtIsNull(request.getUsername())) {
```

```java
// OLD
if (userRepository.existsByEmail(request.getEmail())) {
// NEW
if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
```

No other changes needed — `findByUsernameWithRolesAndPermissions` already filters deleted users after Task 1.3.

**Verification**: Registration still works. Cannot register with existing active username/email. CAN register with a soft-deleted user's username (because soft-deleted users don't block uniqueness).

---

### Task 1.7 — Update UserResponse: Add `deletedAt` Field

**File**: `backend/src/main/java/com/boilerplate/application/dto/response/UserResponse.java`

Add this field:

```java
private LocalDateTime deletedAt;
```

Add import:

```java
import java.time.LocalDateTime;
```

The `UserMapper.toResponse()` will automatically map this since the field name matches.

**Verification**: API responses now include `"deletedAt": null` for active users.

---

**CHECKPOINT: Review Batch 1 before proceeding.**

---

## Batch 2: Server-Side Search & Filtering (Backend)

### Task 2.1 — Create UserSpecification

**File**: `backend/src/main/java/com/boilerplate/domain/repository/UserSpecification.java` *(new file)*

```java
package com.boilerplate.domain.repository;

import com.boilerplate.domain.model.User;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> searchByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("username")), pattern),
            cb.like(cb.lower(root.get("email")), pattern)
        );
    }

    public static Specification<User> hasRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }
        return (root, query, cb) -> {
            var rolesJoin = root.join("roles", JoinType.INNER);
            return cb.equal(rolesJoin.get("name"), roleName);
        };
    }

    public static Specification<User> hasEnabled(Boolean enabled) {
        if (enabled == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("enabled"), enabled);
    }

    public static Specification<User> isDeleted(boolean includeDeleted) {
        if (includeDeleted) {
            return (root, query, cb) -> cb.isNotNull(root.get("deletedAt"));
        }
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<User> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
```

**Verification**: Class compiles. No runtime test yet — used in Task 2.3.

---

### Task 2.2 — Create UserSearchRequest DTO

**File**: `backend/src/main/java/com/boilerplate/application/dto/request/UserSearchRequest.java` *(new file)*

```java
package com.boilerplate.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchRequest {

    private String search;
    private String role;
    private Boolean enabled;
    private Boolean showDeleted;
}
```

**Verification**: Compiles.

---

### Task 2.3 — Update UserService: Add Search Method

**File**: `backend/src/main/java/com/boilerplate/application/service/UserService.java`

Add this method (keep existing `getAllUsers` for backward compatibility but it will be replaced by the controller in Task 2.4):

```java
@Transactional(readOnly = true)
public Page<UserResponse> searchUsers(UserSearchRequest searchRequest, Pageable pageable) {
    Specification<User> spec = Specification.where(null);

    if (Boolean.TRUE.equals(searchRequest.getShowDeleted())) {
        spec = spec.and(UserSpecification.isDeleted(true));
    } else {
        spec = spec.and(UserSpecification.isNotDeleted());
    }

    Specification<User> keywordSpec = UserSpecification.searchByKeyword(searchRequest.getSearch());
    if (keywordSpec != null) {
        spec = spec.and(keywordSpec);
    }

    Specification<User> roleSpec = UserSpecification.hasRole(searchRequest.getRole());
    if (roleSpec != null) {
        spec = spec.and(roleSpec);
    }

    Specification<User> enabledSpec = UserSpecification.hasEnabled(searchRequest.getEnabled());
    if (enabledSpec != null) {
        spec = spec.and(enabledSpec);
    }

    return userRepository.findAll(spec, pageable)
        .map(userMapper::toResponse);
}
```

Add this import:

```java
import com.boilerplate.application.dto.request.UserSearchRequest;
import com.boilerplate.domain.repository.UserSpecification;
```

**Verification**: Unit-testable. Full integration test in Batch 8.

---

### Task 2.4 — Update UserController: Add Search Query Params

**File**: `backend/src/main/java/com/boilerplate/presentation/controller/UserController.java`

Replace the `getAllUsers` method:

```java
@GetMapping
@PreAuthorize("hasAuthority('USER_READ')")
@Operation(summary = "Search users", description = "Search and filter users with pagination")
public ResponseEntity<Page<UserResponse>> getAllUsers(
    @RequestParam(required = false) String search,
    @RequestParam(required = false) String role,
    @RequestParam(required = false) Boolean enabled,
    @RequestParam(required = false, defaultValue = "false") Boolean showDeleted,
    Pageable pageable
) {
    UserSearchRequest searchRequest = UserSearchRequest.builder()
        .search(search)
        .role(role)
        .enabled(enabled)
        .showDeleted(showDeleted)
        .build();
    return ResponseEntity.ok(userService.searchUsers(searchRequest, pageable));
}
```

Add import:

```java
import com.boilerplate.application.dto.request.UserSearchRequest;
```

**Verification**:
- `GET /api/users` — returns all active users (backward compatible)
- `GET /api/users?search=admin` — filters by username/email
- `GET /api/users?role=ADMIN` — filters by role
- `GET /api/users?enabled=true` — filters by status
- `GET /api/users?showDeleted=true` — shows only deleted users
- `GET /api/users?search=test&role=USER&enabled=true` — combines all filters
- `GET /api/users?sort=username,asc` — sorts by username (Spring Data Pageable handles this)

---

**CHECKPOINT: Review Batch 2 before proceeding.**

---

## Batch 3: Soft-Delete Endpoints + Bulk Operations (Backend)

### Task 3.1 — Create BulkActionRequest DTO

**File**: `backend/src/main/java/com/boilerplate/application/dto/request/BulkActionRequest.java` *(new file)*

```java
package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkActionRequest {

    @NotEmpty(message = "User IDs are required")
    private List<Long> userIds;
}
```

**Verification**: Compiles.

---

### Task 3.2 — Create BulkStatusRequest DTO

**File**: `backend/src/main/java/com/boilerplate/application/dto/request/BulkStatusRequest.java` *(new file)*

```java
package com.boilerplate.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkStatusRequest {

    @NotEmpty(message = "User IDs are required")
    private List<Long> userIds;

    @NotNull(message = "Enabled status is required")
    private Boolean enabled;
}
```

**Verification**: Compiles.

---

### Task 3.3 — Create BulkActionResponse DTO

**File**: `backend/src/main/java/com/boilerplate/application/dto/response/BulkActionResponse.java` *(new file)*

```java
package com.boilerplate.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkActionResponse {
    private int affected;
    private String message;
}
```

**Verification**: Compiles.

---

### Task 3.4 — Update UserController: Add Restore, Purge, and Bulk Endpoints

**File**: `backend/src/main/java/com/boilerplate/presentation/controller/UserController.java`

Add these methods to the existing controller:

```java
@PostMapping("/{id}/restore")
@PreAuthorize("hasAuthority('USER_MANAGE')")
@Operation(summary = "Restore user", description = "Restore a soft-deleted user")
public ResponseEntity<UserResponse> restoreUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.restoreUser(id));
}

@DeleteMapping("/{id}/purge")
@PreAuthorize("hasAuthority('USER_DELETE') and hasAuthority('SYSTEM_MANAGE')")
@Operation(summary = "Purge user", description = "Permanently delete a soft-deleted user")
public ResponseEntity<Void> purgeUser(@PathVariable Long id) {
    userService.purgeUser(id);
    return ResponseEntity.noContent().build();
}

@PostMapping("/bulk/delete")
@PreAuthorize("hasAuthority('USER_DELETE')")
@Operation(summary = "Bulk delete users", description = "Soft-delete multiple users")
public ResponseEntity<BulkActionResponse> bulkDelete(@Valid @RequestBody BulkActionRequest request) {
    int affected = userService.bulkSoftDelete(request.getUserIds());
    return ResponseEntity.ok(BulkActionResponse.builder()
        .affected(affected)
        .message(affected + " users deleted")
        .build());
}

@PostMapping("/bulk/status")
@PreAuthorize("hasAuthority('USER_UPDATE')")
@Operation(summary = "Bulk update status", description = "Enable or disable multiple users")
public ResponseEntity<BulkActionResponse> bulkUpdateStatus(@Valid @RequestBody BulkStatusRequest request) {
    int affected = userService.bulkUpdateStatus(request.getUserIds(), request.getEnabled());
    return ResponseEntity.ok(BulkActionResponse.builder()
        .affected(affected)
        .message(affected + " users updated")
        .build());
}
```

Add imports:

```java
import com.boilerplate.application.dto.request.BulkActionRequest;
import com.boilerplate.application.dto.request.BulkStatusRequest;
import com.boilerplate.application.dto.response.BulkActionResponse;
```

**Verification** (Swagger UI):
- `POST /api/users/3/restore` — restores soft-deleted user
- `DELETE /api/users/3/purge` — permanently removes soft-deleted user (requires ADMIN)
- `POST /api/users/bulk/delete` with `{"userIds": [2, 3]}` — soft-deletes multiple
- `POST /api/users/bulk/status` with `{"userIds": [2, 3], "enabled": false}` — disables multiple

---

### Task 3.5 — Add `GET /api/roles` Endpoint for Frontend Role Filter

The frontend role filter dropdown needs a list of available roles. Add a simple endpoint.

**File**: `backend/src/main/java/com/boilerplate/presentation/controller/RoleController.java` *(new file)*

```java
package com.boilerplate.presentation.controller;

import com.boilerplate.application.dto.response.RoleResponse;
import com.boilerplate.application.mapper.RoleMapper;
import com.boilerplate.domain.repository.RoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Roles", description = "Role management endpoints")
public class RoleController {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ')")
    @Operation(summary = "Get all roles", description = "Retrieve list of all roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        List<RoleResponse> roles = roleRepository.findAll().stream()
            .map(roleMapper::toResponse)
            .toList();
        return ResponseEntity.ok(roles);
    }
}
```

**Verification**: `GET /api/roles` returns `[{id: 1, name: "ADMIN", ...}, ...]`

---

**CHECKPOINT: Review Batch 3 before proceeding.**

---

## Batch 4: Frontend Service Updates

### Task 4.1 — Create Shared Model Interfaces

Extract interfaces from `auth.service.ts` into a dedicated models file for reuse.

**File**: `frontend/src/app/core/models/user.model.ts` *(new file)*

```typescript
export interface UserResponse {
  id: number;
  username: string;
  email: string;
  enabled: boolean;
  roles: RoleResponse[];
  deletedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RoleResponse {
  id: number;
  name: string;
  description: string;
  permissions: PermissionResponse[];
}

export interface PermissionResponse {
  id: number;
  name: string;
  description: string;
  resource: string;
  action: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  roleIds?: number[];
}

export interface UpdateUserRequest {
  username?: string;
  email?: string;
  password?: string;
  enabled?: boolean;
  roleIds?: number[];
}

export interface BulkActionRequest {
  userIds: number[];
}

export interface BulkStatusRequest {
  userIds: number[];
  enabled: boolean;
}

export interface BulkActionResponse {
  affected: number;
  message: string;
}

export interface UserSearchParams {
  search?: string;
  role?: string;
  enabled?: boolean;
  showDeleted?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}
```

**Verification**: Compiles with `pnpm build` (no runtime test yet).

---

### Task 4.2 — Update AuthService: Use Shared Models

**File**: `frontend/src/app/core/services/auth.service.ts`

Replace the inline interfaces with imports from the shared models file. Keep `LoginRequest`, `RegisterRequest`, and `AuthResponse` in this file since they're auth-specific.

Replace these lines at the top:

```typescript
// REMOVE the inline UserResponse, RoleResponse, PermissionResponse interfaces
// ADD this import:
import { UserResponse, RoleResponse, PermissionResponse } from '../models/user.model';
```

Keep `LoginRequest`, `RegisterRequest`, `AuthResponse` as-is in this file.

Remove the inline `UserResponse`, `RoleResponse`, `PermissionResponse` interface declarations from this file.

**Verification**: `pnpm build` succeeds. Login still works.

---

### Task 4.3 — Update UserService: Full CRUD + Search + Bulk

**File**: `frontend/src/app/services/user.service.ts`

Replace entire file:

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  UserResponse,
  RoleResponse,
  PageResponse,
  CreateUserRequest,
  UpdateUserRequest,
  BulkActionRequest,
  BulkStatusRequest,
  BulkActionResponse,
  UserSearchParams,
} from '../core/models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/users`;

  searchUsers(params: UserSearchParams): Observable<PageResponse<UserResponse>> {
    let httpParams = new HttpParams()
      .set('page', (params.page ?? 0).toString())
      .set('size', (params.size ?? 10).toString());

    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.role) httpParams = httpParams.set('role', params.role);
    if (params.enabled !== undefined) httpParams = httpParams.set('enabled', params.enabled.toString());
    if (params.showDeleted) httpParams = httpParams.set('showDeleted', 'true');
    if (params.sort) httpParams = httpParams.set('sort', params.sort);

    return this.http.get<PageResponse<UserResponse>>(this.apiUrl, { params: httpParams });
  }

  getUserById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/${id}`);
  }

  createUser(user: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.apiUrl, user);
  }

  updateUser(id: number, user: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}`, user);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  restoreUser(id: number): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.apiUrl}/${id}/restore`, {});
  }

  purgeUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/purge`);
  }

  bulkDelete(request: BulkActionRequest): Observable<BulkActionResponse> {
    return this.http.post<BulkActionResponse>(`${this.apiUrl}/bulk/delete`, request);
  }

  bulkUpdateStatus(request: BulkStatusRequest): Observable<BulkActionResponse> {
    return this.http.post<BulkActionResponse>(`${this.apiUrl}/bulk/status`, request);
  }

  getRoles(): Observable<RoleResponse[]> {
    return this.http.get<RoleResponse[]>(`${environment.apiUrl}/roles`);
  }
}
```

**Verification**: `pnpm build` succeeds.

---

### Task 4.4 — Update AuthResponse import in AuthService

**File**: `frontend/src/app/core/services/auth.service.ts`

The `AuthResponse` interface in `auth.service.ts` references `UserResponse`. After Task 4.2, `UserResponse` is imported from the models file. Ensure the `AuthResponse` interface still references the correct `UserResponse`:

```typescript
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;  // This now comes from the import
}
```

Also re-export `UserResponse` so existing consumers (`user-list.component.ts`, etc.) don't break:

Add at the bottom of `auth.service.ts`:

```typescript
export { UserResponse, RoleResponse, PermissionResponse } from '../models/user.model';
```

**Verification**: `pnpm build` succeeds. All existing imports still resolve.

---

**CHECKPOINT: Review Batch 4 before proceeding.**

---

## Batch 5: Frontend — Search & Filter UI

### Task 5.1 — Update UserListComponent TypeScript

**File**: `frontend/src/app/features/users/user-list/user-list.component.ts`

Replace entire file:

```typescript
import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../services/user.service';
import { AuthService, UserResponse } from '../../../core/services/auth.service';
import { RoleResponse, PageResponse, UserSearchParams } from '../../../core/models/user.model';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-list.component.html'
})
export class UserListComponent implements OnInit {
  private userService = inject(UserService);
  authService = inject(AuthService);

  users = signal<PageResponse<UserResponse> | null>(null);
  roles = signal<RoleResponse[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  page = signal(0);
  size = signal(10);

  // Search & filter state
  searchTerm = signal('');
  selectedRole = signal('');
  selectedStatus = signal<string>('');
  showDeleted = signal(false);
  sortField = signal('id');
  sortDirection = signal<'asc' | 'desc'>('asc');

  // Selection state for bulk operations
  selectedIds = signal<Set<number>>(new Set());
  allOnPageSelected = computed(() => {
    const data = this.users();
    if (!data || data.content.length === 0) return false;
    const ids = this.selectedIds();
    return data.content.every(u => ids.has(u.id));
  });
  selectionCount = computed(() => this.selectedIds().size);

  // Edit panel state
  editingUser = signal<UserResponse | null>(null);
  panelOpen = signal(false);

  Math = Math;

  private searchSubject = new Subject<string>();

  ngOnInit(): void {
    this.loadUsers();
    this.loadRoles();

    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(term => {
      this.searchTerm.set(term);
      this.page.set(0);
      this.loadUsers();
    });
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  onRoleChange(role: string): void {
    this.selectedRole.set(role);
    this.page.set(0);
    this.loadUsers();
  }

  onStatusChange(status: string): void {
    this.selectedStatus.set(status);
    this.page.set(0);
    this.loadUsers();
  }

  onShowDeletedChange(show: boolean): void {
    this.showDeleted.set(show);
    this.page.set(0);
    this.selectedIds.set(new Set());
    this.loadUsers();
  }

  onSort(field: string): void {
    if (this.sortField() === field) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortField.set(field);
      this.sortDirection.set('asc');
    }
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.error.set(null);

    const params: UserSearchParams = {
      page: this.page(),
      size: this.size(),
      sort: `${this.sortField()},${this.sortDirection()}`
    };

    if (this.searchTerm()) params.search = this.searchTerm();
    if (this.selectedRole()) params.role = this.selectedRole();
    if (this.selectedStatus() !== '') params.enabled = this.selectedStatus() === 'true';
    if (this.showDeleted()) params.showDeleted = true;

    this.userService.searchUsers(params).subscribe({
      next: (data) => {
        this.users.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load users');
        this.loading.set(false);
        console.error(err);
      }
    });
  }

  loadRoles(): void {
    this.userService.getRoles().subscribe({
      next: (roles) => this.roles.set(roles),
      error: (err) => console.error('Failed to load roles', err)
    });
  }

  nextPage(): void {
    const data = this.users();
    if (data && this.page() < data.totalPages - 1) {
      this.page.update(p => p + 1);
      this.loadUsers();
    }
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.update(p => p - 1);
      this.loadUsers();
    }
  }

  // --- Selection ---

  toggleSelection(id: number): void {
    this.selectedIds.update(ids => {
      const next = new Set(ids);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  toggleAllOnPage(): void {
    const data = this.users();
    if (!data) return;

    if (this.allOnPageSelected()) {
      this.selectedIds.update(ids => {
        const next = new Set(ids);
        data.content.forEach(u => next.delete(u.id));
        return next;
      });
    } else {
      this.selectedIds.update(ids => {
        const next = new Set(ids);
        data.content.forEach(u => next.add(u.id));
        return next;
      });
    }
  }

  isSelected(id: number): boolean {
    return this.selectedIds().has(id);
  }

  clearSelection(): void {
    this.selectedIds.set(new Set());
  }

  // --- Single Actions ---

  deleteUser(id: number): void {
    if (!confirm('Are you sure you want to delete this user?')) return;

    this.userService.deleteUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        alert('Failed to delete user');
        console.error(err);
      }
    });
  }

  restoreUser(id: number): void {
    this.userService.restoreUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        alert('Failed to restore user');
        console.error(err);
      }
    });
  }

  purgeUser(user: UserResponse): void {
    const confirmation = prompt(`Type "${user.username}" to permanently delete this user:`);
    if (confirmation !== user.username) return;

    this.userService.purgeUser(user.id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        alert('Failed to purge user');
        console.error(err);
      }
    });
  }

  // --- Bulk Actions ---

  bulkDelete(): void {
    const ids = Array.from(this.selectedIds());
    if (!confirm(`Delete ${ids.length} user(s)?`)) return;

    this.userService.bulkDelete({ userIds: ids }).subscribe({
      next: (res) => {
        this.clearSelection();
        this.loadUsers();
      },
      error: (err) => {
        alert(err.error?.message || 'Bulk delete failed');
        console.error(err);
      }
    });
  }

  bulkEnable(): void {
    const ids = Array.from(this.selectedIds());
    this.userService.bulkUpdateStatus({ userIds: ids, enabled: true }).subscribe({
      next: () => {
        this.clearSelection();
        this.loadUsers();
      },
      error: (err) => {
        alert('Bulk enable failed');
        console.error(err);
      }
    });
  }

  bulkDisable(): void {
    const ids = Array.from(this.selectedIds());
    this.userService.bulkUpdateStatus({ userIds: ids, enabled: false }).subscribe({
      next: () => {
        this.clearSelection();
        this.loadUsers();
      },
      error: (err) => {
        alert('Bulk disable failed');
        console.error(err);
      }
    });
  }

  // --- Edit Panel ---

  openEditPanel(user: UserResponse): void {
    this.editingUser.set(user);
    this.panelOpen.set(true);
  }

  closeEditPanel(): void {
    this.editingUser.set(null);
    this.panelOpen.set(false);
  }

  onUserUpdated(): void {
    this.closeEditPanel();
    this.loadUsers();
  }
}
```

**Verification**: `pnpm build` succeeds (template will be updated in Task 5.2).

---

### Task 5.2 — Update UserListComponent Template

**File**: `frontend/src/app/features/users/user-list/user-list.component.html`

Replace entire file:

```html
<div class="container mx-auto px-4 py-8">
  <!-- Header -->
  <div class="flex justify-between items-center mb-6">
    <h1 class="text-3xl font-bold">Users</h1>
    @if (authService.hasPermission('USER_CREATE')) {
      <button class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
        Add User
      </button>
    }
  </div>

  <!-- Search & Filters Bar -->
  <div class="bg-white shadow-md rounded-lg p-4 mb-4 flex flex-wrap gap-4 items-center">
    <!-- Search -->
    <div class="flex-1 min-w-[200px]">
      <input
        type="text"
        placeholder="Search by username or email..."
        (input)="onSearchInput($event)"
        class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
    </div>

    <!-- Role Filter -->
    <select
      (change)="onRoleChange($any($event.target).value)"
      class="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
    >
      <option value="">All Roles</option>
      @for (role of roles(); track role.id) {
        <option [value]="role.name">{{ role.name }}</option>
      }
    </select>

    <!-- Status Filter -->
    <select
      (change)="onStatusChange($any($event.target).value)"
      class="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
    >
      <option value="">All Statuses</option>
      <option value="true">Active</option>
      <option value="false">Inactive</option>
    </select>

    <!-- Show Deleted Toggle -->
    @if (authService.hasPermission('USER_MANAGE')) {
      <label class="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
        <input
          type="checkbox"
          [checked]="showDeleted()"
          (change)="onShowDeletedChange($any($event.target).checked)"
          class="h-4 w-4 text-red-600 focus:ring-red-500 border-gray-300 rounded"
        />
        Show Deleted
      </label>
    }
  </div>

  <!-- Bulk Action Bar -->
  @if (selectionCount() > 0) {
    <div class="bg-blue-50 border border-blue-200 rounded-lg p-3 mb-4 flex items-center gap-3">
      <span class="text-sm font-medium text-blue-800">{{ selectionCount() }} selected</span>
      <div class="flex gap-2 ml-auto">
        @if (!showDeleted()) {
          @if (authService.hasPermission('USER_UPDATE')) {
            <button (click)="bulkEnable()" class="px-3 py-1 text-sm bg-green-600 text-white rounded hover:bg-green-700">
              Enable
            </button>
            <button (click)="bulkDisable()" class="px-3 py-1 text-sm bg-yellow-600 text-white rounded hover:bg-yellow-700">
              Disable
            </button>
          }
          @if (authService.hasPermission('USER_DELETE')) {
            <button (click)="bulkDelete()" class="px-3 py-1 text-sm bg-red-600 text-white rounded hover:bg-red-700">
              Delete
            </button>
          }
        }
        <button (click)="clearSelection()" class="px-3 py-1 text-sm bg-gray-200 text-gray-700 rounded hover:bg-gray-300">
          Clear
        </button>
      </div>
    </div>
  }

  @if (loading()) {
    <div class="text-center py-8">
      <p>Loading users...</p>
    </div>
  } @else if (error()) {
    <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
      {{ error() }}
    </div>
  } @else if (users()) {
    <div class="bg-white shadow-md rounded-lg overflow-hidden">
      <table class="min-w-full divide-y divide-gray-200">
        <thead class="bg-gray-50">
          <tr>
            <!-- Checkbox Column -->
            <th class="px-4 py-3 w-10">
              <input
                type="checkbox"
                [checked]="allOnPageSelected()"
                (change)="toggleAllOnPage()"
                class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              />
            </th>
            <th (click)="onSort('id')" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:text-gray-700">
              ID
              @if (sortField() === 'id') {
                <span>{{ sortDirection() === 'asc' ? ' &#9650;' : ' &#9660;' }}</span>
              }
            </th>
            <th (click)="onSort('username')" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:text-gray-700">
              Username
              @if (sortField() === 'username') {
                <span>{{ sortDirection() === 'asc' ? ' &#9650;' : ' &#9660;' }}</span>
              }
            </th>
            <th (click)="onSort('email')" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer hover:text-gray-700">
              Email
              @if (sortField() === 'email') {
                <span>{{ sortDirection() === 'asc' ? ' &#9650;' : ' &#9660;' }}</span>
              }
            </th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Roles</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-gray-200">
          @for (user of users()!.content; track user.id) {
            <tr [class.bg-red-50]="user.deletedAt" [class.opacity-60]="user.deletedAt">
              <!-- Checkbox -->
              <td class="px-4 py-4 w-10">
                <input
                  type="checkbox"
                  [checked]="isSelected(user.id)"
                  (change)="toggleSelection(user.id)"
                  class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">{{ user.id }}</td>
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium" [class.line-through]="user.deletedAt">
                {{ user.username }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm" [class.line-through]="user.deletedAt">
                {{ user.email }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">
                @for (role of user.roles; track role.id) {
                  <span class="inline-flex items-center px-2 py-1 mr-1 text-xs font-medium bg-blue-100 text-blue-800 rounded">
                    {{ role.name }}
                  </span>
                }
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">
                @if (user.deletedAt) {
                  <span class="text-red-600 font-medium">Deleted</span>
                } @else if (user.enabled) {
                  <span class="text-green-600">Active</span>
                } @else {
                  <span class="text-yellow-600">Inactive</span>
                }
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm">
                @if (user.deletedAt) {
                  @if (authService.hasPermission('USER_MANAGE')) {
                    <button (click)="restoreUser(user.id)" class="text-green-600 hover:text-green-900 mr-3">Restore</button>
                  }
                  @if (authService.hasPermission('USER_DELETE') && authService.hasPermission('SYSTEM_MANAGE')) {
                    <button (click)="purgeUser(user)" class="text-red-600 hover:text-red-900">Purge</button>
                  }
                } @else {
                  @if (authService.hasPermission('USER_UPDATE')) {
                    <button (click)="openEditPanel(user)" class="text-blue-600 hover:text-blue-900 mr-3">Edit</button>
                  }
                  @if (authService.hasPermission('USER_DELETE')) {
                    <button (click)="deleteUser(user.id)" class="text-red-600 hover:text-red-900">Delete</button>
                  }
                }
              </td>
            </tr>
          }
        </tbody>
      </table>

      <!-- Pagination -->
      <div class="bg-gray-50 px-6 py-3 flex items-center justify-between border-t">
        <div class="text-sm text-gray-700">
          Showing {{ users()!.number * users()!.size + 1 }} to {{ Math.min((users()!.number + 1) * users()!.size, users()!.totalElements) }} of {{ users()!.totalElements }} results
        </div>
        <div class="flex gap-2">
          <button
            (click)="previousPage()"
            [disabled]="page() === 0"
            class="px-4 py-2 border rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Previous
          </button>
          <button
            (click)="nextPage()"
            [disabled]="page() >= users()!.totalPages - 1"
            class="px-4 py-2 border rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Next
          </button>
        </div>
      </div>
    </div>
  }
</div>

<!-- Edit Panel Overlay -->
@if (panelOpen()) {
  <app-user-edit-panel
    [user]="editingUser()!"
    [roles]="roles()"
    (close)="closeEditPanel()"
    (saved)="onUserUpdated()"
  />
}
```

**Note**: The `<app-user-edit-panel>` component doesn't exist yet — it will be created in Batch 6. Until then, add it to the imports conditionally or comment out the last block. The component import will be added in Batch 6.

**Verification**: `pnpm build` succeeds (after commenting out the edit panel block temporarily). Search, filter, sort, bulk selection all functional.

---

**CHECKPOINT: Review Batch 5 before proceeding.**

---

## Batch 6: Frontend — Slide-Out Edit Panel

### Task 6.1 — Create UserEditPanelComponent TypeScript

**File**: `frontend/src/app/features/users/user-edit-panel/user-edit-panel.component.ts` *(new file)*

```typescript
import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../../../services/user.service';
import { UserResponse, RoleResponse, UpdateUserRequest } from '../../../core/models/user.model';

@Component({
  selector: 'app-user-edit-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-edit-panel.component.html'
})
export class UserEditPanelComponent implements OnInit {
  @Input({ required: true }) user!: UserResponse;
  @Input() roles: RoleResponse[] = [];
  @Output() close = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private userService = inject(UserService);

  loading = signal(false);
  error = signal<string | null>(null);
  showPassword = signal(false);

  editForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    password: [''],
    enabled: [true],
    roleIds: [[] as number[]]
  });

  ngOnInit(): void {
    this.editForm.patchValue({
      username: this.user.username,
      email: this.user.email,
      enabled: this.user.enabled,
      roleIds: this.user.roles.map(r => r.id)
    });
  }

  toggleRole(roleId: number): void {
    const current = this.editForm.get('roleIds')!.value;
    const index = current.indexOf(roleId);
    if (index === -1) {
      this.editForm.get('roleIds')!.setValue([...current, roleId]);
    } else {
      this.editForm.get('roleIds')!.setValue(current.filter((id: number) => id !== roleId));
    }
  }

  isRoleSelected(roleId: number): boolean {
    return this.editForm.get('roleIds')!.value.includes(roleId);
  }

  onSubmit(): void {
    if (this.editForm.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const formValue = this.editForm.getRawValue();
    const request: UpdateUserRequest = {};

    if (formValue.username !== this.user.username) request.username = formValue.username;
    if (formValue.email !== this.user.email) request.email = formValue.email;
    if (formValue.password) request.password = formValue.password;
    if (formValue.enabled !== this.user.enabled) request.enabled = formValue.enabled;

    const originalRoleIds = this.user.roles.map(r => r.id).sort();
    const newRoleIds = formValue.roleIds.sort();
    if (JSON.stringify(originalRoleIds) !== JSON.stringify(newRoleIds)) {
      request.roleIds = formValue.roleIds;
    }

    // Only send if there are changes
    if (Object.keys(request).length === 0) {
      this.close.emit();
      return;
    }

    this.userService.updateUser(this.user.id, request).subscribe({
      next: () => {
        this.loading.set(false);
        this.saved.emit();
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Failed to update user');
        this.loading.set(false);
      }
    });
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('panel-overlay')) {
      this.close.emit();
    }
  }
}
```

**Verification**: Compiles.

---

### Task 6.2 — Create UserEditPanelComponent Template

**File**: `frontend/src/app/features/users/user-edit-panel/user-edit-panel.component.html` *(new file)*

```html
<!-- Overlay -->
<div
  class="panel-overlay fixed inset-0 bg-black bg-opacity-30 z-40"
  (click)="onOverlayClick($event)"
>
  <!-- Slide-out Panel -->
  <div class="fixed inset-y-0 right-0 w-full max-w-md bg-white shadow-xl z-50 overflow-y-auto">
    <!-- Header -->
    <div class="flex items-center justify-between px-6 py-4 border-b">
      <h2 class="text-lg font-semibold">Edit User</h2>
      <button (click)="close.emit()" class="text-gray-400 hover:text-gray-600">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <!-- Body -->
    <div class="px-6 py-4">
      @if (error()) {
        <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
          {{ error() }}
        </div>
      }

      <!-- User Meta -->
      <div class="mb-4 text-sm text-gray-500 space-y-1">
        <p>ID: {{ user.id }}</p>
        <p>Created: {{ user.createdAt | date:'medium' }}</p>
        <p>Updated: {{ user.updatedAt | date:'medium' }}</p>
      </div>

      <form [formGroup]="editForm" (ngSubmit)="onSubmit()" class="space-y-4">
        <!-- Username -->
        <div>
          <label for="edit-username" class="block text-sm font-medium text-gray-700 mb-1">Username</label>
          <input
            id="edit-username"
            type="text"
            formControlName="username"
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          @if (editForm.get('username')?.touched && editForm.get('username')?.errors) {
            <p class="mt-1 text-sm text-red-600">
              @if (editForm.get('username')?.errors?.['required']) {
                Username is required.
              } @else if (editForm.get('username')?.errors?.['minlength']) {
                Username must be at least 3 characters.
              }
            </p>
          }
        </div>

        <!-- Email -->
        <div>
          <label for="edit-email" class="block text-sm font-medium text-gray-700 mb-1">Email</label>
          <input
            id="edit-email"
            type="email"
            formControlName="email"
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          @if (editForm.get('email')?.touched && editForm.get('email')?.errors) {
            <p class="mt-1 text-sm text-red-600">Please enter a valid email address.</p>
          }
        </div>

        <!-- Password (optional) -->
        <div>
          <label for="edit-password" class="block text-sm font-medium text-gray-700 mb-1">
            New Password <span class="text-gray-400">(leave blank to keep current)</span>
          </label>
          <div class="relative">
            <input
              id="edit-password"
              [type]="showPassword() ? 'text' : 'password'"
              formControlName="password"
              placeholder="Enter new password"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 pr-10"
            />
            <button
              type="button"
              (click)="showPassword.set(!showPassword())"
              class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-500 hover:text-gray-700"
            >
              {{ showPassword() ? 'Hide' : 'Show' }}
            </button>
          </div>
        </div>

        <!-- Enabled Toggle -->
        <div class="flex items-center gap-3">
          <label class="relative inline-flex items-center cursor-pointer">
            <input
              type="checkbox"
              formControlName="enabled"
              class="sr-only peer"
            />
            <div class="w-11 h-6 bg-gray-200 peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
          </label>
          <span class="text-sm font-medium text-gray-700">Account Enabled</span>
        </div>

        <!-- Roles -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Roles</label>
          <div class="space-y-2">
            @for (role of roles; track role.id) {
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  [checked]="isRoleSelected(role.id)"
                  (change)="toggleRole(role.id)"
                  class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <span class="text-sm">
                  <span class="font-medium">{{ role.name }}</span>
                  @if (role.description) {
                    <span class="text-gray-500"> - {{ role.description }}</span>
                  }
                </span>
              </label>
            }
          </div>
        </div>

        <!-- Actions -->
        <div class="flex gap-3 pt-4 border-t">
          <button
            type="submit"
            [disabled]="editForm.invalid || loading()"
            class="flex-1 bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            @if (loading()) {
              Saving...
            } @else {
              Save Changes
            }
          </button>
          <button
            type="button"
            (click)="close.emit()"
            class="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  </div>
</div>
```

**Verification**: File created.

---

### Task 6.3 — Wire Edit Panel into UserListComponent

**File**: `frontend/src/app/features/users/user-list/user-list.component.ts`

Add the import for `UserEditPanelComponent`:

```typescript
import { UserEditPanelComponent } from '../user-edit-panel/user-edit-panel.component';
```

Update the `imports` array in the `@Component` decorator:

```typescript
imports: [CommonModule, FormsModule, UserEditPanelComponent],
```

Now uncomment / ensure the `<app-user-edit-panel>` block at the bottom of the template is present (from Task 5.2).

**Verification**: Click "Edit" on a user row — panel slides in from the right. Edit username, save — panel closes, table refreshes with updated data.

---

**CHECKPOINT: Review Batch 6 before proceeding.**

---

## Batch 7: Frontend — Final Polish

### Task 7.1 — Handle Empty States

In `user-list.component.html`, after the `@for` loop inside `<tbody>`, add an empty state row:

```html
@empty {
  <tr>
    <td colspan="7" class="px-6 py-8 text-center text-gray-500">
      @if (showDeleted()) {
        No deleted users found.
      } @else if (searchTerm() || selectedRole() || selectedStatus() !== '') {
        No users match your filters. Try adjusting your search criteria.
      } @else {
        No users found.
      }
    </td>
  </tr>
}
```

Place this right after the closing `}` of the `@for` block and before `</tbody>`.

**Verification**: Clear all users or apply a filter that returns no results — empty state message shows.

---

### Task 7.2 — Add Keyboard Support to Edit Panel

**File**: `frontend/src/app/features/users/user-edit-panel/user-edit-panel.component.ts`

Add `HostListener` for Escape key:

```typescript
import { Component, EventEmitter, HostListener, Input, OnInit, Output, inject, signal } from '@angular/core';
```

Add method:

```typescript
@HostListener('document:keydown.escape')
onEscapeKey(): void {
  this.close.emit();
}
```

**Verification**: Open edit panel, press Escape — panel closes.

---

**CHECKPOINT: Review Batch 7 before proceeding.**

---

## Batch 8: Backend Unit Tests

### Task 8.1 — Update UserServiceTest

**File**: `backend/src/test/java/com/boilerplate/application/service/UserServiceTest.java`

Add these test methods to the existing test class:

```java
@Test
void deleteUser_SoftDeletes() {
    // Arrange
    when(userRepository.softDeleteById(eq(1L), any(LocalDateTime.class))).thenReturn(1);

    // Act
    userService.deleteUser(1L);

    // Assert
    verify(userRepository).softDeleteById(eq(1L), any(LocalDateTime.class));
    verify(userRepository, never()).deleteById(anyLong());
}

@Test
void deleteUser_NotFound_ThrowsException() {
    // Arrange
    when(userRepository.softDeleteById(eq(1L), any(LocalDateTime.class))).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> userService.deleteUser(1L))
        .isInstanceOf(ResourceNotFoundException.class);
}

@Test
void restoreUser_Success() {
    // Arrange
    when(userRepository.restoreById(1L)).thenReturn(1);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

    // Act
    UserResponse result = userService.restoreUser(1L);

    // Assert
    assertThat(result).isNotNull();
    verify(userRepository).restoreById(1L);
}

@Test
void restoreUser_NotFound_ThrowsException() {
    // Arrange
    when(userRepository.restoreById(1L)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> userService.restoreUser(1L))
        .isInstanceOf(ResourceNotFoundException.class);
}

@Test
void purgeUser_OnlyDeletesSoftDeletedUsers() {
    // Arrange
    testUser.setDeletedAt(LocalDateTime.now());
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // Act
    userService.purgeUser(1L);

    // Assert
    verify(userRepository).delete(testUser);
}

@Test
void purgeUser_ActiveUser_ThrowsException() {
    // Arrange - user has deletedAt = null
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    // Act & Assert
    assertThatThrownBy(() -> userService.purgeUser(1L))
        .isInstanceOf(ResourceNotFoundException.class);
}

@Test
void searchUsers_NoFilters_ReturnsAllActive() {
    // Arrange
    UserSearchRequest searchRequest = UserSearchRequest.builder().build();
    Page<User> page = new PageImpl<>(List.of(testUser));
    when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
    when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

    // Act
    Page<UserResponse> result = userService.searchUsers(searchRequest, Pageable.unpaged());

    // Assert
    assertThat(result.getContent()).hasSize(1);
}
```

Add these imports to the test file:

```java
import com.boilerplate.application.dto.request.UserSearchRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;
import java.util.List;
```

**Verification**: `mvn test` — all tests pass.

---

### Task 8.2 — Update Existing Tests for Soft-Delete Aware Methods

In `UserServiceTest.java`, update the existing `createUser_DuplicateUsername_ThrowsException` test:

```java
@Test
void createUser_DuplicateUsername_ThrowsException() {
    // Arrange
    when(userRepository.existsByUsernameAndDeletedAtIsNull(anyString())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> userService.createUser(createRequest))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("Username already exists");

    verify(userRepository, never()).save(any());
}
```

Update `createUser_Success`:

```java
@Test
void createUser_Success() {
    // Arrange
    when(userRepository.existsByUsernameAndDeletedAtIsNull(anyString())).thenReturn(false);
    when(userRepository.existsByEmailAndDeletedAtIsNull(anyString())).thenReturn(false);
    when(userMapper.toEntity(any())).thenReturn(testUser);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(roleRepository.findByName("USER")).thenReturn(Optional.of(new Role()));
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(userMapper.toResponse(any())).thenReturn(testUserResponse);

    // Act
    UserResponse result = userService.createUser(createRequest);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getUsername()).isEqualTo("testuser");
    verify(userRepository).save(any(User.class));
}
```

Update `deleteUser_Success`:

```java
@Test
void deleteUser_Success() {
    // Arrange
    when(userRepository.softDeleteById(eq(1L), any(LocalDateTime.class))).thenReturn(1);

    // Act
    userService.deleteUser(1L);

    // Assert
    verify(userRepository).softDeleteById(eq(1L), any(LocalDateTime.class));
}
```

Update `deleteUser_NotFound_ThrowsException`:

```java
@Test
void deleteUser_NotFound_ThrowsException() {
    // Arrange
    when(userRepository.softDeleteById(eq(1L), any(LocalDateTime.class))).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> userService.deleteUser(1L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User not found");
}
```

**Verification**: `mvn test` — all tests pass (old and new).

---

**CHECKPOINT: Review Batch 8 before proceeding.**

---

## File Change Summary

### New Files (10)
| File | Layer |
|---|---|
| `backend/src/main/resources/db/migration/V9__add_soft_delete_to_users.sql` | Database |
| `backend/src/main/java/com/boilerplate/domain/repository/UserSpecification.java` | Domain |
| `backend/src/main/java/com/boilerplate/application/dto/request/UserSearchRequest.java` | Application |
| `backend/src/main/java/com/boilerplate/application/dto/request/BulkActionRequest.java` | Application |
| `backend/src/main/java/com/boilerplate/application/dto/request/BulkStatusRequest.java` | Application |
| `backend/src/main/java/com/boilerplate/application/dto/response/BulkActionResponse.java` | Application |
| `backend/src/main/java/com/boilerplate/presentation/controller/RoleController.java` | Presentation |
| `frontend/src/app/core/models/user.model.ts` | Frontend Core |
| `frontend/src/app/features/users/user-edit-panel/user-edit-panel.component.ts` | Frontend Feature |
| `frontend/src/app/features/users/user-edit-panel/user-edit-panel.component.html` | Frontend Feature |

### Modified Files (10)
| File | Changes |
|---|---|
| `backend/.../domain/model/User.java` | Add `deletedAt` field |
| `backend/.../domain/repository/UserRepository.java` | Extend `JpaSpecificationExecutor`, soft-delete queries |
| `backend/.../application/service/UserService.java` | Soft-delete, restore, purge, bulk ops, search method |
| `backend/.../application/service/AuthService.java` | Use `*AndDeletedAtIsNull` methods |
| `backend/.../application/dto/response/UserResponse.java` | Add `deletedAt` field |
| `backend/.../presentation/controller/UserController.java` | Search params, restore/purge/bulk endpoints |
| `frontend/src/app/core/services/auth.service.ts` | Extract shared models, re-export |
| `frontend/src/app/services/user.service.ts` | Full rewrite with search, bulk, CRUD |
| `frontend/src/app/features/users/user-list/user-list.component.ts` | Search, filter, sort, bulk, edit panel state |
| `frontend/src/app/features/users/user-list/user-list.component.html` | Full template rewrite |

### Unchanged Files
All other files (migrations V1-V8, login, register, dashboard, auth guard, interceptors, security config, mappers, etc.) remain unchanged.
