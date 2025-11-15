package com.jobportal.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.jobportal.entity.Application;
import com.jobportal.entity.Job;
import com.jobportal.entity.User;
import com.jobportal.service.AdminService;

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
    public ResponseEntity<List<Map<String, Object>>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        logger.debug("Getting all users with role: {} and includeInactive: {}", role, includeInactive);
        List<User> users = adminService.getAllUsers(role, includeInactive);
        List<Map<String, Object>> response = users.stream().map(user -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("email", user.getEmail());
            map.put("role", user.getRole().toString());
            map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
            map.put("active", user.isActive());
            map.put("bio", user.getBio());
            return map;
        }).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        logger.debug("Getting user by id: {}", id);
        return adminService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        logger.debug("Deleting user with id: {}", id);
        try {
            adminService.deleteUser(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("Error deleting user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        logger.debug("Updating user status. UserId: {}, active: {}", id, active);
        try {
            User user = adminService.updateUserStatus(id, active);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            logger.error("Error updating user status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Job Management Endpoints
    @GetMapping("/jobs")
    public ResponseEntity<List<Map<String, Object>>> getAllJobs(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "false") boolean includeExpired) {
        logger.debug("Getting all jobs with active: {} and includeExpired: {}", active, includeExpired);
        List<Job> jobs = adminService.getAllJobs(active, includeExpired);
        List<Map<String, Object>> response = jobs.stream().map(job -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", job.getId());
            map.put("title", job.getTitle());
            map.put("description", job.getDescription());
            map.put("company", job.getCompany() != null ? job.getCompany().getName() : null);
            map.put("location", job.getLocation());
            map.put("active", job.isActive());
            return map;
        }).toList();
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        logger.debug("Deleting job with id: {}", id);
        try {
            adminService.deleteJob(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("Error deleting job: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Application Management Endpoints
    @GetMapping("/applications")
    public ResponseEntity<List<Application>> getAllApplications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        logger.debug("Getting all applications with status: {} and includeArchived: {}", status, includeArchived);
        List<Application> applications = adminService.getAllApplications(status, includeArchived);
        return ResponseEntity.ok(applications);
    }

    @PutMapping("/applications/{id}/status")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        logger.debug("Updating application status. ApplicationId: {}, status: {}", id, status);
        try {
            Application application = adminService.updateApplicationStatus(id, status);
            return ResponseEntity.ok(application);
        } catch (RuntimeException e) {
            logger.error("Error updating application status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/applications/{id}")
    public ResponseEntity<?> deleteApplication(@PathVariable Long id) {
        logger.debug("Deleting application with id: {}", id);
        try {
            adminService.deleteApplication(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("Error deleting application: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Bulk Operations
    @PostMapping("/users/bulk-status")
    public ResponseEntity<?> bulkUpdateUserStatus(
            @RequestBody Map<Long, Boolean> userStatusMap) {
        try {
            List<User> updatedUsers = adminService.bulkUpdateUserStatus(userStatusMap);
            return ResponseEntity.ok(updatedUsers);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/jobs/bulk-status")
    public ResponseEntity<?> bulkUpdateJobStatus(
            @RequestBody Map<Long, Boolean> jobStatusMap) {
        try {
            List<Job> updatedJobs = adminService.bulkUpdateJobStatus(jobStatusMap);
            return ResponseEntity.ok(updatedJobs);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
} 