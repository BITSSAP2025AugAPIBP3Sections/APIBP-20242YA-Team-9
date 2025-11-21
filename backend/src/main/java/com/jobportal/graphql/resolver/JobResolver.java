package com.jobportal.graphql.resolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import com.jobportal.entity.Job;
import com.jobportal.security.CustomUserDetails;
import com.jobportal.service.JobService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class JobResolver {

    @Autowired
    private JobService jobService;

    @QueryMapping
    public List<Map<String, Object>> getAllJobs(
            @Argument String location,
            @Argument String title,
            @Argument String salaryRange,
            @Argument String companyName) {
        List<Job> jobs = jobService.searchJobs(location, title, salaryRange,companyName);
        return jobs.stream()
            .sorted((j1, j2) -> {
                if (j1.getPostedAt() == null && j2.getPostedAt() == null) return 0;
                if (j1.getPostedAt() == null) return 1;
                if (j2.getPostedAt() == null) return -1;
                return j2.getPostedAt().compareTo(j1.getPostedAt());
            })
            .map(this::mapJobToGraphQL)
            .toList();
    }

    @QueryMapping
    public Map<String, Object> getJobById(@Argument Long id) {
        Optional<Job> jobOpt = jobService.getJobById(id);
        return jobOpt.map(this::mapJobToGraphQL).orElse(null);
    }

    @QueryMapping
    public List<Map<String, Object>> getCompanyJobs(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.getUser().getRole().toString().equals("COMPANY")) {
            throw new RuntimeException("Access denied. Company role required.");
        }
        List<Job> jobs = jobService.getJobsByCompany(userDetails.getUser().getId());
        return jobs.stream().map(this::mapJobToGraphQL).toList();
    }

    @MutationMapping
    public Map<String, Object> createJob(
            @Argument Map<String, Object> input,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.getUser().getRole().toString().equals("COMPANY")) {
            throw new RuntimeException("Access denied. Company role required.");
        }

        Job job = new Job();
        job.setTitle((String) input.get("title"));
        job.setDescription((String) input.get("description"));
        job.setLocation((String) input.get("location"));
        job.setSalaryRange((String) input.get("salaryRange"));
        job.setRequirements((List<String>) input.get("requirements"));
        job.setResponsibilities((List<String>) input.get("responsibilities"));

        Job createdJob = jobService.createJob(userDetails.getUser(), job);
        return mapJobToGraphQL(createdJob);
    }

    @MutationMapping
    public Map<String, Object> updateJob(
            @Argument Long id,
            @Argument Map<String, Object> input,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.getUser().getRole().toString().equals("COMPANY")) {
            throw new RuntimeException("Access denied. Company role required.");
        }

        Job job = new Job();
        if (input.containsKey("title")) job.setTitle((String) input.get("title"));
        if (input.containsKey("description")) job.setDescription((String) input.get("description"));
        if (input.containsKey("location")) job.setLocation((String) input.get("location"));
        if (input.containsKey("salaryRange")) job.setSalaryRange((String) input.get("salaryRange"));
        if (input.containsKey("requirements")) job.setRequirements((List<String>) input.get("requirements"));
        if (input.containsKey("responsibilities")) job.setResponsibilities((List<String>) input.get("responsibilities"));

        Job updatedJob = jobService.updateJob(userDetails.getUser().getId(), id, job);
        return mapJobToGraphQL(updatedJob);
    }

    @MutationMapping
    public Map<String, Object> deleteJob(
            @Argument Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.getUser().getRole().toString().equals("COMPANY")) {
            throw new RuntimeException("Access denied. Company role required.");
        }

        jobService.deleteJob(userDetails.getUser().getId(), id);
        return Map.of("message", "Job deleted successfully");
    }

    @MutationMapping
    public Map<String, Object> updateJobStatus(
            @Argument Long id,
            @Argument Boolean active,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.getUser().getRole().toString().equals("COMPANY")) {
            throw new RuntimeException("Access denied. Company role required.");
        }

        Job updatedJob = jobService.updateJobActiveStatus(userDetails.getUser().getId(), id, active);
        return mapJobToGraphQL(updatedJob);
    }

    private Map<String, Object> mapJobToGraphQL(Job job) {
        String formattedDate = null;
        if (job.getPostedAt() != null) {
            formattedDate = job.getPostedAt().toString().replace("T", "T").concat("Z");
        }

        Map<String, Object> jobMap = new HashMap<>();
        jobMap.put("id", String.valueOf(job.getId()));
        jobMap.put("title", job.getTitle());
        jobMap.put("description", job.getDescription());
        jobMap.put("company", Map.of(
            "id", job.getCompany().getId(),
            "name", job.getCompany().getName(),
            "email", job.getCompany().getEmail(),
            "bio", job.getCompany().getBio() != null ? job.getCompany().getBio() : "No Description"
        ));
        jobMap.put("location", job.getLocation());
        jobMap.put("salaryRange", job.getSalaryRange());
        jobMap.put("postedAt", formattedDate);
        jobMap.put("requirements", job.getRequirements());
        jobMap.put("responsibilities", job.getResponsibilities());
        jobMap.put("applicationsCount", jobService.getApplicationsCountForJob(job.getId()));
        jobMap.put("active", job.isActive());

        return jobMap;
    }
}
