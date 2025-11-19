package com.jobportal.repository.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jobportal.entity.audit.AuditLog;
import com.jobportal.entity.audit.AuditOperation;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityNameAndEntityId(String entityName, String entityId);

    List<AuditLog> findByUserEmail(String userEmail);

    List<AuditLog> findByOperation(AuditOperation operation);

    @Query("SELECT al FROM AuditLog al WHERE al.timestamp BETWEEN :startDate AND :endDate")
    List<AuditLog> findByTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);

    @Query("SELECT al FROM AuditLog al WHERE al.entityName = :entityName AND al.timestamp BETWEEN :startDate AND :endDate")
    List<AuditLog> findByEntityNameAndTimestampBetween(@Param("entityName") String entityName,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    Page<AuditLog> findByEntityNameOrderByTimestampDesc(String entityName, Pageable pageable);

    Page<AuditLog> findByUserEmailOrderByTimestampDesc(String userEmail, Pageable pageable);

    @Query("SELECT DISTINCT al.entityName FROM AuditLog al")
    List<String> findDistinctEntityNames();

    @Query("SELECT al FROM AuditLog al ORDER BY al.timestamp DESC")
    Page<AuditLog> findAllOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.operation = :operation AND al.timestamp >= :since")
    Long countByOperationSince(@Param("operation") AuditOperation operation, @Param("since") LocalDateTime since);

    @Query("SELECT al FROM AuditLog al WHERE al.userEmail = :userEmail AND al.entityName = :entityName ORDER BY al.timestamp DESC")
    Page<AuditLog> findByUserEmailAndEntityNameOrderByTimestampDesc(String userEmail, String entityName, Pageable pageable);

    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.success = true")
    Long countBySuccessStatusTrue();

    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.success = false")
    Long countBySuccessStatusFalse();

    @Query("SELECT al FROM AuditLog al WHERE LOWER(al.entityName) != LOWER('AuditController') ORDER BY al.timestamp DESC")
    Page<AuditLog> findAllExceptAuditControllerIgnoreCaseOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.timestamp BETWEEN :startDate AND :endDate ORDER BY al.timestamp DESC")
    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime startDate,
                                                             LocalDateTime endDate,
                                                              Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.userEmail = :userEmail AND al.entityName = :entityName AND al.timestamp BETWEEN :startDate AND :endDate ORDER BY al.timestamp DESC")
    Page<AuditLog> findByUserEmailAndEntityNameAndTimestampBetweenOrderByTimestampDesc(
            @Param("userEmail") String userEmail,
            @Param("entityName") String entityName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT al FROM AuditLog al WHERE al.entityName = :entityName AND al.timestamp BETWEEN :startDate AND :endDate ORDER BY al.timestamp DESC")
    Page<AuditLog> findByEntityNameAndTimestampBetweenOrderByTimestampDesc(
            @Param("entityName") String entityName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );


    // Fetch logs by userEmail and date range with pagination, ordered by timestamp desc
    @Query("SELECT al FROM AuditLog al WHERE al.userEmail = :userEmail AND al.timestamp BETWEEN :startDate AND :endDate ORDER BY al.timestamp DESC")
    Page<AuditLog> findByUserEmailAndTimestampBetweenOrderByTimestampDesc(
            @Param("userEmail") String userEmail,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT al FROM AuditLog al WHERE al.success = :success ORDER BY al.timestamp DESC")
    Page<AuditLog> findBySuccessOrderByTimestampDesc(@Param("success") Boolean success, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.userEmail = :userEmail AND al.entityName = :entityName AND al.timestamp BETWEEN :startDate AND :endDate AND al.success = :success ORDER BY al.timestamp DESC")
    Page<AuditLog> findByUserEmailAndEntityNameAndTimestampBetweenAndSuccessOrderByTimestampDesc(
            @Param("userEmail") String userEmail,
            @Param("entityName") String entityName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("success") Boolean success,
            Pageable pageable
    );

    @Query("SELECT al FROM AuditLog al WHERE al.entityName = :entityName AND al.timestamp BETWEEN :startDate AND :endDate AND al.success = :success ORDER BY al.timestamp DESC")
    Page<AuditLog> findByEntityNameAndTimestampBetweenAndSuccessOrderByTimestampDesc(
            @Param("entityName") String entityName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("success") Boolean success,
            Pageable pageable
    );

    @Query("SELECT al FROM AuditLog al WHERE al.userEmail = :userEmail AND al.timestamp BETWEEN :startDate AND :endDate AND al.success = :success ORDER BY al.timestamp DESC")
    Page<AuditLog> findByUserEmailAndTimestampBetweenAndSuccessOrderByTimestampDesc(
            @Param("userEmail") String userEmail,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("success") Boolean success,
            Pageable pageable
    );

    @Query("SELECT al FROM AuditLog al WHERE al.timestamp BETWEEN :startDate AND :endDate AND al.success = :success ORDER BY al.timestamp DESC")
    Page<AuditLog> findByTimestampBetweenAndSuccessOrderByTimestampDesc(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("success") Boolean success,
            Pageable pageable
    );

    @Query("SELECT al FROM AuditLog al WHERE al.userEmail = :userEmail AND al.entityName = :entityName AND al.success = :success ORDER BY al.timestamp DESC")
    Page<AuditLog> findByUserEmailAndEntityNameAndSuccessOrderByTimestampDesc(
            @Param("userEmail") String userEmail,
            @Param("entityName") String entityName,
            @Param("success") Boolean success,
            Pageable pageable
    );

    // 1. userEmail + success
    @Query("SELECT al FROM AuditLog al WHERE al.userEmail = :userEmail AND al.success = :success ORDER BY al.timestamp DESC")
    Page<AuditLog> findByUserEmailAndSuccessOrderByTimestampDesc(
            @Param("userEmail") String userEmail,
            @Param("success") Boolean success,
            Pageable pageable
    );

    // 2. entityName + success
    @Query("SELECT al FROM AuditLog al WHERE al.entityName = :entityName AND al.success = :success ORDER BY al.timestamp DESC")
    Page<AuditLog> findByEntityNameAndSuccessOrderByTimestampDesc(
            @Param("entityName") String entityName,
            @Param("success") Boolean success,
            Pageable pageable
    );


}
