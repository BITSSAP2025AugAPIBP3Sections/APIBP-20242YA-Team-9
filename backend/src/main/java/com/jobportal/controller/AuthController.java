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

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private EmailService emailService;

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
