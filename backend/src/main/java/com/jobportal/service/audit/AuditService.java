package com.jobportal.service.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobportal.entity.audit.AuditLog;
import com.jobportal.entity.audit.AuditOperation;
import com.jobportal.repository.audit.AuditLogRepository;
import com.jobportal.security.CustomUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public AuditLog logOperation(String entityName, String entityId, AuditOperation operation) {
        return logOperation(entityName, entityId, operation, null, null, null);
    }

    public AuditLog logOperation(String entityName, String entityId, AuditOperation operation, 
                                Object oldValues, Object newValues, List<String> changedFields) {
        AuditLog auditLog = new AuditLog(entityName, entityId, operation);
        
        // Set user information
        setUserInformation(auditLog);
        
        // Set old and new values
        if (oldValues != null) {
            try {
                auditLog.setOldValues(objectMapper.writeValueAsString(oldValues));
            } catch (JsonProcessingException e) {
                auditLog.setOldValues("Error serializing old values: " + e.getMessage());
            }
        }
        
        if (newValues != null) {
            try {
                auditLog.setNewValues(objectMapper.writeValueAsString(newValues));
            } catch (JsonProcessingException e) {
                auditLog.setNewValues("Error serializing new values: " + e.getMessage());
            }
        }
        
        if (changedFields != null && !changedFields.isEmpty()) {
            auditLog.setChangedFields(String.join(",", changedFields));
        }
        
        return auditLogRepository.save(auditLog);
    }

    public AuditLog logOperationWithRequest(String entityName, String entityId, AuditOperation operation,
                                           HttpServletRequest request) {
        AuditLog auditLog = logOperation(entityName, entityId, operation);
        
        if (request != null) {
            auditLog.setIpAddress(getClientIpAddress(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
            auditLog.setSessionId(request.getSession().getId());
        }
        
        return auditLogRepository.save(auditLog);
    }

    public AuditLog logFailedOperation(String entityName, String entityId, AuditOperation operation, 
                                      String errorMessage) {
        AuditLog auditLog = new AuditLog(entityName, entityId, operation);
        setUserInformation(auditLog);
        auditLog.setSuccess(false);
        auditLog.setErrorMessage(errorMessage);
        
        return auditLogRepository.save(auditLog);
    }

    private void setUserInformation(AuditLog auditLog) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) principal;
                auditLog.setUserEmail(userDetails.getUser().getEmail());
                auditLog.setUserId(userDetails.getUser().getId());
                auditLog.setUserRole(userDetails.getUser().getRole().toString());
            } else if (principal instanceof String) {
                auditLog.setUserEmail((String) principal);
            }
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            // X-Forwarded-For can contain multiple IPs, we want the first one
            return xForwardedForHeader.split(",")[0].trim();
        }
    }

    // Query methods
    public List<AuditLog> getAuditLogsForEntity(String entityName, String entityId) {
        return auditLogRepository.findByEntityNameAndEntityId(entityName, entityId);
    }

    public List<AuditLog> getAuditLogsForUser(String userEmail) {
        return auditLogRepository.findByUserEmail(userEmail);
    }

    public List<AuditLog> getAuditLogsByOperation(AuditOperation operation) {
        return auditLogRepository.findByOperation(operation);
    }

    public List<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByTimestampBetween(startDate, endDate);
    }

    public Page<AuditLog> getAuditLogsByEntity(String entityName, Pageable pageable) {
        return auditLogRepository.findByEntityNameOrderByTimestampDesc(entityName, pageable);
    }

    public Page<AuditLog> getAuditLogsByUser(String userEmail, Pageable pageable) {
        return auditLogRepository.findByUserEmailOrderByTimestampDesc(userEmail, pageable);
    }

    public List<String> getAllEntityNames() {
        return auditLogRepository.findDistinctEntityNames();
    }

    public Long getOperationCountSince(AuditOperation operation, LocalDateTime since) {
        return auditLogRepository.countByOperationSince(operation, since);
    }
}
