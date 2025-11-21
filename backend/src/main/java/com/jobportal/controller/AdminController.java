package com.jobportal.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.jobportal.dto.UserDTO;
import com.jobportal.entity.Application;
import com.jobportal.entity.Job;
import com.jobportal.entity.User;
import com.jobportal.service.AdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")  // Entire controller is admin-only
@Tag(name = "Admin v1", description = "Administrative operations (Admin role required) - Version 1")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;

    // User Management Endpoints
    @Operation(
        summary = "Get all users",
        description = "Retrieve all users with optional filtering by active status and role"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        logger.debug("Getting all users with role: {} and includeInactive: {}", role, includeInactive);
        List<User> users = adminService.getAllUsers(role, includeInactive);
        
        // Convert to DTOs to exclude sensitive data like passwords
        List<UserDTO> userDTOs = users.stream()
            .map(UserDTO::new)
            .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Users retrieved successfully");
        response.put("data", userDTOs);
        response.put("count", userDTOs.size());
        response.put("filters", Map.of("role", role != null ? role : "all", "includeInactive", includeInactive));
        response.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieve detailed information about a specific user by their ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Map<String, Object>> getUserById(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {
        logger.debug("Getting user by id: {}", id);
        
        return adminService.getUserById(id)
                .map(user -> {
                    // Convert to DTO to exclude sensitive data like password
                    UserDTO userDTO = new UserDTO(user);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "User found successfully");
                    response.put("data", userDTO);
                    response.put("userId", id);
                    response.put("timestamp", java.time.Instant.now().toString());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "error");
                    response.put("message", "User not found with ID: " + id);
                    response.put("userId", id);
                    response.put("timestamp", java.time.Instant.now().toString());
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
    }

    @DeleteMapping("/users/{id}")
    @Operation(
        summary = "Delete user",
        description = "Permanently delete a user account (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - User deletion failed"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Map<String, Object>> deleteUser(
            @Parameter(description = "User ID to delete", required = true) @PathVariable Long id) {
        logger.debug("Deleting user with id: {}", id);
        try {
            adminService.deleteUser(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "User deleted successfully");
            response.put("userId", id);
            response.put("action", "delete");
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error deleting user: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to delete user: " + e.getMessage());
            response.put("userId", id);
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/users/{id}/status")
    @Operation(
        summary = "Update user status",
        description = "Activate or deactivate a user account (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - Status update failed"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Map<String, Object>> updateUserStatus(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @Parameter(description = "Active status (true for active, false for inactive)", required = true) @RequestParam boolean active) {
        logger.debug("Updating user status. UserId: {}, active: {}", id, active);
        try {
            User user = adminService.updateUserStatus(id, active);
            
            // Convert to DTO to exclude sensitive data like password
            UserDTO userDTO = new UserDTO(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "User status updated successfully");
            response.put("data", userDTO);
            response.put("userId", id);
            response.put("newStatus", active ? "active" : "inactive");
            response.put("action", "status_update");
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error updating user status: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to update user status: " + e.getMessage());
            response.put("userId", id);
            response.put("requestedStatus", active ? "active" : "inactive");
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Job Management Endpoints
    @GetMapping("/jobs")
    @Operation(
        summary = "Get all jobs",
        description = "Retrieve all jobs with optional filtering by active status and expiration"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Jobs retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Map<String, Object>> getAllJobs(
            @Parameter(description = "Filter by active status (true/false), null for all") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Include expired jobs in results") @RequestParam(defaultValue = "false") boolean includeExpired) {
        logger.debug("Getting all jobs with active: {} and includeExpired: {}", active, includeExpired);
        List<Job> jobs = adminService.getAllJobs(active, includeExpired);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Jobs retrieved successfully");
        response.put("data", jobs);
        response.put("count", jobs.size());
        response.put("filters", Map.of("active", active != null ? active.toString() : "all", "includeExpired", includeExpired));
        response.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/jobs/{id}/status")
    @Operation(
        summary = "Update job status",
        description = "Activate or deactivate a job posting (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - Status update failed"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
        @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<Map<String, Object>> updateJobStatus(
            @Parameter(description = "Job ID", required = true) @PathVariable Long id,
            @Parameter(description = "Active status (true for active, false for inactive)", required = true) @RequestParam boolean active) {
        logger.debug("Updating job status. JobId: {}, active: {}", id, active);
        try {
            Job job = adminService.updateJobStatus(id, active);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Job status updated successfully");
            response.put("data", job);
            response.put("jobId", id);
            response.put("newStatus", active ? "active" : "inactive");
            response.put("action", "status_update");
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error updating job status: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to update job status: " + e.getMessage());
            response.put("jobId", id);
            response.put("requestedStatus", active ? "active" : "inactive");
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/jobs/{id}")
    @Operation(
        summary = "Delete job",
        description = "Permanently delete a job posting (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - Job deletion failed"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
        @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<Map<String, Object>> deleteJob(
            @Parameter(description = "Job ID to delete", required = true) @PathVariable Long id) {
        logger.debug("Deleting job with id: {}", id);
        try {
            adminService.deleteJob(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Job deleted successfully");
            response.put("jobId", id);
            response.put("action", "delete");
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error deleting job: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to delete job: " + e.getMessage());
            response.put("jobId", id);
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Application Management Endpoints
    @GetMapping("/applications")
    @Operation(
        summary = "Get all applications",
        description = "Retrieve all job applications with optional filtering by status and archived state"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Applications retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getAllApplications(
            @Parameter(description = "Filter by application status (PENDING, REVIEWED, APPROVED, REJECTED)") @RequestParam(required = false) String status,
            @Parameter(description = "Include archived applications in results") @RequestParam(defaultValue = "false") boolean includeArchived) {
        logger.debug("Getting all applications with status: {} and includeArchived: {}", status, includeArchived);
        try {
            List<Application> applications = adminService.getAllApplications(status, includeArchived);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Applications retrieved successfully");
            response.put("data", applications);
            response.put("count", applications.size());
            response.put("filters", Map.of("status", status != null ? status : "all", "includeArchived", includeArchived));
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting all applications: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to retrieve applications: " + e.getMessage());
            response.put("filters", Map.of("status", status != null ? status : "all", "includeArchived", includeArchived));
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/applications/{id}/status")
    @Operation(
        summary = "Update application status",
        description = "Update the status of a job application (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - Invalid status or update failed"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
        @ApiResponse(responseCode = "404", description = "Application not found")
    })
    public ResponseEntity<Map<String, Object>> updateApplicationStatus(
            @Parameter(description = "Application ID", required = true) @PathVariable Long id,
            @Parameter(description = "New application status (PENDING, REVIEWED, APPROVED, REJECTED)", required = true) @RequestParam String status) {
        logger.debug("Updating application status. ApplicationId: {}, status: {}", id, status);
        try {
            Application application = adminService.updateApplicationStatus(id, status);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Application status updated successfully");
            response.put("data", application);
            response.put("applicationId", id);
            response.put("newStatus", status);
            response.put("action", "status_update");
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error updating application status: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to update application status: " + e.getMessage());
            response.put("applicationId", id);
            response.put("requestedStatus", status);
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/applications/{id}")
    @Operation(
        summary = "Delete application",
        description = "Permanently delete a job application (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - Application deletion failed"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required"),
        @ApiResponse(responseCode = "404", description = "Application not found")
    })
    public ResponseEntity<Map<String, Object>> deleteApplication(
            @Parameter(description = "Application ID to delete", required = true) @PathVariable Long id) {
        logger.debug("Deleting application with id: {}", id);
        try {
            adminService.deleteApplication(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Application deleted successfully");
            response.put("applicationId", id);
            response.put("action", "delete");
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error deleting application: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to delete application: " + e.getMessage());
            response.put("applicationId", id);
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Bulk Operations
    @PostMapping("/users/bulk-status")
    @Operation(
        summary = "Bulk update user status",
        description = "Update the active status of multiple users in a single operation"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Bulk user status update completed successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"1\": true, \"2\": false, \"3\": true}"
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Bad request - Bulk update failed"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Map<String, Object>> bulkUpdateUserStatus(
            @Parameter(description = "Map of user IDs to their new active status", required = true) @RequestBody Map<Long, Boolean> userStatusMap) {
        logger.debug("Bulk updating user status for {} users", userStatusMap.size());
        try {
            List<User> updatedUsers = adminService.bulkUpdateUserStatus(userStatusMap);
            
            // Convert to DTOs to exclude sensitive data like passwords
            List<UserDTO> userDTOs = updatedUsers.stream()
                .map(UserDTO::new)
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Bulk user status update completed successfully");
            response.put("data", userDTOs);
            response.put("count", userDTOs.size());
            response.put("requestedCount", userStatusMap.size());
            response.put("action", "bulk_status_update");
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error in bulk user status update: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to bulk update user status: " + e.getMessage());
            response.put("requestedCount", userStatusMap.size());
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/jobs/bulk-status")
    @Operation(
        summary = "Bulk update job status",
        description = "Update the active status of multiple jobs in a single operation"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Bulk job status update completed successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\"1\": true, \"2\": false, \"3\": true}"
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Bad request - Bulk update failed"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Map<String, Object>> bulkUpdateJobStatus(
            @Parameter(description = "Map of job IDs to their new active status", required = true) @RequestBody Map<Long, Boolean> jobStatusMap) {
        logger.debug("Bulk updating job status for {} jobs", jobStatusMap.size());
        try {
            List<Job> updatedJobs = adminService.bulkUpdateJobStatus(jobStatusMap);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Bulk job status update completed successfully");
            response.put("data", updatedJobs);
            response.put("count", updatedJobs.size());
            response.put("requestedCount", jobStatusMap.size());
            response.put("action", "bulk_status_update");
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error in bulk job status update: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to bulk update job status: " + e.getMessage());
            response.put("requestedCount", jobStatusMap.size());
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
} 