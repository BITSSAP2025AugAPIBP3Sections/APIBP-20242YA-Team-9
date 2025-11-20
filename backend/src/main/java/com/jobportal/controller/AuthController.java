package com.jobportal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.jobportal.dto.AuthResponse;
import com.jobportal.dto.LoginRequest;
import com.jobportal.dto.RegisterRequest;
import com.jobportal.entity.User;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.jwt.JwtUtils;
import com.jobportal.service.EmailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication v1", description = "User authentication and registration endpoints - Version 1 API")
public class AuthController {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private EmailService emailService;

    @Operation(
        summary = "Register a new user",
        description = "Create a new user account with the specified role (ADMIN, COMPANY, or APPLICANT)",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User registration data",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RegisterRequest.class),
                examples = @ExampleObject(
                    name = "Register Example",
                    value = """
                        {
                          "name": "John Doe",
                          "email": "john@example.com",
                          "password": "securePassword123",
                          "role": "APPLICANT"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User registered successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": "success",
                          "message": "User registered successfully",
                          "data": {
                            "userId": 123,
                            "name": "John Doe",
                            "email": "john@example.com",
                            "role": "APPLICANT"
                          },
                          "action": "registration",
                          "timestamp": "2024-11-16T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Registration failed - Email already exists or validation error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": "error",
                          "message": "Registration failed: Email already exists",
                          "email": "john@example.com",
                          "timestamp": "2024-11-16T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Validated @RequestBody RegisterRequest req) {
        try {
            if (userRepo.findByEmail(req.getEmail()).isPresent()) {
                Map<String, Object> response = Map.of(
                    "status", "error",
                    "message", "Registration failed: Email already exists",
                    "email", req.getEmail(),
                    "timestamp", java.time.Instant.now().toString()
                );
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = new User(
                    req.getName(),
                    req.getEmail(),
                    passwordEncoder.encode(req.getPassword()),
                    req.getRole()
            );
            User savedUser = userRepo.save(user);
//        emailService.sendRegistrationEmail(user.getEmail(), user.getName());
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "User registered successfully",
                "data", Map.of(
                    "userId", savedUser.getId(),
                    "name", savedUser.getName(),
                    "email", savedUser.getEmail(),
                    "role", savedUser.getRole()
                ),
                "action", "registration",
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Registration failed: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    @Operation(
        summary = "User login",
        description = "Authenticate user and return JWT token with user details and expiration information",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "User login credentials",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoginRequest.class),
                examples = @ExampleObject(
                    name = "Login Example",
                    value = """
                        {
                          "email": "john@example.com",
                          "password": "securePassword123"
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": "success",
                          "message": "Login successful",
                          "data": {
                            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                            "expiresAt": 1700123400000,
                            "expiresAtISO": "Sat Nov 16 12:30:00 UTC 2024",
                            "user": {
                              "id": 123,
                              "name": "John Doe",
                              "email": "john@example.com",
                              "role": "APPLICANT",
                              "bio": ""
                            }
                          },
                          "action": "authentication",
                          "timestamp": "2024-11-16T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication failed - Invalid credentials",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "status": "error",
                          "message": "Authentication failed: Invalid email or password",
                          "email": "john@example.com",
                          "timestamp": "2024-11-16T10:30:00Z"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Validated @RequestBody LoginRequest req) {
        try {
            Optional<User> userOpt = userRepo.findByEmail(req.getEmail());
            if (userOpt.isEmpty() ||
                    !passwordEncoder.matches(req.getPassword(), userOpt.get().getPassword())) {
                Map<String, Object> response = Map.of(
                    "status", "error",
                    "message", "Authentication failed: Invalid email or password",
                    "email", req.getEmail(),
                    "timestamp", java.time.Instant.now().toString()
                );
                return ResponseEntity.status(401).body(response);
            }
            
            User user = userOpt.get();
            String token = jwtUtils.generateToken(user.getEmail(), user.getId(), user.getRole());
            
            // Calculate expiration time
            long currentTime = System.currentTimeMillis();
            long expirationTime = currentTime + jwtUtils.getExpirationTimeInMs();
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Login successful",
                "data", Map.of(
                    "token", token,
                    "expiresAt", expirationTime,
                    "expiresAtISO", new java.util.Date(expirationTime).toString(),
                    "user", Map.of(
                        "id", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail(),
                        "role", user.getRole(),
                        "bio", user.getBio() == null ? "" : user.getBio()
                    )
                ),
                "action", "authentication",
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Login failed: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            );
            return ResponseEntity.status(500).body(response);
        }
    }
}
