package com.boilerplate.application.service;

import com.boilerplate.application.dto.request.LoginRequest;
import com.boilerplate.application.dto.request.RegisterRequest;
import com.boilerplate.application.dto.response.AuthResponse;
import com.boilerplate.application.dto.response.UserResponse;
import com.boilerplate.application.mapper.UserMapper;
import com.boilerplate.domain.model.Group;
import com.boilerplate.domain.model.User;
import com.boilerplate.domain.repository.GroupRepository;
import com.boilerplate.domain.repository.UserRepository;
import com.boilerplate.infrastructure.security.JwtService;
import com.boilerplate.presentation.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuditPublisher auditPublisher;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.debug("Login attempt for user: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(
            userDetails,
            Boolean.TRUE.equals(request.getRememberMe())
        );

        UserResponse userResponse = userRepository.findByUsernameWithRolesAndPermissions(request.getUsername())
            .map(userMapper::toResponse)
            .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        log.info("User logged in successfully: {}", request.getUsername());

        auditPublisher.publish(
            userResponse.getId(),
            userResponse.getUsername(),
            "LOGIN_SUCCESS",
            "AUTH",
            userResponse.getId().toString(),
            "Login successful via username/password"
        );

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(15 * 60L) // 15 minutes in seconds
            .user(userResponse)
            .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateAccessToken(userDetails);

        UserResponse userResponse = userRepository.findByUsernameWithRolesAndPermissions(username)
            .map(userMapper::toResponse)
            .orElseThrow(() -> new RuntimeException("User not found"));

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(15 * 60L)
            .user(userResponse)
            .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.debug("Registration attempt for user: {}", request.getUsername());

        if (userRepository.existsByUsernameAndDeletedAtIsNull(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build();

        User savedUser = userRepository.save(user);

        // Assign user to Default Users group
        Group defaultGroup = groupRepository.findByName("Default Users")
            .orElseThrow(() -> new RuntimeException("Default Users group not found"));

        savedUser.getGroups().add(defaultGroup);
        userRepository.save(savedUser);

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

        auditPublisher.publish(
            userResponse.getId(),
            userResponse.getUsername(),
            "USER_REGISTER",
            "USER",
            userResponse.getId().toString(),
            "Self-registration"
        );

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(15 * 60L)
            .user(userResponse)
            .build();
    }
}
