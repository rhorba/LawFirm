package com.boilerplate.application.service;

import com.boilerplate.application.dto.request.CreateUserRequest;
import com.boilerplate.application.dto.request.UserSearchRequest;
import com.boilerplate.application.dto.response.UserResponse;
import com.boilerplate.application.mapper.UserMapper;
import com.boilerplate.domain.model.Group;
import com.boilerplate.domain.model.Role;
import com.boilerplate.domain.model.User;
import com.boilerplate.domain.repository.GroupRepository;
import com.boilerplate.domain.repository.RoleRepository;
import com.boilerplate.domain.repository.UserRepository;
import com.boilerplate.presentation.exception.DuplicateResourceException;
import com.boilerplate.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditPublisher auditPublisher;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserResponse testUserResponse;
    private CreateUserRequest createRequest;
    private Group defaultGroup;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .password("encodedPassword")
            .enabled(true)
            .build();

        testUserResponse = UserResponse.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .enabled(true)
            .build();

        createRequest = CreateUserRequest.builder()
            .username("newuser")
            .email("new@example.com")
            .password("password123")
            .build();

        defaultGroup = Group.builder()
            .id(1L)
            .name("Default Users")
            .description("Default group for new users")
            .build();
    }

    @Test
    void createUser_Success() {
        // Arrange
        when(userRepository.existsByUsernameAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(userMapper.toEntity(any())).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(groupRepository.findByName("Default Users")).thenReturn(Optional.of(defaultGroup));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(any())).thenReturn(testUserResponse);

        // Act
        UserResponse result = userService.createUser(createRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(groupRepository).findByName("Default Users");
        verify(userRepository, times(2)).save(any(User.class));
    }

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

    @Test
    void getUserById_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        // Act
        UserResponse result = userService.getUserById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void deleteUser_Success() {
        // Arrange
        when(userRepository.softDeleteById(eq(1L), any(LocalDateTime.class))).thenReturn(1);

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository).softDeleteById(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void deleteUser_NotFound_ThrowsException() {
        // Arrange
        when(userRepository.softDeleteById(eq(1L), any(LocalDateTime.class))).thenReturn(0);

        // Act & Assert
        assertThatThrownBy(() -> userService.deleteUser(1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");
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

    @SuppressWarnings("unchecked")
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
}
