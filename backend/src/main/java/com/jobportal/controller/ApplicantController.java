package com.jobportal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.jobportal.entity.Application;
import com.jobportal.entity.Job;
import com.jobportal.entity.User;
import com.jobportal.security.CustomUserDetails;
import com.jobportal.service.ApplicantService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applicant")
@PreAuthorize("hasRole('APPLICANT')")  // Entire controller is applicant-only
public class ApplicantController {

    @Value("${app.upload.dir:${user.home}/FSAD-job-portal/Job-portal/frontend/public/uploads/resumes}")
    private String uploadDir;

    @Autowired
    private ApplicantService applicantService;

    // Get applicant profile
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            User user = userDetails.getUser();
            Map<String, Object> userData = Map.ofEntries(
                    Map.entry("id", String.valueOf(user.getId())),
                    Map.entry("name", user.getName()),
                    Map.entry("email", user.getEmail()),
                    Map.entry("role", user.getRole().toString()),
                    Map.entry("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null),
                    Map.entry("active", user.isActive()),
                    Map.entry("bio", user.getBio() != null ? user.getBio() : "No Description")
            );
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Profile retrieved successfully",
                "data", userData,
                "userId", user.getId(),
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Failed to retrieve profile: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    // Update applicant profile
    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) User updatedUser) {
        try {
            // Validate request body
            if (updatedUser == null) {
                Map<String, Object> response = Map.of(
                    "status", "error",
                    "message", "Request body is required for profile update",
                    "suggestion", "Please provide profile data in JSON format: {\"name\": \"John Doe\", \"bio\": \"Software Developer\"}",
                    "userId", userDetails.getUser().getId(),
                    "timestamp", java.time.Instant.now().toString()
                );
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = applicantService.updateProfile(userDetails.getUser().getId(), updatedUser);
            Map<String, Object> userData = Map.ofEntries(
                    Map.entry("id", String.valueOf(user.getId())),
                    Map.entry("name", user.getName()),
                    Map.entry("email", user.getEmail()),
                    Map.entry("role", user.getRole().toString()),
                    Map.entry("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null),
                    Map.entry("active", user.isActive()),
                    Map.entry("bio", user.getBio() != null ? user.getBio() : "No Description")
            );
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Profile updated successfully",
                "data", userData,
                "userId", user.getId(),
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

    // Upload resume
    @PostMapping("/resume")
    public ResponseEntity<Map<String, Object>> uploadResume(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        try {
            String resumeUrl = applicantService.uploadResume(userDetails.getUser().getId(), file);
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Resume uploaded successfully",
                "data", Map.of("resumeUrl", resumeUrl, "filename", file.getOriginalFilename()),
                "userId", userDetails.getUser().getId(),
                "action", "upload",
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Failed to upload resume: " + e.getMessage(),
                "userId", userDetails.getUser().getId(),
                "filename", file.getOriginalFilename(),
                "timestamp", java.time.Instant.now().toString()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Get resume file
    @GetMapping("/resume/{userId}/{filename}")
    @PreAuthorize("hasAnyRole('COMPANY', 'ADMIN', 'APPLICANT')")
    public ResponseEntity<Resource> getResume(
            @PathVariable Long userId,
            @PathVariable String filename,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            // Security check - only allow access if:
            // 1. User is accessing their own resume
            // 2. User is a company viewing an application
            // 3. User is an admin
            if (!userDetails.getUser().getRole().toString().equals("ADMIN") &&
                !userDetails.getUser().getId().equals(userId) &&
                !userDetails.getUser().getRole().toString().equals("COMPANY")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Path filePath = Paths.get(uploadDir)
                    .resolve(userId.toString())
                    .resolve(filename)
                    .normalize();
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = determineContentType(filename);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String determineContentType(String filename) {
        if (filename.toLowerCase().endsWith(".pdf")) {
            return "application/pdf";
        } else if (filename.toLowerCase().endsWith(".doc")) {
            return "application/msword";
        } else if (filename.toLowerCase().endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "application/octet-stream";
    }

    // Get all available jobs
    @GetMapping("/jobs")
    public ResponseEntity<Map<String, Object>> getAllJobs(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String salaryRange) {
        try {
            List<Job> jobs = applicantService.searchJobs(location, title, salaryRange);
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Jobs retrieved successfully",
                "data", jobs,
                "count", jobs.size(),
                "filters", Map.of(
                    "location", location != null ? location : "all",
                    "title", title != null ? title : "all",
                    "salaryRange", salaryRange != null ? salaryRange : "all"
                ),
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Failed to retrieve jobs: " + e.getMessage(),
                "timestamp", java.time.Instant.now().toString()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Apply for a job
    @PostMapping("/apply/{jobId}")
    public ResponseEntity<Map<String, Object>> applyToJob(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long jobId,
            @RequestParam(required = false) String resumeUrl) {
        try {
            Application application = applicantService.applyToJob(
                userDetails.getUser().getId(), jobId, resumeUrl);
                
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Applied successfully to job",
                "data", Map.of(
                    "applicationId", application.getId(),
                    "jobId", jobId,
                    "status", application.getStatus().toString(),
                    "appliedAt", application.getAppliedAt().toString()
                ),
                "userId", userDetails.getUser().getId(),
                "action", "apply",
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Failed to apply for job: " + e.getMessage(),
                "jobId", jobId,
                "userId", userDetails.getUser().getId(),
                "timestamp", java.time.Instant.now().toString()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Get my applications
    @GetMapping("/applications")
    public ResponseEntity<Map<String, Object>> getMyApplications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            List<Application> applications = applicantService.getApplicationsByApplicant(
                userDetails.getUser().getId());
            List<Map<String, Object>> applicationsList = applications.stream().map(app -> Map.of(
                "id", String.valueOf(app.getId()),
                "job", Map.of(
                    "id", String.valueOf(app.getJob().getId()),
                    "title", app.getJob().getTitle(),
                    "company", Map.of(
                        "name", app.getJob().getCompany().getName()
                    )
                ),
                "status", app.getStatus().toString(),
                "appliedAt", app.getAppliedAt().atZone(ZoneOffset.UTC).toInstant().toString()
            )).toList();
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Applications retrieved successfully",
                "data", applicationsList,
                "count", applicationsList.size(),
                "userId", userDetails.getUser().getId(),
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Failed to retrieve applications: " + e.getMessage(),
                "userId", userDetails.getUser().getId(),
                "timestamp", java.time.Instant.now().toString()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get application status
    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<Map<String, Object>> getApplicationStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long applicationId) {
        try {
            Application application = applicantService.getApplicationStatus(
                    userDetails.getUser().getId(), applicationId);
            Job job = application.getJob();
            User company = job.getCompany();
            User applicant = application.getApplicant();

            Map<String, Object> applicationData = Map.of(
                    "applicationId", applicationId,
                    "status", application.getStatus().toString(),
                    "appliedAt", application.getAppliedAt().toString(),
                    "job", Map.of(
                        "id", job.getId(),
                        "title", job.getTitle(),
                        "description", job.getDescription(),
                        "requirements", job.getRequirements(),
                        "responsibilities", job.getResponsibilities()
                    ),
                    "company", Map.of(
                            "name", company.getName(),
                            "email", company.getEmail()
                    ),
                    "applicant", Map.of(
                            "name", applicant.getName(),
                            "email", applicant.getEmail()
                    )
            );
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Application details retrieved successfully",
                "data", applicationData,
                "userId", userDetails.getUser().getId(),
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Failed to retrieve application details: " + e.getMessage(),
                "applicationId", applicationId,
                "userId", userDetails.getUser().getId(),
                "timestamp", java.time.Instant.now().toString()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Withdraw application
    @DeleteMapping("/applications/{applicationId}")
    public ResponseEntity<Map<String, Object>> withdrawApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long applicationId) {
        try {
            applicantService.withdrawApplication(userDetails.getUser().getId(), applicationId);
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Application withdrawn successfully",
                "applicationId", applicationId,
                "userId", userDetails.getUser().getId(),
                "action", "withdraw",
                "timestamp", java.time.Instant.now().toString()
            );
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = Map.of(
                "status", "error",
                "message", "Failed to withdraw application: " + e.getMessage(),
                "applicationId", applicationId,
                "userId", userDetails.getUser().getId(),
                "timestamp", java.time.Instant.now().toString()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }
}
