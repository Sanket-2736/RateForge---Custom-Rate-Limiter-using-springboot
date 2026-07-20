package com.backend.rate_limiter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

@Configuration
public class RedisConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Autowired
    private RedisProperties redisProperties;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        logger.info("Configuring Redis connection to {}:{}", redisProperties.getHost(), redisProperties.getPort());

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());

        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            config.setPassword(redisProperties.getPassword());
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        logger.info("Redis connection factory configured successfully");

        return factory;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    public ScriptSource tokenBucketScriptSource() {
        logger.info("Loading token bucket Lua script");
        return new ResourceScriptSource(new ClassPathResource("scripts/token_bucket.lua"));
    }

    @Bean
    public ScriptSource slidingWindowScriptSource() {
        logger.info("Loading sliding window Lua script");
        return new ResourceScriptSource(new ClassPathResource("scripts/sliding_window.lua"));
    }

    @Bean
    public ScriptSource leakyBucketScriptSource() {
        logger.info("Loading leaky bucket Lua script");
        return new ResourceScriptSource(new ClassPathResource("scripts/leaky_bucket.lua"));
    }
}