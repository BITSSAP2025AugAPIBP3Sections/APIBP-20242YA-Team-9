package com.jobportal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, 
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        // Create detailed log message
        String logMessage = String.format("403 FORBIDDEN - Access denied for %s %s | User-Agent: %s | Remote-Addr: %s | Error: %s", 
                   request.getMethod(), 
                   request.getRequestURI(), 
                   request.getHeader("User-Agent"), 
                   request.getRemoteAddr(),
                   accessDeniedException.getMessage());
        
        logger.warn(logMessage);
        
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("code", 403);
        errorResponse.put("path", request.getRequestURI());
        errorResponse.put("method", request.getMethod());
        errorResponse.put("timestamp", java.time.Instant.now().toString());
        
        // Include the actual exception message and log details
        errorResponse.put("message", "Access Forbidden: " + accessDeniedException.getMessage());
        errorResponse.put("logMessage", logMessage);
        errorResponse.put("userAgent", request.getHeader("User-Agent"));
        errorResponse.put("remoteAddress", request.getRemoteAddr());
        
        // Get generic guidance
        errorResponse.put("suggestion", getGenericSuggestion(request.getRequestURI(), request.getMethod()));
        
        // Log the structured response being sent
        String responseJson = objectMapper.writeValueAsString(errorResponse);
        logger.info("Sending 403 response: {}", responseJson);
        
        response.getWriter().write(responseJson);
    }
    
    private String getGenericSuggestion(String requestPath, String method) {
        // Generic suggestions based on endpoint patterns
        String requiredRole = "unknown";
        String actionType = method.toLowerCase();
        
        if (requestPath.startsWith("/api/admin")) {
            requiredRole = "ADMIN";
        } else if (requestPath.startsWith("/api/applicant")) {
            requiredRole = "APPLICANT";
        } else if (requestPath.startsWith("/api/jobs") && (method.equals("POST") || method.equals("PUT") || method.equals("DELETE") || requestPath.contains("company"))) {
            requiredRole = "COMPANY";
        } else if (requestPath.startsWith("/api/profiles") && method.equals("PUT")) {
            return "Ensure you're updating your own profile or have administrative privileges. Check authentication token and user permissions.";
        }
        
        return String.format("This %s operation on '%s' requires %s role privileges. " +
                           "Please verify: 1) You're authenticated, 2) Your token is valid, 3) You have the correct role (%s). " +
                           "If you believe this is an error, contact support.", 
                           actionType, requestPath, requiredRole, requiredRole);
    }
}
