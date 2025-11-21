package com.jobportal.controller;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for cache management and monitoring
 * Provides endpoints to view cache statistics and clear caches
 */
@RestController
@RequestMapping("/api/v1/cache")
@Tag(name = "Cache Management", description = "Cache monitoring and management operations")
public class CacheController {

    private final CacheManager cacheManager;

    public CacheController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Get statistics for all caches
     */
    @Operation(summary = "Get cache statistics", description = "View statistics for all configured caches (Admin only)")
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        for (String cacheName : cacheManager.getCacheNames()) {
            CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheName);
            if (cache != null) {
                CacheStats cacheStats = cache.getNativeCache().stats();
                
                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("hitCount", cacheStats.hitCount());
                cacheInfo.put("missCount", cacheStats.missCount());
                cacheInfo.put("hitRate", String.format("%.2f%%", cacheStats.hitRate() * 100));
                cacheInfo.put("evictionCount", cacheStats.evictionCount());
                cacheInfo.put("loadSuccessCount", cacheStats.loadSuccessCount());
                cacheInfo.put("loadFailureCount", cacheStats.loadFailureCount());
                cacheInfo.put("totalLoadTime", cacheStats.totalLoadTime());
                cacheInfo.put("estimatedSize", cache.getNativeCache().estimatedSize());
                
                stats.put(cacheName, cacheInfo);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Cache statistics retrieved successfully");
        response.put("data", stats);
        response.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Clear a specific cache
     */
    @Operation(summary = "Clear specific cache", description = "Clear all entries from a specific cache (Admin only)")
    @DeleteMapping("/{cacheName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> clearCache(@PathVariable String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        
        if (cache == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Cache not found: " + cacheName);
            response.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.notFound().build();
        }
        
        cache.clear();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Cache cleared successfully: " + cacheName);
        response.put("cacheName", cacheName);
        response.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Clear all caches
     */
    @Operation(summary = "Clear all caches", description = "Clear all entries from all caches (Admin only)")
    @DeleteMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> clearAllCaches() {
        int clearedCount = 0;
        
        for (String cacheName : cacheManager.getCacheNames()) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                clearedCount++;
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "All caches cleared successfully");
        response.put("clearedCaches", clearedCount);
        response.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get list of all cache names
     */
    @Operation(summary = "Get cache names", description = "Get list of all configured cache names (Admin only)")
    @GetMapping("/names")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCacheNames() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Cache names retrieved successfully");
        response.put("data", cacheManager.getCacheNames());
        response.put("count", cacheManager.getCacheNames().size());
        response.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }
}
