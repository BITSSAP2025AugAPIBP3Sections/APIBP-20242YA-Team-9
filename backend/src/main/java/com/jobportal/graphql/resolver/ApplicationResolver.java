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
import com.jobportal.enums.ApplicationStatus;
import com.jobportal.security.CustomUserDetails;
import com.jobportal.service.ApplicantService;
import com.jobportal.service.JobService;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Controller
public class ApplicationResolver {

    @Autowired
    private ApplicantService applicantService;

    @Autowired
    private JobService jobService;

    @QueryMapping
    public List<Map<String, Object>> getMyApplications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.getUser().getRole().toString().equals("APPLICANT")) {
            throw new RuntimeException("Access denied. Applicant role required.");
        }

        List<Application> applications = applicantService.getApplicationsByApplicant(userDetails.getUser().getId());
        return applications.stream().map(app -> Map.of(
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
    }

    @QueryMapping
    public Map<String, Object> getApplicationStatus(
            @Argument Long applicationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.getUser().getRole().toString().equals("APPLICANT")) {
            throw new RuntimeException("Access denied. Applicant role required.");
        }

        Application application = applicantService.getApplicationStatus(userDetails.getUser().getId(), applicationId);
        Job job = application.getJob();
        User company = job.getCompany();
        User applicant = application.getApplicant();

        return Map.of(
            "title", job.getTitle(),
            "description", job.getDescription(),
            "company", Map.of(
                "id", company.getId(),
                "name", company.getName(),
                "email", company.getEmail(),
                "bio", company.getBio() != null ? company.getBio() : "No Description"
            ),
            "requirements", job.getRequirements(),
            "responsibilities", job.getResponsibilities(),
            "applicant", Map.of(
                "id", applicant.getId(),
                "name", applicant.getName(),
                "email", applicant.getEmail(),
                "role", applicant.getRole().toString(),
                "createdAt", applicant.getCreatedAt().toString(),
                "active", applicant.isActive(),
                "bio", applicant.getBio()
            ),
            "status", application.getStatus().toString(),
            "appliedAt", application.getAppliedAt().atZone(ZoneOffset.UTC).toInstant().toString()
        );
    }

    @QueryMapping
    public List<Map<String, Object>> getJobApplications(
            @Argument Long jobId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.getUser().getRole().toString().equals("COMPANY")) {
            throw new RuntimeException("Access denied. Company role required.");
        }

        List<Application> applications = jobService.getJobApplications(userDetails.getUser().getId(), jobId);
        return applications.stream().map(app -> Map.of(
            "id", String.valueOf(app.getId()),
            "job", Map.of(
                "id", String.valueOf(app.getJob().getId()),
                "title", app.getJob().getTitle()
            ),
            "applicant", Map.of(
                "id", app.getApplicant().getId(),
                "name", app.getApplicant().getName(),
                "email", app.getApplicant().getEmail()
            ),
            "status", app.getStatus().toString(),
            "appliedAt", app.getAppliedAt().atZone(ZoneOffset.UTC).toInstant().toString(),
            "resumeUrl", app.getResumeUrl()
        )).toList();
    }

    @MutationMapping
    public Map<String, Object> applyToJob(
            @Argument Long jobId,
            @Argument String resumeUrl,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.getUser().getRole().toString().equals("APPLICANT")) {
            throw new RuntimeException("Access denied. Applicant role required.");
        }

        Application application = applicantService.applyToJob(userDetails.getUser().getId(), jobId, resumeUrl);
        return Map.of("message", "Applied successfully to job with application ID: " + application.getId());
    }

    @MutationMapping
    public Map<String, Object> withdrawApplication(
            @Argument Long applicationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.getUser().getRole().toString().equals("APPLICANT")) {
            throw new RuntimeException("Access denied. Applicant role required.");
        }

        applicantService.withdrawApplication(userDetails.getUser().getId(), applicationId);
        return Map.of("message", "Application withdrawn successfully");
    }

    @MutationMapping
    public Map<String, Object> updateApplicationStatus(
            @Argument Long applicationId,
            @Argument Map<String, Object> input,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.getUser().getRole().toString().equals("COMPANY")) {
            throw new RuntimeException("Access denied. Company role required.");
        }

        String statusString = (String) input.get("status");
        // Convert String to ApplicationStatus enum
        ApplicationStatus status = ApplicationStatus.valueOf(statusString);
        Application application = jobService.updateApplicationStatus(
            userDetails.getUser().getId(), applicationId, status);

        return Map.of(
            "id", String.valueOf(application.getId()),
            "job", Map.of(
                "id", String.valueOf(application.getJob().getId()),
                "title", application.getJob().getTitle()
            ),
            "applicant", Map.of(
                "id", application.getApplicant().getId(),
                "name", application.getApplicant().getName(),
                "email", application.getApplicant().getEmail()
            ),
            "status", application.getStatus().toString(),
            "appliedAt", application.getAppliedAt().atZone(ZoneOffset.UTC).toInstant().toString(),
            "resumeUrl", application.getResumeUrl()
        );
    }
}
