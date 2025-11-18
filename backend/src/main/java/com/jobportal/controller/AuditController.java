package com.jobportal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.jobportal.entity.audit.AuditLog;
import com.jobportal.entity.audit.AuditOperation;
import com.jobportal.service.audit.AuditService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Audit", description = "Audit log management and viewing (Admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @GetMapping("/entity/{entityName}")
    @Operation(
        summary = "Get audit logs by entity type",
        description = "Retrieve all audit logs for a specific entity type with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Map<String, Object>> getAuditLogsByEntity(
            @Parameter(description = "Entity name (e.g., User, Job, Application)", required = true) 
            @PathVariable String entityName,
            @Parameter(description = "Page number (0-based)") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditService.getAuditLogsByEntity(entityName, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Audit logs retrieved successfully");
        response.put("data", auditLogs.getContent());
        response.put("totalElements", auditLogs.getTotalElements());
        response.put("totalPages", auditLogs.getTotalPages());
        response.put("currentPage", page);
        response.put("pageSize", size);
        response.put("entityName", entityName);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/entity/{entityName}/{entityId}")
    @Operation(
        summary = "Get audit logs for specific entity",
        description = "Retrieve all audit logs for a specific entity instance"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Entity audit logs retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Map<String, Object>> getAuditLogsForEntity(
            @Parameter(description = "Entity name", required = true) @PathVariable String entityName,
            @Parameter(description = "Entity ID", required = true) @PathVariable String entityId) {
        
        List<AuditLog> auditLogs = auditService.getAuditLogsForEntity(entityName, entityId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Entity audit logs retrieved successfully");
        response.put("data", auditLogs);
        response.put("count", auditLogs.size());
        response.put("entityName", entityName);
        response.put("entityId", entityId);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userEmail}")
    @Operation(
        summary = "Get audit logs by user",
        description = "Retrieve all audit logs for a specific user with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User audit logs retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Map<String, Object>> getAuditLogsByUser(
            @Parameter(description = "User email", required = true) @PathVariable String userEmail,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditLogs = auditService.getAuditLogsByUser(userEmail, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "User audit logs retrieved successfully");
        response.put("data", auditLogs.getContent());
        response.put("totalElements", auditLogs.getTotalElements());
        response.put("totalPages", auditLogs.getTotalPages());
        response.put("currentPage", page);
        response.put("pageSize", size);
        response.put("userEmail", userEmail);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/operation/{operation}")
    @Operation(
        summary = "Get audit logs by operation",
        description = "Retrieve all audit logs for a specific operation type"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Operation audit logs retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid operation type"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Map<String, Object>> getAuditLogsByOperation(
            @Parameter(description = "Operation type (CREATE, UPDATE, DELETE, etc.)", required = true) 
            @PathVariable String operation) {
        
        try {
            AuditOperation auditOperation = AuditOperation.valueOf(operation.toUpperCase());
            List<AuditLog> auditLogs = auditService.getAuditLogsByOperation(auditOperation);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Operation audit logs retrieved successfully");
            response.put("data", auditLogs);
            response.put("count", auditLogs.size());
            response.put("operation", operation);
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Invalid operation type: " + operation);
            response.put("validOperations", List.of(AuditOperation.values()));
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/daterange")
    @Operation(
        summary = "Get audit logs by date range",
        description = "Retrieve audit logs within a specific date range"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Date range audit logs retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date format"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Map<String, Object>> getAuditLogsByDateRange(
            @Parameter(description = "Start date (ISO format)", required = true) @RequestParam String startDate,
            @Parameter(description = "End date (ISO format)", required = true) @RequestParam String endDate) {
        
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            
            List<AuditLog> auditLogs = auditService.getAuditLogsByDateRange(start, end);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Date range audit logs retrieved successfully");
            response.put("data", auditLogs);
            response.put("count", auditLogs.size());
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Invalid date format. Please use ISO format (yyyy-MM-ddTHH:mm:ss)");
            response.put("error", e.getMessage());
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "Get audit statistics",
        description = "Retrieve audit log statistics and summaries"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Audit statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<Map<String, Object>> getAuditStatistics() {
        List<String> entityNames = auditService.getAllEntityNames();
        
        Map<String, Object> statistics = new HashMap<>();
        
        // Get operation counts for the last 24 hours
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        for (AuditOperation operation : AuditOperation.values()) {
            Long count = auditService.getOperationCountSince(operation, last24Hours);
            statistics.put(operation.name().toLowerCase() + "_last_24h", count);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Audit statistics retrieved successfully");
        response.put("data", Map.of(
            "entityNames", entityNames,
            "operationCounts", statistics
        ));
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
}
