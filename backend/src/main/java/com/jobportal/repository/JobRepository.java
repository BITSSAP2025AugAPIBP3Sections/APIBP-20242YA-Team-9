package com.jobportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;

import com.jobportal.entity.Job;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByIsActiveTrue();
    Page<Job> findByIsActiveTrue(Pageable pageable);
    List<Job> findByCompanyId(Long companyId);
    Page<Job> findByCompanyId(Long companyId, Pageable pageable);
    long countByIsActiveTrue();
    List<Job> findByIsActive(Boolean isActive);
    Page<Job> findByIsActive(Boolean isActive, Pageable pageable);

    @Modifying
    @Query("UPDATE Job j SET j.isActive = :active WHERE j.id = :jobId")
    void updateJobActiveStatus(Long jobId, boolean active);

    // Search by title (case-insensitive partial match)
    List<Job> findByTitleContainingIgnoreCaseAndIsActiveTrue(String title);

    // Search by location (case-insensitive partial match)
    List<Job> findByLocationContainingIgnoreCaseAndIsActiveTrue(String location);

    // Search by salary range (case-insensitive partial match)
    // Use exact matching for salary range since it might contain special characters
    List<Job> findBySalaryRangeAndIsActiveTrue(String salaryRange);

    // Custom query for advanced search with dynamic parameters
    @Query("SELECT j FROM Job j WHERE j.isActive = true " +
            "AND (:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
            "AND (:salaryRange IS NULL OR j.salaryRange = :salaryRange) " +
            "AND (:companyName IS NULL OR LOWER(j.companyName) LIKE LOWER(CONCAT('%', :companyName, '%')))")
    List<Job> searchJobs(String title, String location, String salaryRange, String companyName);

    @Query("""
    SELECT j FROM Job j
    WHERE (:title IS NULL OR j.title LIKE %:title%)
      AND (:location IS NULL OR j.location LIKE %:location%)
      AND (:salaryRange IS NULL OR j.salaryRange = :salaryRange)
      AND (:companyName IS NULL OR j.company.name LIKE %:companyName%)
      AND j.isActive = true
""")
Page<Job> searchJobsPaginated(
        @Param("title") String title,
        @Param("location") String location,
        @Param("salaryRange") String salaryRange,
        @Param("companyName") String companyName,
        Pageable pageable
);

}
