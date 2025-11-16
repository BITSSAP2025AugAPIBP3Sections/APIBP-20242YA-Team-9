package com.jobportal.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class SecurityAuditEventListener {

    @Autowired
    private AuditLogger auditLogger;

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String ipAddress = getClientIpAddress();
        
        auditLogger.logAuthenticationAction(
            "AUTHENTICATION_SUCCESS",
            username,
            ipAddress,
            true,
            "User successfully authenticated"
        );
    }

    @EventListener
    public void handleAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        String ipAddress = getClientIpAddress();
        
        auditLogger.logAuthenticationAction(
            "AUTHENTICATION_FAILURE",
            username,
            ipAddress,
            false,
            "Bad credentials provided"
        );
        
        auditLogger.logSecurityEvent(
            "SUSPICIOUS_LOGIN_ATTEMPT",
            null,
            username,
            "Failed login attempt with bad credentials from IP: " + ipAddress,
            "MEDIUM"
        );
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Handle gracefully
        }
        return "Unknown";
    }
}
