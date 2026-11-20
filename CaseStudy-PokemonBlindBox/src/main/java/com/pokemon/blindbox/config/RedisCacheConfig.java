package com.pokemon.blindbox.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Cấu hình RedisCacheManager chuẩn Spring Boot 3 theo .agent/rules.md:
 * - Key: StringRedisSerializer
 * - Value: GenericJackson2JsonRedisSerializer (tích hợp JavaTimeModule)
 * - 3 vùng nhớ Cache chuyên biệt:
 * 1. card_sets (TTL: 24h)
 * 2. rarities (TTL: 24h)
 * 3. card_templates (TTL: 12h)
 */
@Configuration
@EnableCaching
@Slf4j
public class RedisCacheConfig implements CachingConfigurer {

        public static final String CACHE_CARD_SETS = "card_sets";
        public static final String CACHE_RARITIES = "rarities";
        public static final String CACHE_CARD_TEMPLATES = "card_templates";
        public static final String CACHE_CARD_SPECIES = "card_species";

        @Bean
        public GenericJackson2JsonRedisSerializer redisJsonSerializer() {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                // P2-04: Giới hạn PolymorphicTypeValidator chỉ cho phép package nội bộ & kiểu chuẩn Java
                objectMapper.activateDefaultTyping(
                                BasicPolymorphicTypeValidator.builder()
                                                .allowIfSubType("com.pokemon.blindbox.")
                                                .allowIfSubType("java.util.")
                                                .allowIfSubType("java.time.")
                                                .allowIfSubType("java.math.")
                                                .build(),
                                ObjectMapper.DefaultTyping.NON_FINAL_AND_ENUMS,
                                JsonTypeInfo.As.PROPERTY);
                return new GenericJackson2JsonRedisSerializer(objectMapper);
        }

        @Bean
        public CacheManager cacheManager(
                        RedisConnectionFactory connectionFactory,
                        GenericJackson2JsonRedisSerializer redisJsonSerializer) {
                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .disableCachingNullValues()
                                .serializeKeysWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(redisJsonSerializer))
                                .entryTtl(Duration.ofHours(12));

                Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                                CACHE_CARD_SETS, defaultConfig.entryTtl(Duration.ofHours(24)),
                                CACHE_RARITIES, defaultConfig.entryTtl(Duration.ofHours(24)),
                                CACHE_CARD_TEMPLATES, defaultConfig.entryTtl(Duration.ofHours(12)),
                                CACHE_CARD_SPECIES, defaultConfig.entryTtl(Duration.ofHours(24)));

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withInitialCacheConfigurations(cacheConfigurations)
                                .build();
        }

        /**
         * Đảm bảo ứng dụng vẫn hoạt động trơn tru với MySQL ngay cả khi Redis Server
         * trên máy đang tắt
         * (Graceful Degradation: tự động đọc trực tiếp từ MySQL nếu Redis chưa bật).
         */
        @Override
        public CacheErrorHandler errorHandler() {
                return new CacheErrorHandler() {
                        @Override
                        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                                log.debug("[REDIS BYPASS] Cannot read cache '{}' key '{}', falling back to MySQL: {}",
                                                cache.getName(), key, exception.getMessage());
                        }

                        @Override
                        public void handleCachePutError(RuntimeException exception, Cache cache, Object key,
                                        Object value) {
                                log.debug("[REDIS BYPASS] Cannot write cache '{}' key '{}': {}",
                                                cache.getName(), key, exception.getMessage());
                        }

                        @Override
                        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                                log.debug("[REDIS BYPASS] Cannot evict cache '{}' key '{}': {}",
                                                cache.getName(), key, exception.getMessage());
                        }

                        @Override
                        public void handleCacheClearError(RuntimeException exception, Cache cache) {
                                log.debug("[REDIS BYPASS] Cannot clear cache '{}': {}",
                                                cache.getName(), exception.getMessage());
                        }
                };
        }
}