package com.tasktracker.backend.controller;

import com.tasktracker.backend.dto.AuthResponse;
import com.tasktracker.backend.dto.LoginRequest;
import com.tasktracker.backend.dto.RegisterRequest;
import com.tasktracker.backend.dto.UserResponse;
import com.tasktracker.backend.exception.ApiException;
import com.tasktracker.backend.model.User;
import com.tasktracker.backend.repository.UserRepository;
import com.tasktracker.backend.security.JwtService;
import com.tasktracker.backend.security.TokenBlacklistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenBlacklistService tokenBlacklistService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setBio(request.bio());

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, UserResponse.from(saved)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, UserResponse.from(user)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenBlacklistService.blacklist(authHeader.substring(7));
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}