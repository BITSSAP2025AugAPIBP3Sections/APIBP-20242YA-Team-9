package com.jobportal.graphql.resolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import com.jobportal.entity.Application;
import com.jobportal.entity.Job;
import com.jobportal.entity.User;
import com.jobportal.security.CustomUserDetails;
import com.jobportal.service.AdminService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class AdminResolver {

    @Autowired
    private AdminService adminService;

    @QueryMapping
    public List<Map<String, Object>> getAllUsers(
            @Argument String role,
            @Argument Boolean includeInactive,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        checkAdminAccess(userDetails);

        List<User> users = adminService.getAllUsers(role, includeInactive != null ? includeInactive : false);
        return users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("email", user.getEmail());
            map.put("role", user.getRole().toString());
            map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
            map.put("active", user.isActive());
            map.put("bio", user.getBio());
            return map;
        }).toList();
    }

    @QueryMapping
    public Map<String, Object> getUserById(
            @Argument Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        checkAdminAccess(userDetails);

        Optional<User> userOpt = adminService.getUserById(id);
        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("role", user.getRole().toString());
        map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        map.put("active", user.isActive());
        map.put("bio", user.getBio());
        return map;
    }

    @QueryMapping
    public List<Map<String, Object>> getAdminJobs(
            @Argument Boolean active,
            @Argument Boolean includeExpired,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        checkAdminAccess(userDetails);

        List<Job> jobs = adminService.getAllJobs(active, includeExpired != null ? includeExpired : false);
        return jobs.stream().map(job -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", job.getId());
            map.put("title", job.getTitle());
            map.put("description", job.getDescription());
            map.put("company", Map.of(
                "id", job.getCompany().getId(),
                "name", job.getCompany().getName(),
                "email", job.getCompany().getEmail(),
                "bio", job.getCompany().getBio() != null ? job.getCompany().getBio() : "No Description"
            ));
            map.put("location", job.getLocation());
            map.put("salaryRange", job.getSalaryRange());
            map.put("postedAt", job.getPostedAt() != null ? job.getPostedAt().toString() : null);
            map.put("requirements", job.getRequirements());
            map.put("responsibilities", job.getResponsibilities());
            map.put("active", job.isActive());
            return map;
        }).toList();
    }

    @QueryMapping
    public List<Map<String, Object>> getAllApplications(
            @Argument String status,
            @Argument Boolean includeArchived,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        checkAdminAccess(userDetails);

        List<Application> applications = adminService.getAllApplications(status, includeArchived != null ? includeArchived : false);
        return applications.stream().map(app -> Map.of(
            "id", String.valueOf(app.getId()),
            "job", Map.of(
                "id", String.valueOf(app.getJob().getId()),
                "title", app.getJob().getTitle(),
                "company", Map.of(
                    "id", app.getJob().getCompany().getId(),
                    "name", app.getJob().getCompany().getName()
                )
            ),
            "applicant", Map.of(
                "id", app.getApplicant().getId(),
                "name", app.getApplicant().getName(),
                "email", app.getApplicant().getEmail()
            ),
            "status", app.getStatus().toString(),
            "appliedAt", app.getAppliedAt().toString(),
            "resumeUrl", app.getResumeUrl()
        )).toList();
    }

    @MutationMapping
    public Map<String, Object> deleteUser(
            @Argument Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        checkAdminAccess(userDetails);

        adminService.deleteUser(id);
        return Map.of("message", "User deleted successfully");
    }

    @MutationMapping
    public Map<String, Object> updateUserStatus(
            @Argument Long id,
            @Argument Map<String, Object> input,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        checkAdminAccess(userDetails);

        Boolean active = (Boolean) input.get("active");
        User user = adminService.updateUserStatus(id, active);

        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("role", user.getRole().toString());
        map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        map.put("active", user.isActive());
        map.put("bio", user.getBio());
        return map;
    }

    @MutationMapping
    public Map<String, Object> deleteAdminJob(
            @Argument Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        checkAdminAccess(userDetails);

        adminService.deleteJob(id);
        return Map.of("message", "Job deleted successfully");
    }

    @MutationMapping
    public Map<String, Object> deleteApplication(
            @Argument Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        checkAdminAccess(userDetails);

        adminService.deleteApplication(id);
        return Map.of("message", "Application deleted successfully");
    }

    @MutationMapping
    public List<Map<String, Object>> bulkUpdateUserStatus(
            @Argument List<Map<String, Object>> userStatusMap,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        checkAdminAccess(userDetails);

        Map<Long, Boolean> statusMap = new HashMap<>();
        for (Map<String, Object> entry : userStatusMap) {
            Long userId = Long.valueOf(entry.get("userId").toString());
            Boolean active = (Boolean) entry.get("active");
            statusMap.put(userId, active);
        }

        List<User> updatedUsers = adminService.bulkUpdateUserStatus(statusMap);
        return updatedUsers.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getName());
            map.put("email", user.getEmail());
            map.put("role", user.getRole().toString());
            map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
            map.put("active", user.isActive());
            map.put("bio", user.getBio());
            return map;
        }).toList();
    }

    @MutationMapping
    public List<Map<String, Object>> bulkUpdateJobStatus(
            @Argument List<Map<String, Object>> jobStatusMap,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        checkAdminAccess(userDetails);

        Map<Long, Boolean> statusMap = new HashMap<>();
        for (Map<String, Object> entry : jobStatusMap) {
            Long jobId = Long.valueOf(entry.get("jobId").toString());
            Boolean active = (Boolean) entry.get("active");
            statusMap.put(jobId, active);
        }

        List<Job> updatedJobs = adminService.bulkUpdateJobStatus(statusMap);
        return updatedJobs.stream().map(job -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", job.getId());
            map.put("title", job.getTitle());
            map.put("description", job.getDescription());
            map.put("company", Map.of(
                "id", job.getCompany().getId(),
                "name", job.getCompany().getName()
            ));
            map.put("location", job.getLocation());
            map.put("salaryRange", job.getSalaryRange());
            map.put("active", job.isActive());
            return map;
        }).toList();
    }

    private void checkAdminAccess(CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.getUser().getRole().toString().equals("ADMIN")) {
            throw new RuntimeException("Access denied. Admin role required.");
        }
    }
}
