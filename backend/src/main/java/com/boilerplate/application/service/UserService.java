package com.boilerplate.application.service;

import com.boilerplate.application.dto.request.CreateUserRequest;
import com.boilerplate.application.dto.request.UpdateUserRequest;
import com.boilerplate.application.dto.request.UserSearchRequest;
import com.boilerplate.application.dto.response.UserResponse;
import com.boilerplate.application.mapper.UserMapper;
import com.boilerplate.domain.model.Group;
import com.boilerplate.domain.model.User;
import com.boilerplate.domain.repository.GroupRepository;
import com.boilerplate.domain.repository.UserRepository;
import com.boilerplate.domain.repository.UserSpecification;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditPublisher auditPublisher;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> cb.isNull(root.get("deletedAt"));
        return userRepository.findAll(spec, pageable)
            .map(userMapper::toResponse);
    }

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

        User savedUser = userRepository.save(user);

        // Assign user to Default Users group
        Group defaultGroup = groupRepository.findByName("Default Users")
            .orElseThrow(() -> new RuntimeException("Default Users group not found"));

        savedUser.getGroups().add(defaultGroup);
        savedUser = userRepository.save(savedUser);
        log.info("User created successfully: {}", savedUser.getUsername());

        auditPublisher.publish(
            "USER_CREATE",
            "USER",
            savedUser.getId().toString(),
            "Created user: " + savedUser.getUsername()
        );

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

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", updatedUser.getUsername());

        auditPublisher.publish(
            "USER_UPDATE",
            "USER",
            updatedUser.getId().toString(),
            "Updated user profile"
        );

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

        auditPublisher.publish(
            "USER_DELETE",
            "USER",
            id.toString(),
            "Soft-deleted user"
        );
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

        auditPublisher.publish(
            "USER_RESTORE",
            "USER",
            id.toString(),
            "Restored user"
        );

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
        
                auditPublisher.publish(
                    "USER_PURGE",
                    "USER",
                    id.toString(),
                    "Permanently deleted user: " + user.getUsername()
                );
            }
    @Transactional
    public int bulkSoftDelete(List<Long> ids) {
        log.debug("Bulk soft-deleting users: {}", ids);

        long adminCount = userRepository.countByDeletedAtIsNullAndGroupsRolesName("ADMIN");
        long adminsInBatch = ids.stream()
            .map(userRepository::findById)
            .flatMap(java.util.Optional::stream)
            .filter(u -> u.getDeletedAt() == null)
            .filter(u -> u.getGroups().stream()
                .flatMap(g -> g.getRoles().stream())
                .anyMatch(r -> "ADMIN".equals(r.getName())))
            .count();

        if (adminCount > 0 && adminCount <= adminsInBatch) {
            throw new IllegalStateException("Cannot delete the last admin user");
        }

        int deleted = userRepository.softDeleteByIds(ids, LocalDateTime.now());
        log.info("Bulk soft-deleted {} users", deleted);

        auditPublisher.publish(
            "USER_BULK_DELETE",
            "USER",
            "N/A",
            "Bulk deleted " + deleted + " users. IDs: " + ids
        );

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

        auditPublisher.publish(
            "USER_BULK_STATUS",
            "USER",
            "N/A",
            "Bulk status update to " + enabled + " for " + count + " users. IDs: " + ids
        );

        return count;
    }
}
