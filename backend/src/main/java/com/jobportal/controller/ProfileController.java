package com.jobportal.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.jobportal.dto.ProfileUpdateDTO;
import com.jobportal.security.CustomUserDetails;
import com.jobportal.service.ProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@RestController
@RequestMapping("/api/profiles")
@Tag(name = "Profiles", description = "User profile management")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    // Get current user's profile
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get my profile",
        description = "Retrieve the profile information of the current authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            Map<String, Object> profile = profileService.getUserProfile(userDetails.getUser().getId());
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Profile retrieved successfully",
                "data", profile,
                "userId", userDetails.getUser().getId(),
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Failed to retrieve profile: " + e.getMessage(),
                "userId", userDetails.getUser().getId(),
                "timestamp", java.time.Instant.now().toString()
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    // Get any user's profile (public)
    @GetMapping("/{userId}")
    @Operation(
        summary = "Get user profile by ID",
        description = "Retrieve public profile information of any user by their ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - User not found"),
        @ApiResponse(responseCode = "404", description = "User profile not found")
    })
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable Long userId) {
        try {
            Map<String, Object> profile = profileService.getUserProfile(userId);
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Profile retrieved successfully",
                "data", profile,
                "userId", userId,
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Failed to retrieve profile: " + e.getMessage(),
                "userId", userId,
                "timestamp", java.time.Instant.now().toString()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Update current user's profile
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Update my profile",
        description = "Update the profile information of the current authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - Invalid profile data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
        @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProfileUpdateDTO updateDTO) {
        try {
            Map<String, Object> updatedProfile = profileService.updateUserProfile(
                    userDetails.getUser().getId(), updateDTO);
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Profile updated successfully",
                "data", updatedProfile,
                "userId", userDetails.getUser().getId(),
                "action", "update",
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Failed to update profile: " + e.getMessage(),
                "userId", userDetails.getUser().getId(),
                "timestamp", java.time.Instant.now().toString()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Admin can update any user's profile
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Admin: Update user profile",
        description = "Allow administrators to update any user's profile information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - Invalid profile data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> updateUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody ProfileUpdateDTO updateDTO) {
        try {
            Map<String, Object> updatedProfile = profileService.updateUserProfile(userId, updateDTO);
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Profile updated successfully by admin",
                "data", updatedProfile,
                "userId", userId,
                "action", "admin_update",
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Failed to update profile: " + e.getMessage(),
                "userId", userId,
                "timestamp", java.time.Instant.now().toString()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }
} 