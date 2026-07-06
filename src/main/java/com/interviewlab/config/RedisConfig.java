package com.interviewlab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;

/**
 * Redis wiring for RedisSessionStore — only created when SESSION_STORE=redis.
 * Spring Boot's own Redis auto-configuration is excluded (see application.yml
 * spring.autoconfigure.exclude) because it eagerly instantiates a connection factory
 * for every profile, including SESSION_STORE=memory where REDIS_URL is blank — this
 * class owns the connection factory too, so an unset REDIS_URL never breaks a plain
 * in-memory run. Uses GenericJackson2JsonRedisSerializer for values, never JDK
 * serialization, which breaks on Java records (see ADR-009).
 */
@Configuration
@ConditionalOnProperty(name = "app.session.store", havingValue = "redis")
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(@Value("${spring.data.redis.url}") String redisUrl) {
        URI uri = URI.create(redisUrl);
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(uri.getHost(), uri.getPort());
        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            String password = userInfo.contains(":") ? userInfo.substring(userInfo.indexOf(':') + 1) : userInfo;
            standaloneConfig.setPassword(RedisPassword.of(password));
        }

        boolean useTls = "rediss".equalsIgnoreCase(uri.getScheme());
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
            LettuceClientConfiguration.builder();
        if (useTls) {
            clientConfigBuilder.useSsl();
        }

        return new LettuceConnectionFactory(standaloneConfig, clientConfigBuilder.build());
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
