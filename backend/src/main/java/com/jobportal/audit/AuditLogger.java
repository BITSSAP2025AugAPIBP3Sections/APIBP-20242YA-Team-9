package com.jobportal.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class AuditLogger {

    private static final Logger logger = LoggerFactory.getLogger("AUDIT_LOGGER");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public void logUserAction(String action, String userId, String userName, String entity, String entityId, Map<String, Object> details) {
        try {
            String timestamp = LocalDateTime.now().format(formatter);
            String detailsJson = details != null ? objectMapper.writeValueAsString(details) : "{}";
            
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("\n")
                     .append("═══════════════════════════════════════════════════════════════════════════════════════\n")
                     .append("🔍 AUDIT LOG - USER ACTION DETECTED\n")
                     .append("═══════════════════════════════════════════════════════════════════════════════════════\n")
                     .append("📅 TIMESTAMP    : ").append(timestamp).append("\n")
                     .append("👤 USER ID      : ").append(userId != null ? userId : "ANONYMOUS").append("\n")
                     .append("👥 USER NAME    : ").append(userName != null ? userName : "ANONYMOUS").append("\n")
                     .append("🎯 ACTION       : ").append(action).append("\n")
                     .append("📦 ENTITY       : ").append(entity != null ? entity : "N/A").append("\n")
                     .append("🔑 ENTITY ID    : ").append(entityId != null ? entityId : "N/A").append("\n")
                     .append("📋 DETAILS      : ").append(detailsJson).append("\n")
                     .append("═══════════════════════════════════════════════════════════════════════════════════════\n");

            logger.info(logMessage.toString());
            
        } catch (Exception e) {
            logger.error("Failed to log audit action: {}", e.getMessage());
        }
    }

    public void logAuthenticationAction(String action, String email, String ipAddress, boolean success, String reason) {
        String timestamp = LocalDateTime.now().format(formatter);
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n")
                 .append("🔐 AUTHENTICATION AUDIT\n")
                 .append("═══════════════════════════════════════════════════════════════════════════════════════\n")
                 .append("📅 TIMESTAMP    : ").append(timestamp).append("\n")
                 .append("📧 EMAIL        : ").append(email != null ? email : "N/A").append("\n")
                 .append("🌐 IP ADDRESS   : ").append(ipAddress != null ? ipAddress : "N/A").append("\n")
                 .append("🎯 ACTION       : ").append(action).append("\n")
                 .append(success ? "✅ STATUS       : SUCCESS\n" : "❌ STATUS       : FAILED\n")
                 .append("📝 REASON       : ").append(reason != null ? reason : "N/A").append("\n")
                 .append("═══════════════════════════════════════════════════════════════════════════════════════\n");

        logger.info(logMessage.toString());
    }

    public void logSecurityEvent(String event, String userId, String userName, String details, String severity) {
        String timestamp = LocalDateTime.now().format(formatter);
        String severityIcon = getSeverityIcon(severity);
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n")
                 .append("🛡️ SECURITY AUDIT EVENT\n")
                 .append("═══════════════════════════════════════════════════════════════════════════════════════\n")
                 .append("📅 TIMESTAMP    : ").append(timestamp).append("\n")
                 .append("👤 USER ID      : ").append(userId != null ? userId : "ANONYMOUS").append("\n")
                 .append("👥 USER NAME    : ").append(userName != null ? userName : "ANONYMOUS").append("\n")
                 .append("🚨 EVENT        : ").append(event).append("\n")
                 .append(severityIcon).append(" SEVERITY     : ").append(severity).append("\n")
                 .append("📝 DETAILS      : ").append(details != null ? details : "N/A").append("\n")
                 .append("═══════════════════════════════════════════════════════════════════════════════════════\n");

        if ("HIGH".equals(severity) || "CRITICAL".equals(severity)) {
            logger.warn(logMessage.toString());
        } else {
            logger.info(logMessage.toString());
        }
    }

    public void logDataChange(String operation, String entity, String entityId, String userId, String userName, 
                             Object oldValue, Object newValue, String field) {
        try {
            String timestamp = LocalDateTime.now().format(formatter);
            String oldValueStr = oldValue != null ? objectMapper.writeValueAsString(oldValue) : "null";
            String newValueStr = newValue != null ? objectMapper.writeValueAsString(newValue) : "null";
            
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("\n")
                     .append("📝 DATA CHANGE AUDIT\n")
                     .append("═══════════════════════════════════════════════════════════════════════════════════════\n")
                     .append("📅 TIMESTAMP    : ").append(timestamp).append("\n")
                     .append("👤 USER ID      : ").append(userId != null ? userId : "SYSTEM").append("\n")
                     .append("👥 USER NAME    : ").append(userName != null ? userName : "SYSTEM").append("\n")
                     .append("🔄 OPERATION    : ").append(operation).append("\n")
                     .append("📦 ENTITY       : ").append(entity).append("\n")
                     .append("🔑 ENTITY ID    : ").append(entityId).append("\n")
                     .append("🏷️ FIELD        : ").append(field != null ? field : "ALL_FIELDS").append("\n")
                     .append("⬅️ OLD VALUE    : ").append(oldValueStr).append("\n")
                     .append("➡️ NEW VALUE    : ").append(newValueStr).append("\n")
                     .append("═══════════════════════════════════════════════════════════════════════════════════════\n");

            logger.info(logMessage.toString());
            
        } catch (Exception e) {
            logger.error("Failed to log data change: {}", e.getMessage());
        }
    }

    public void logApiCall(String method, String endpoint, String userId, String userName, 
                          String ipAddress, int responseCode, long duration) {
        String timestamp = LocalDateTime.now().format(formatter);
        String statusIcon = responseCode < 400 ? "✅" : "❌";
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n")
                 .append("🌐 API CALL AUDIT\n")
                 .append("═══════════════════════════════════════════════════════════════════════════════════════\n")
                 .append("📅 TIMESTAMP    : ").append(timestamp).append("\n")
                 .append("👤 USER ID      : ").append(userId != null ? userId : "ANONYMOUS").append("\n")
                 .append("👥 USER NAME    : ").append(userName != null ? userName : "ANONYMOUS").append("\n")
                 .append("🌐 IP ADDRESS   : ").append(ipAddress != null ? ipAddress : "N/A").append("\n")
                 .append("🔗 METHOD       : ").append(method).append("\n")
                 .append("📍 ENDPOINT     : ").append(endpoint).append("\n")
                 .append(statusIcon).append(" RESPONSE     : ").append(responseCode).append("\n")
                 .append("⏱️ DURATION     : ").append(duration).append("ms\n")
                 .append("═══════════════════════════════════════════════════════════════════════════════════════\n");

        logger.info(logMessage.toString());
    }

    public void logBusinessEvent(String eventType, String description, String userId, String userName, Map<String, Object> context) {
        try {
            String timestamp = LocalDateTime.now().format(formatter);
            String contextJson = context != null ? objectMapper.writeValueAsString(context) : "{}";
            
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("\n")
                     .append("💼 BUSINESS EVENT AUDIT\n")
                     .append("═══════════════════════════════════════════════════════════════════════════════════════\n")
                     .append("📅 TIMESTAMP    : ").append(timestamp).append("\n")
                     .append("👤 USER ID      : ").append(userId != null ? userId : "SYSTEM").append("\n")
                     .append("👥 USER NAME    : ").append(userName != null ? userName : "SYSTEM").append("\n")
                     .append("📋 EVENT TYPE   : ").append(eventType).append("\n")
                     .append("📝 DESCRIPTION  : ").append(description).append("\n")
                     .append("🔍 CONTEXT      : ").append(contextJson).append("\n")
                     .append("═══════════════════════════════════════════════════════════════════════════════════════\n");

            logger.info(logMessage.toString());
            
        } catch (Exception e) {
            logger.error("Failed to log business event: {}", e.getMessage());
        }
    }

    private String getSeverityIcon(String severity) {
        return switch (severity != null ? severity.toUpperCase() : "INFO") {
            case "CRITICAL" -> "🔥";
            case "HIGH" -> "⚠️";
            case "MEDIUM" -> "⚡";
            case "LOW" -> "ℹ️";
            default -> "📢";
        };
    }
}
