package com.jobportal.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.jobportal.entity.Application;
import com.jobportal.entity.Job;
import com.jobportal.entity.User;
import com.jobportal.service.AdminService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")  // Entire controller is admin-only
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;

    // User Management Endpoints
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        logger.debug("Getting all users with role: {} and includeInactive: {}", role, includeInactive);
        List<User> users = adminService.getAllUsers(role, includeInactive);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Users retrieved successfully");
        response.put("data", users);
        response.put("count", users.size());
        response.put("filters", Map.of("role", role != null ? role : "all", "includeInactive", includeInactive));
        response.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        logger.debug("Getting user by id: {}", id);
        
        return adminService.getUserById(id)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "User found successfully");
                    response.put("data", user);
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
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
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
    public ResponseEntity<Map<String, Object>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        logger.debug("Updating user status. UserId: {}, active: {}", id, active);
        try {
            User user = adminService.updateUserStatus(id, active);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "User status updated successfully");
            response.put("data", user);
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
    public ResponseEntity<Map<String, Object>> getAllJobs(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "false") boolean includeExpired) {
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
    public ResponseEntity<Map<String, Object>> updateJobStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
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
    public ResponseEntity<Map<String, Object>> deleteJob(@PathVariable Long id) {
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
    public ResponseEntity<Map<String, Object>> getAllApplications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
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
    public ResponseEntity<Map<String, Object>> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam String status) {
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
    public ResponseEntity<Map<String, Object>> deleteApplication(@PathVariable Long id) {
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
    public ResponseEntity<Map<String, Object>> bulkUpdateUserStatus(
            @RequestBody Map<Long, Boolean> userStatusMap) {
        logger.debug("Bulk updating user status for {} users", userStatusMap.size());
        try {
            List<User> updatedUsers = adminService.bulkUpdateUserStatus(userStatusMap);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Bulk user status update completed successfully");
            response.put("data", updatedUsers);
            response.put("count", updatedUsers.size());
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
    public ResponseEntity<Map<String, Object>> bulkUpdateJobStatus(
            @RequestBody Map<Long, Boolean> jobStatusMap) {
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