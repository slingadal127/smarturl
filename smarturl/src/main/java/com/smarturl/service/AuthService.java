package com.smarturl.service;

import com.smarturl.dto.AuthResponse;
import com.smarturl.dto.RegisterRequest;
import com.smarturl.model.User;
import com.smarturl.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user
     * Flow: validate → check duplicate → hash password → save → generate JWT
     */
    public AuthResponse register(RegisterRequest request) {

        // Validate input
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Hash password — NEVER store plain text passwords
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Save user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .build();
        user = userRepository.save(user);

        // Generate JWT token
        String token = jwtService.generateToken(user.getEmail(), user.getId());

        log.info("New user registered: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .userId(user.getId())
                .message("Registration successful")
                .build();
    }

    /**
     * Logs in an existing user
     * Flow: find user → verify password → generate JWT
     */
    public AuthResponse login(RegisterRequest request) {

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        // Verify password against stored hash
        // BCrypt compares plain text to hash safely
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Generate new JWT token
        String token = jwtService.generateToken(user.getEmail(), user.getId());

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .userId(user.getId())
                .message("Login successful")
                .build();
    }
}