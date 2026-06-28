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

        Map<String, RedisCacheConfiguration> perCache = Map.of(
                CacheConstants.SIMULATION_BY_ID, defaults.entryTtl(Duration.ofSeconds(30)),
                CacheConstants.SIMULATIONS_LIST, defaults.entryTtl(Duration.ofSeconds(20)),
                CacheConstants.METRICS_SUMMARY, defaults.entryTtl(Duration.ofSeconds(60)),
                CacheConstants.RECOMMENDATIONS, defaults.entryTtl(Duration.ofSeconds(30)),
                CacheConstants.DASHBOARD_SUMMARY, defaults.entryTtl(Duration.ofSeconds(20)),
                CacheConstants.LOG_SUMMARY,   defaults.entryTtl(Duration.ofSeconds(30)),
                CacheConstants.LOGS_BY_SIM,   defaults.entryTtl(Duration.ofSeconds(20))
        );

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
