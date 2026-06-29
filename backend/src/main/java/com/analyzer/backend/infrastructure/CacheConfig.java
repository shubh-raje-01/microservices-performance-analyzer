package com.analyzer.backend.infrastructure;

import com.analyzer.backend.common.constants.CacheConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration
                .defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                );

        Map<String, RedisCacheConfiguration> perCache = Map.ofEntries(

                // ── Simulation
                // Short TTL — status changes (PENDING → RUNNING → COMPLETED)
                Map.entry(CacheConstants.SIMULATION_BY_ID,
                        defaults.entryTtl(Duration.ofSeconds(30))),
                // List view is evicted on every create/delete
                Map.entry(CacheConstants.SIMULATIONS_LIST,
                        defaults.entryTtl(Duration.ofSeconds(20))),

                // ── Metrics
                // Aggregated values never change after collection — longer TTL
                Map.entry(CacheConstants.METRICS_BY_SIM,
                        defaults.entryTtl(Duration.ofSeconds(120))),
                // Summary DTO is a subset of aggregated — same reasoning
                Map.entry(CacheConstants.METRICS_SUMMARY,
                        defaults.entryTtl(Duration.ofSeconds(120))),

                // ── Logging
                // Log counts are stable once the simulation completes
                Map.entry(CacheConstants.LOG_SUMMARY,
                        defaults.entryTtl(Duration.ofSeconds(60))),
                Map.entry(CacheConstants.LOGS_BY_SIM,
                        defaults.entryTtl(Duration.ofSeconds(60))),

                // ── AI
                // FastAPI calls are expensive — cache aggressively
                Map.entry(CacheConstants.AI_INSIGHT,
                        defaults.entryTtl(Duration.ofMinutes(10))),

                // ── Recommendation
                // Generated once per analysis — stable until re-analysis
                Map.entry(CacheConstants.RECOMMENDATIONS,
                        defaults.entryTtl(Duration.ofMinutes(5))),

                // ── Dashboard
                // Assembled from already-cached sources — short TTL acceptable
                Map.entry(CacheConstants.DASHBOARD_SUMMARY,
                        defaults.entryTtl(Duration.ofSeconds(30))),

                // ── Shared
                // Service name list grows slowly — long TTL
                Map.entry(CacheConstants.SERVICE_NAMES,
                        defaults.entryTtl(Duration.ofMinutes(15)))
        );

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}