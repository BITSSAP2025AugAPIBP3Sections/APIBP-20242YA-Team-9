package com.jobportal.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobportal.entity.Application;
import com.jobportal.entity.Job;
import com.jobportal.entity.User;
import com.jobportal.enums.ApplicationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByApplicantId(Long applicantId);
    Page<Application> findByApplicantId(Long applicantId, Pageable pageable);
    List<Application> findByJobId(Long jobId);
    Page<Application> findByJobId(Long jobId, Pageable pageable);
    List<Application> findByStatus(ApplicationStatus status);
    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);
    void deleteByJobId(Long jobId);
    void deleteByApplicantId(Long applicantId);
    long countByStatus(String status);
    Optional<Application> findByApplicantAndJob(User applicant, Job job);
    List<Application> findByApplicantOrderByAppliedAtDesc(User applicant);
    Page<Application> findByApplicantOrderByAppliedAtDesc(User applicant, Pageable pageable);
    long countByJobId(Long jobId);
}
