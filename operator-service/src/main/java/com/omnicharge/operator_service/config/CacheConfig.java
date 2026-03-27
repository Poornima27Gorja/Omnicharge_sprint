package com.omnicharge.operator_service.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * operators        — cache all operators list       TTL: 10 minutes
     * operator-by-id   — cache single operator by id   TTL: 10 minutes
     * plans            — cache all plans list           TTL: 10 minutes
     * plan-by-id       — cache single plan by id        TTL: 10 minutes
     * plans-by-operator— cache plans for an operator   TTL: 10 minutes
     *
     * Why cache operators and plans?
     * These are read VERY frequently (every recharge initiation calls operator + plan),
     * but written rarely (only by ADMIN). Caching reduces DB hits dramatically.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // Default config — JSON serialization, no null values cached
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Per-cache TTL overrides
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("operators",          defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("operator-by-id",     defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("plans",              defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("plan-by-id",         defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("plans-by-operator",  defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
