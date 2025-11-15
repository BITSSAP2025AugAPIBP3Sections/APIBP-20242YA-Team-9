package com.jobportal.graphql.resolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;

import com.jobportal.entity.User;
import com.jobportal.enums.Role;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.CustomUserDetails;
import com.jobportal.security.jwt.JwtUtils;
import com.jobportal.service.ProfileService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class AuthResolver {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ProfileService profileService;

    @MutationMapping
    public Map<String, Object> register(@Argument Map<String, Object> input) {
        try {
            String email = (String) input.get("email");

            // Check if user already exists
            if (userRepository.findByEmail(email).isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Email already exists");
                errorResponse.put("user", null);
                return errorResponse;
            }

            // Create new user
            User user = new User(
                (String) input.get("name"),
                email,
                passwordEncoder.encode((String) input.get("password")),
                Role.valueOf((String) input.get("role"))
            );

            User savedUser = userRepository.save(user);

            // No token generation during registration - users must login separately
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully. Please login to continue.");

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", savedUser.getId());
            userMap.put("name", savedUser.getName());
            userMap.put("email", savedUser.getEmail());
            userMap.put("role", savedUser.getRole().toString());
            userMap.put("createdAt", savedUser.getCreatedAt().toString());
            userMap.put("active", savedUser.isActive());
            userMap.put("bio", savedUser.getBio());

            response.put("user", userMap);
            return response;

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Registration failed: " + e.getMessage());
            errorResponse.put("user", null);
            return errorResponse;
        }
    }

    @MutationMapping
    public Map<String, Object> login(@Argument Map<String, Object> input) {
        try {
            String email = (String) input.get("email");
            String password = (String) input.get("password");

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid email or password");
                errorResponse.put("token", null);
                errorResponse.put("user", null);
                return errorResponse;
            }

            User user = userOpt.get();
            String token = jwtUtils.generateToken(user.getEmail(), user.getId(), user.getRole());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("token", token);

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("name", user.getName());
            userMap.put("email", user.getEmail());
            userMap.put("role", user.getRole().toString());
            userMap.put("createdAt", user.getCreatedAt().toString());
            userMap.put("active", user.isActive());
            userMap.put("bio", user.getBio() != null ? user.getBio() : "");

            response.put("user", userMap);
            return response;

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Login failed: " + e.getMessage());
            errorResponse.put("token", null);
            errorResponse.put("user", null);
            return errorResponse;
        }
    }

    @QueryMapping
    public Map<String, Object> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (userDetails == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "User not authenticated");
                return errorResponse;
            }
            Map<String, Object> profile = profileService.getUserProfile(userDetails.getUser().getId());
            profile.put("success", true);
            profile.put("message", "Profile retrieved successfully");
            return profile;

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve profile: " + e.getMessage());
            return errorResponse;
        }
    }

    @QueryMapping
    public Map<String, Object> getUserProfile(@Argument Long userId) {
        try {
            Map<String, Object> profile = profileService.getUserProfile(userId);
            profile.put("success", true);
            profile.put("message", "Profile retrieved successfully");
            return profile;

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve profile: " + e.getMessage());
            return errorResponse;
        }
    }
}
