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
        
        logger.warn("Access denied for request: {} - {} | Error: {}", 
                   request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());
        
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("code", 403);
        errorResponse.put("path", request.getRequestURI());
        errorResponse.put("method", request.getMethod());
        errorResponse.put("timestamp", java.time.Instant.now().toString());
        
        // Get specific message based on the request path and method
        String message = getSpecificErrorMessage(request.getRequestURI(), request.getMethod());
        errorResponse.put("message", message);
        errorResponse.put("suggestion", getSuggestion(request.getRequestURI()));
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
    
    private String getSpecificErrorMessage(String requestPath, String method) {
        // Admin endpoints
        if (requestPath.startsWith("/api/admin")) {
            if (requestPath.contains("/users")) {
                return "Access denied. Only administrators can manage user accounts.";
            } else if (requestPath.contains("/jobs")) {
                return "Access denied. Only administrators can manage job listings globally.";
            } else if (requestPath.contains("/applications")) {
                return "Access denied. Only administrators can manage all applications.";
            }
            return "Access denied. Administrator privileges required for this operation.";
        }
        
        // Applicant endpoints
        if (requestPath.startsWith("/api/applicant")) {
            if (requestPath.contains("/profile")) {
                return "Access denied. Only job applicants can access applicant profiles.";
            } else if (requestPath.contains("/resume")) {
                return "Access denied. Only job applicants can manage resumes.";
            } else if (requestPath.contains("/apply")) {
                return "Access denied. Only job applicants can apply for jobs.";
            } else if (requestPath.contains("/applications")) {
                return "Access denied. Only job applicants can view their applications.";
            }
            return "Access denied. Applicant role required to access this feature.";
        }
        
        // Company/Job management endpoints
        if (requestPath.startsWith("/api/jobs")) {
            if (method.equals("POST")) {
                return "Access denied. Only companies can create job postings.";
            } else if (method.equals("PUT") || method.equals("DELETE")) {
                return "Access denied. Only job owners (companies) can modify job postings.";
            } else if (requestPath.contains("/applications")) {
                return "Access denied. Only the hiring company can view job applications.";
            } else if (requestPath.contains("/company")) {
                return "Access denied. Only companies can view their job listings.";
            }
        }
        
        // Profile endpoints
        if (requestPath.startsWith("/api/profiles")) {
            if (method.equals("PUT")) {
                return "Access denied. You can only update your own profile or need admin privileges.";
            }
        }
        
        // Default message
        return "Access denied. You don't have the required permissions to access this resource.";
    }
    
    private String getSuggestion(String requestPath) {
        if (requestPath.startsWith("/api/admin")) {
            return "Please login with an administrator account to access admin features.";
        } else if (requestPath.startsWith("/api/applicant")) {
            return "Please login with a job applicant account to access these features.";
        } else if (requestPath.startsWith("/api/jobs") && (requestPath.contains("applications") || requestPath.contains("company"))) {
            return "Please login with a company account to manage jobs and applications.";
        } else if (requestPath.startsWith("/api/profiles")) {
            return "Please ensure you're accessing your own profile or have proper authorization.";
        }
        return "Please check your account permissions or contact support if you believe this is an error.";
    }
}
