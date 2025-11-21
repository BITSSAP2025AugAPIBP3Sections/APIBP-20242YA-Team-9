package com.jobportal.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration for API-level caching
 * Uses Caffeine as the cache provider for high-performance in-memory caching
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine cache manager with multiple cache regions
     * Each cache has specific TTL and size limits based on use case
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "jobs",              // Job listings cache
            "job",               // Individual job cache
            "applications",      // Applications cache
            "userJobs",          // User's jobs cache
            "jobApplications",   // Job applications cache
            "activeJobs",        // Active jobs cache
            "jobsPaginated",     // Paginated jobs cache
            "applicationsPaginated",  // Paginated applications cache
            "userJobsPaginated", // Paginated user jobs cache
            "users",             // Users cache
            "companies"          // Companies cache
        );
        
        // Default cache configuration
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)  // Cache expires after 5 minutes
            .maximumSize(1000)                       // Max 1000 entries per cache
            .recordStats());                         // Enable cache statistics
        
        return cacheManager;
    }

    /**
     * Custom cache configuration for specific caches with different TTLs
     */
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .recordStats();
    }

    /**
     * Cache for job listings (shorter TTL as they change frequently)
     */
    @Bean
    public Caffeine<Object, Object> jobsCaffeineConfig() {
        return Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)  // 2 minutes for job listings
            .maximumSize(500)
            .recordStats();
    }

    /**
     * Cache for individual jobs (longer TTL)
     */
    @Bean
    public Caffeine<Object, Object> jobCaffeineConfig() {
        return Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)  // 10 minutes for individual jobs
            .maximumSize(2000)
            .recordStats();
    }

    /**
     * Cache for active jobs (very short TTL)
     */
    @Bean
    public Caffeine<Object, Object> activeJobsCaffeineConfig() {
        return Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)   // 1 minute for active status
            .maximumSize(500)
            .recordStats();
    }
}
