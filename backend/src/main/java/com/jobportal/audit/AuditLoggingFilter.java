package com.jobportal.audit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.jobportal.security.CustomUserDetails;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuditLoggingFilter implements Filter {

    @Autowired
    private AuditLogger auditLogger;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest && 
            response instanceof HttpServletResponse httpResponse) {
            
            long startTime = System.currentTimeMillis();
            String uri = httpRequest.getRequestURI();
            String queryString = httpRequest.getQueryString();
            String fullUrl = uri + (queryString != null ? "?" + queryString : "");
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            // Skip logging for static resources and health checks
            if (shouldSkipLogging(uri)) {
                chain.doFilter(request, response);
                return;
            }

            try {
                // Process the request
                chain.doFilter(request, response);
                
                long duration = System.currentTimeMillis() - startTime;
                int statusCode = httpResponse.getStatus();
                
                // Get user information after processing (in case authentication happens during the request)
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String userId = null;
                String userName = null;
                
                if (authentication != null && authentication.isAuthenticated() && 
                    !"anonymousUser".equals(authentication.getPrincipal())) {
                    if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                        userId = userDetails.getUser().getId().toString();
                        userName = userDetails.getUser().getName();
                    }
                }
                
                // Log the HTTP request
                Map<String, Object> requestDetails = new HashMap<>();
                requestDetails.put("userAgent", userAgent);
                requestDetails.put("queryString", queryString);
                requestDetails.put("contentType", httpRequest.getContentType());
                requestDetails.put("responseCode", statusCode);
                requestDetails.put("duration", duration + "ms");
                
                auditLogger.logUserAction(
                    "HTTP_REQUEST",
                    userId,
                    userName,
                    "HTTP_ENDPOINT",
                    fullUrl,
                    requestDetails
                );
                
                // Log suspicious activities
                if (statusCode == 401) {
                    auditLogger.logSecurityEvent(
                        "UNAUTHORIZED_ACCESS_ATTEMPT",
                        userId,
                        userName,
                        "Unauthorized access attempt to " + fullUrl + " from IP: " + ipAddress,
                        "MEDIUM"
                    );
                } else if (statusCode == 403) {
                    auditLogger.logSecurityEvent(
                        "FORBIDDEN_ACCESS_ATTEMPT",
                        userId,
                        userName,
                        "Forbidden access attempt to " + fullUrl + " from IP: " + ipAddress,
                        "HIGH"
                    );
                } else if (statusCode >= 500) {
                    auditLogger.logSecurityEvent(
                        "SERVER_ERROR",
                        userId,
                        userName,
                        "Server error (" + statusCode + ") on " + fullUrl + " from IP: " + ipAddress,
                        "HIGH"
                    );
                }
                
            } catch (Exception e) {
                auditLogger.logSecurityEvent(
                    "REQUEST_PROCESSING_ERROR",
                    null,
                    null,
                    "Error processing request " + fullUrl + " from IP: " + ipAddress + " - " + e.getMessage(),
                    "HIGH"
                );
                
                throw e;
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean shouldSkipLogging(String uri) {
        return uri.startsWith("/css/") || 
               uri.startsWith("/js/") || 
               uri.startsWith("/images/") || 
               uri.startsWith("/favicon.ico") ||
               uri.startsWith("/actuator/health") ||
               uri.startsWith("/swagger-ui/") ||
               uri.startsWith("/v3/api-docs");
    }

    private String getClientIpAddress(HttpServletRequest request) {
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
}
