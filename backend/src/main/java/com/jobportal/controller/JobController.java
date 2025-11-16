package com.jobportal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.jobportal.dto.ApplicationStatusDTO;
import com.jobportal.entity.Application;
import com.jobportal.entity.Job;
import com.jobportal.security.CustomUserDetails;
import com.jobportal.service.JobService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Job management and search operations")
public class JobController {
    private static final Logger logger = LoggerFactory.getLogger(JobController.class);

    @Autowired
    private JobService jobService;



    @Operation(
        summary = "Create a new job posting",
        description = "Create a new job posting (requires COMPANY role)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid job data"),
        @ApiResponse(responseCode = "403", description = "Access denied - COMPANY role required")
    })
    @PostMapping
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<Map<String, Object>> createJob(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody Job job) {
        try {
            // Validate required fields
            if (job.getTitle() == null || job.getTitle().trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Job title is required and cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            if (job.getDescription() == null || job.getDescription().trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Job description is required and cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            if (job.getLocation() == null || job.getLocation().trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Job location is required and cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Job createdJob = jobService.createJob(userDetails.getUser(), job);
            
            // Format the date as ISO-8601 string (2023-05-01T00:00:00Z format)
            String formattedDate = null;
            if (createdJob.getPostedAt() != null) {
                formattedDate = createdJob.getPostedAt().toString().replace("T", "T").concat("Z");
            }
            
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("id", String.valueOf(createdJob.getId()));
            jobData.put("title", createdJob.getTitle());
            jobData.put("company", Map.of(
                    "name", createdJob.getCompany().getName(),
                    "bio", createdJob.getCompany().getBio() != null ? createdJob.getCompany().getBio() : "No Description"
            ));
            jobData.put("location", createdJob.getLocation());
            jobData.put("salaryRange", createdJob.getSalaryRange());
            jobData.put("description", createdJob.getDescription());
            jobData.put("postedAt", formattedDate);
            jobData.put("requirements", createdJob.getRequirements());
            jobData.put("responsibilities", createdJob.getResponsibilities());
            jobData.put("applicationsCount", jobService.getApplicationsCountForJob(createdJob.getId()));
            jobData.put("active", createdJob.isActive());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Job created successfully");
            response.put("data", jobData);
            response.put("action", "create");
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Invalid request data: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(
        summary = "Get all active jobs",
        description = "Retrieve all active job postings with optional filtering by location, title, and salary range (Public endpoint)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Jobs retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllActiveJobs(
            @Parameter(description = "Filter by job location") @RequestParam(required = false) String location,
            @Parameter(description = "Filter by job title") @RequestParam(required = false) String title,
            @Parameter(description = "Filter by salary range") @RequestParam(required = false) String salaryRange) {
        try {
            List<Job> jobs = jobService.searchJobs(location, title, salaryRange);
            List<Map<String, Object>> jobsList = jobs.stream()
                .sorted((j1, j2) -> {
                    if (j1.getPostedAt() == null && j2.getPostedAt() == null) return 0;
                    if (j1.getPostedAt() == null) return 1;
                    if (j2.getPostedAt() == null) return -1;
                    return j2.getPostedAt().compareTo(j1.getPostedAt()); // Descending order
                })
                .map(job -> {
                    // Format the date as ISO-8601 string (2023-05-01T00:00:00Z format)
                    String formattedDate = null;
                    if (job.getPostedAt() != null) {
                        // Convert LocalDateTime to proper ISO-8601 format with Z suffix for UTC
                        formattedDate = job.getPostedAt().toString().replace("T", "T").concat("Z");
                    }

                    Map<String, Object> jobMap = new HashMap<>();
                    jobMap.put("id", String.valueOf(job.getId()));
                    jobMap.put("title", job.getTitle());
                    jobMap.put("company", Map.of(
                        "name", job.getCompany().getName(),
                        "bio", job.getCompany().getBio() != null ? job.getCompany().getBio() : "No Description"
                    ));
                    jobMap.put("location", job.getLocation());
                    jobMap.put("salaryRange", job.getSalaryRange());
                    jobMap.put("description", job.getDescription());
                    jobMap.put("postedAt", formattedDate);
                    jobMap.put("requirements", job.getRequirements());
                    jobMap.put("responsibilities", job.getResponsibilities());
                    jobMap.put("applicationsCount", jobService.getApplicationsCountForJob(job.getId()));
                    jobMap.put("active", job.isActive());
                    
                    return jobMap;
                }).toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Jobs retrieved successfully");
            response.put("data", jobsList);
            response.put("count", jobsList.size());
            response.put("filters", Map.of(
                "location", location != null ? location : "all",
                "title", title != null ? title : "all",
                "salaryRange", salaryRange != null ? salaryRange : "all"
            ));
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to retrieve jobs: " + e.getMessage());
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get job by ID (Public)
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getJobById(@PathVariable Long id) {
        return jobService.getJobById(id)
                .map(job -> {
                    // Format the date as ISO-8601 string
                    String formattedDate = null;
                    if (job.getPostedAt() != null) {
                        formattedDate = job.getPostedAt().toString().replace("T", "T").concat("Z");
                    }
                    
                    Map<String, Object> jobData = Map.of(
                        "id", String.valueOf(job.getId()),
                        "title", job.getTitle(),
                        "company", Map.of(
                            "name", job.getCompany().getName(),
                            "bio", job.getCompany().getBio() != null ? job.getCompany().getBio() : "No Description"
                        ),
                        "location", job.getLocation(),
                        "salaryRange", job.getSalaryRange(),
                        "description", job.getDescription(),
                        "postedAt", formattedDate,
                        "requirements", job.getRequirements(),
                        "responsibilities", job.getResponsibilities(),
                        "applicationsCount", jobService.getApplicationsCountForJob(job.getId())
                    );
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "Job retrieved successfully");
                    response.put("data", jobData);
                    response.put("jobId", id);
                    response.put("timestamp", java.time.Instant.now().toString());
                    
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "Job not found",
                    "jobId", id,
                    "timestamp", java.time.Instant.now().toString()
                )));
    }

    // Update job posting (COMPANY only, must be owner)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<Map<String, Object>> updateJob(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody Job job) {
        try {
            Job updatedJob = jobService.updateJob(userDetails.getUser().getId(), id, job);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Job updated successfully");
            response.put("data", updatedJob);
            response.put("jobId", id);
            response.put("action", "update");
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to update job: " + e.getMessage());
            response.put("jobId", id);
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Delete job posting (COMPANY only, must be owner)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<?> deleteJob(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        try {
            jobService.deleteJob(userDetails.getUser().getId(), id);
            
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("status", "success");
            successResponse.put("message", String.format("Job with ID %d has been successfully deactivated (marked as inactive)", id));
            successResponse.put("jobId", id);
            successResponse.put("action", "soft_delete");
            successResponse.put("note", "Job is now hidden from public listings but data is preserved");
            successResponse.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(successResponse);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to delete job: " + e.getMessage());
            errorResponse.put("jobId", id);
            errorResponse.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Get company's job postings (COMPANY only)
    @GetMapping("/company")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<Map<String, Object>> getCompanyJobs(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            List<Job> jobs = jobService.getJobsByCompany(userDetails.getUser().getId());
            List<Map<String, Object>> jobsList = jobs.stream().map(job -> Map.ofEntries(
                Map.entry("id", String.valueOf(job.getId())),
                Map.entry("title", job.getTitle()),
                Map.entry("company", Map.of(
                    "name", job.getCompany().getName(),
                    "bio", job.getCompany().getBio() != null ? job.getCompany().getBio() : "No Description"
                )),
                Map.entry("location", job.getLocation()),
                Map.entry("salaryRange", job.getSalaryRange()),
                Map.entry("description", job.getDescription()),
                Map.entry("postedAt", job.getPostedAt() != null ? job.getPostedAt().toString().replace("T", "T").concat("Z") : null),
                Map.entry("requirements", job.getRequirements()),
                Map.entry("responsibilities", job.getResponsibilities()),
                Map.entry("applicationsCount", jobService.getApplicationsCountForJob(job.getId())),
                Map.entry("active", job.isActive())
            )).toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Company jobs retrieved successfully");
            response.put("data", jobsList);
            response.put("count", jobsList.size());
            response.put("companyId", userDetails.getUser().getId());
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to retrieve company jobs: " + e.getMessage());
            response.put("companyId", userDetails.getUser().getId());
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Update job active status (COMPANY only, must be owner)
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<Map<String, Object>> updateJobStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestParam boolean active) {
        logger.debug("Updating job status. JobId: {}, active: {}", id, active);
        try {
            Job updatedJob = jobService.updateJobActiveStatus(userDetails.getUser().getId(), id, active);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Job status updated successfully");
            response.put("data", updatedJob);
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

    // Get applications for a job (COMPANY only, must be owner)
    @GetMapping("/{jobId}/applications")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<Map<String, Object>> getJobApplications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long jobId) {
        try {
            List<Application> applications = jobService.getJobApplications(userDetails.getUser().getId(), jobId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Job applications retrieved successfully");
            response.put("data", applications);
            response.put("count", applications.size());
            response.put("jobId", jobId);
            response.put("companyId", userDetails.getUser().getId());
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to retrieve job applications: " + e.getMessage());
            response.put("jobId", jobId);
            response.put("companyId", userDetails.getUser().getId());
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Update application status (COMPANY only, must be owner)
    @PutMapping("/applications/{applicationId}")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<?> updateApplicationStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationStatusDTO statusDTO) {
        try {
            // Validate input parameters
            if (applicationId == null || applicationId <= 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Invalid application ID. Application ID must be a positive number");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            if (statusDTO == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Request body is required. Please provide application status data");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            if (statusDTO.getStatus() == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Application status is required. Valid values are: PENDING, ACCEPTED, REJECTED, REVIEWING");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            logger.info("Received status update request for application {}: {}", applicationId, statusDTO.getStatus());
            logger.info("Company ID from token: {}", userDetails.getUser().getId());
            
            Application application = jobService.updateApplicationStatus(
                userDetails.getUser().getId(), applicationId, statusDTO.getStatus());
            
            logger.info("Successfully updated application status");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Application status updated successfully");
            response.put("data", application);
            response.put("applicationId", applicationId);
            response.put("newStatus", statusDTO.getStatus());
            response.put("action", "status_update");
            response.put("timestamp", java.time.Instant.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error updating application status: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to update application status: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Invalid request format or data type mismatch");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}