package com.quad.services.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration for QUAD Services
 *
 * Caching Strategy:
 * 1. agentRules - Cache industry defaults + org customizations (5 min TTL)
 *    - Rules don't change often
 *    - High hit rate expected
 *    - Saves database queries on every code generation request
 *
 * 2. orgContext - Cache organization context for AI prompts (10 min TTL)
 *    - Industry, settings, preferences
 *    - Used in every AI call
 *
 * 3. userHistory - Cache recent user coding patterns (15 min TTL)
 *    - For RAG context building
 *    - Personalized code suggestions
 *
 * Cache Invalidation:
 * - On rule update: Evict agentRules cache for that org/industry
 * - On org settings change: Evict orgContext cache
 * - Manual: POST /api/cache/clear (admin only)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Default cache spec: 1000 entries, 5 minutes TTL
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());  // Enable stats for monitoring

        // Register cache names
        cacheManager.setCacheNames(java.util.List.of(
                "agentRules",      // Industry defaults + org customizations
                "orgContext",      // Organization settings/context
                "userHistory",     // User coding history for RAG
                "activityTypes"    // Activity type catalog
        ));

        return cacheManager;
    }
}
