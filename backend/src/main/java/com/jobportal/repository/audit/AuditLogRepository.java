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

    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.operation = :operation AND al.timestamp >= :since")
    Long countByOperationSince(@Param("operation") AuditOperation operation, @Param("since") LocalDateTime since);
}
