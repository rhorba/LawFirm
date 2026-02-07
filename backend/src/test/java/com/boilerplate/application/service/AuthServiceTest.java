package com.boilerplate.application.service;

import com.boilerplate.application.dto.request.RegisterRequest;
import com.boilerplate.application.dto.response.AuthResponse;
import com.boilerplate.application.dto.response.UserResponse;
import com.boilerplate.application.mapper.UserMapper;
import com.boilerplate.domain.model.Group;
import com.boilerplate.domain.model.Role;
import com.boilerplate.domain.model.User;
import com.boilerplate.domain.repository.GroupRepository;
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
    private GroupRepository groupRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuditPublisher auditPublisher;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User savedUser;
    private UserResponse userResponse;
    private Role userRole;
    private Group defaultGroup;

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

        defaultGroup = Group.builder()
            .id(1L)
            .name("Default Users")
            .description("Default group for new users")
            .build();

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
        when(userRepository.existsByUsernameAndDeletedAtIsNull("newuser")).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedAtIsNull("new@example.com")).thenReturn(false);
        when(groupRepository.findByName("Default Users")).thenReturn(Optional.of(defaultGroup));
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
        verify(groupRepository).findByName("Default Users");
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsernameAndDeletedAtIsNull("newuser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsernameAndDeletedAtIsNull("newuser")).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedAtIsNull("new@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }
}
