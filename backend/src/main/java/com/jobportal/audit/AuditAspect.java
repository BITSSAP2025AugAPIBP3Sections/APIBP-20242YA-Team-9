package com.jobportal.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.jobportal.service.audit.AuditService;
import com.jobportal.entity.audit.AuditOperation;

import com.jobportal.security.CustomUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class AuditAspect {

    @Autowired
    private AuditLogger auditLogger;

    @Autowired
    private AuditService auditService;

    // Audit all controller methods
    @Around("execution(* com.jobportal.controller.*.*(..))")
    public Object auditControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // Get user information
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

        // Get request information
        HttpServletRequest request = null;
        String ipAddress = null;
        String endpoint = null;
        String httpMethod = null;
        
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                request = attributes.getRequest();
                ipAddress = getClientIpAddress(request);
                endpoint = request.getRequestURI();
                httpMethod = request.getMethod();
            }
        } catch (Exception e) {
            // Handle case where no request context is available
        }

        // Log the API call start
        Map<String, Object> callDetails = new HashMap<>();
        callDetails.put("controller", className);
        callDetails.put("method", methodName);
        callDetails.put("arguments", getArgumentsAsString(joinPoint.getArgs()));

        auditLogger.logUserAction(
            "API_CALL_START", 
            userId, 
            userName, 
            className, 
            methodName, 
            callDetails
        );

        try {
            auditService.logOperationWithRequest(className, methodName, AuditOperation.READ, request);
        } catch (Exception e) {
            System.err.println("Failed to save audit log to database: " + e.getMessage());
        }

        Object result = null;
        int responseCode = 200;
        
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            responseCode = 500;
            auditLogger.logSecurityEvent(
                "API_CALL_ERROR",
                userId,
                userName,
                "Error in " + className + "." + methodName + ": " + e.getMessage(),
                "HIGH"
            );
            throw e;
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Log the API call completion
        if (request != null) {
            auditLogger.logApiCall(httpMethod, endpoint, userId, userName, ipAddress, responseCode, duration);
        }

        return result;
    }

    // Audit authentication operations
    @AfterReturning(pointcut = "execution(* com.jobportal.controller.AuthController.*(..))", returning = "result")
    public void auditAuthenticationSuccess(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        String email = extractEmailFromArgs(args);
        String ipAddress = getClientIpFromRequest();
        
        if ("login".equals(methodName)) {
            auditLogger.logAuthenticationAction("LOGIN_SUCCESS", email, ipAddress, true, "User login successful");
            try {
                auditService.logOperationWithRequest("User", email != null ? email : "unknown", AuditOperation.LOGIN, getCurrentRequest());
            } catch (Exception e) {
                System.err.println("Failed to save login audit to database: " + e.getMessage());
            }
        } else if ("register".equals(methodName)) {
            auditLogger.logAuthenticationAction("REGISTRATION_SUCCESS", email, ipAddress, true, "User registration successful");
        }
    }

    @AfterThrowing(pointcut = "execution(* com.jobportal.controller.AuthController.*(..))", throwing = "ex")
    public void auditAuthenticationFailure(JoinPoint joinPoint, Throwable ex) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        String email = extractEmailFromArgs(args);
        String ipAddress = getClientIpFromRequest();
        
        if ("login".equals(methodName)) {
            auditLogger.logAuthenticationAction("LOGIN_FAILED", email, ipAddress, false, "Login failed: " + ex.getMessage());
        } else if ("register".equals(methodName)) {
            auditLogger.logAuthenticationAction("REGISTRATION_FAILED", email, ipAddress, false, "Registration failed: " + ex.getMessage());
        }
    }

    // Audit job operations
    @After("execution(* com.jobportal.service.JobService.createJob(..))")
    public void auditJobCreation(JoinPoint joinPoint) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            Map<String, Object> details = new HashMap<>();
            details.put("operation", "CREATE_JOB");
            details.put("arguments", getArgumentsAsString(joinPoint.getArgs()));
            
            auditLogger.logBusinessEvent(
                "JOB_CREATED",
                "New job posting created by user",
                userDetails.getUser().getId().toString(),
                userDetails.getUser().getName(),
                details
            );
        }
    }

    @After("execution(* com.jobportal.service.JobService.updateJob(..))")
    public void auditJobUpdate(JoinPoint joinPoint) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            Map<String, Object> details = new HashMap<>();
            details.put("operation", "UPDATE_JOB");
            details.put("arguments", getArgumentsAsString(joinPoint.getArgs()));
            
            auditLogger.logBusinessEvent(
                "JOB_UPDATED",
                "Job posting updated by user",
                userDetails.getUser().getId().toString(),
                userDetails.getUser().getName(),
                details
            );
        }
    }

    // Audit application operations
    @After("execution(* com.jobportal.service.ApplicantService.applyToJob(..))")
    public void auditJobApplication(JoinPoint joinPoint) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            Map<String, Object> details = new HashMap<>();
            details.put("operation", "APPLY_TO_JOB");
            details.put("arguments", getArgumentsAsString(joinPoint.getArgs()));
            
            auditLogger.logBusinessEvent(
                "JOB_APPLICATION_SUBMITTED",
                "User applied to a job position",
                userDetails.getUser().getId().toString(),
                userDetails.getUser().getName(),
                details
            );
        }
    }

    @After("execution(* com.jobportal.service.ApplicantService.withdrawApplication(..))")
    public void auditApplicationWithdrawal(JoinPoint joinPoint) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            Map<String, Object> details = new HashMap<>();
            details.put("operation", "WITHDRAW_APPLICATION");
            details.put("arguments", getArgumentsAsString(joinPoint.getArgs()));
            
            auditLogger.logBusinessEvent(
                "APPLICATION_WITHDRAWN",
                "User withdrew job application",
                userDetails.getUser().getId().toString(),
                userDetails.getUser().getName(),
                details
            );
        }
    }

    // Audit admin operations
    @After("execution(* com.jobportal.service.AdminService.*(..))")
    public void auditAdminOperations(JoinPoint joinPoint) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            String methodName = joinPoint.getSignature().getName();
            Map<String, Object> details = new HashMap<>();
            details.put("operation", methodName);
            details.put("arguments", getArgumentsAsString(joinPoint.getArgs()));
            
            auditLogger.logBusinessEvent(
                "ADMIN_OPERATION",
                "Administrator performed operation: " + methodName,
                userDetails.getUser().getId().toString(),
                userDetails.getUser().getName(),
                details
            );
        }
    }

    // Audit security events
    @AfterThrowing(pointcut = "execution(* com.jobportal.controller.*.*(..))", throwing = "ex")
    public void auditSecurityExceptions(JoinPoint joinPoint, Throwable ex) {
        if (ex instanceof org.springframework.security.access.AccessDeniedException) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = null;
            String userName = null;
            
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                userId = userDetails.getUser().getId().toString();
                userName = userDetails.getUser().getName();
            }
            
            auditLogger.logSecurityEvent(
                "ACCESS_DENIED",
                userId,
                userName,
                "Access denied for " + joinPoint.getSignature().getName() + ": " + ex.getMessage(),
                "HIGH"
            );
        }
    }

    // Helper methods
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

    private String getClientIpFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return getClientIpAddress(attributes.getRequest());
            }
        } catch (Exception e) {
            // Handle gracefully
        }
        return "Unknown";
    }

    private String extractEmailFromArgs(Object[] args) {
        for (Object arg : args) {
            if (arg != null) {
                try {
                    // Try to get email from various request objects
                    if (arg.getClass().getSimpleName().contains("Login") || 
                        arg.getClass().getSimpleName().contains("Register")) {
                        java.lang.reflect.Method getEmail = arg.getClass().getMethod("getEmail");
                        return (String) getEmail.invoke(arg);
                    }
                } catch (Exception e) {
                    // Continue trying other arguments
                }
            }
        }
        return "Unknown";
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getArgumentsAsString(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            if (args[i] == null) {
                sb.append("null");
            } else {
                String className = args[i].getClass().getSimpleName();
                sb.append(className);
                // Don't log sensitive data like passwords
                if (!className.toLowerCase().contains("password") && 
                    !className.toLowerCase().contains("token")) {
                    sb.append("(").append(args[i].toString().length() > 100 ? 
                        args[i].toString().substring(0, 100) + "..." : args[i].toString()).append(")");
                } else {
                    sb.append("(***HIDDEN***)");
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
