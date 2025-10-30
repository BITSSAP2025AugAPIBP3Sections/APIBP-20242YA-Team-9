package com.example.Job_Portal.repository;

import com.example.Job_Portal.entity.Application;
import com.example.Job_Portal.entity.User;
import com.example.Job_Portal.entity.Job;
import com.example.Job_Portal.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByApplicantId(Long applicantId);
    List<Application> findByJobId(Long jobId);
    List<Application> findByStatus(ApplicationStatus status);
    void deleteByJobId(Long jobId);
    void deleteByApplicantId(Long applicantId);
    long countByStatus(String status);
    Optional<Application> findByApplicantAndJob(User applicant, Job job);
    List<Application> findByApplicantOrderByAppliedAtDesc(User applicant);
    long countByJobId(Long jobId);
}
