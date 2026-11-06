package com.hwans.apiserver.common.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * 로컬 개발용 임베디드 Redis (local 프로파일에서만). 포트는 spring.data.redis.port 를 따른다.
 */
@Slf4j
@Profile("local")
@Configuration
@RequiredArgsConstructor
public class EmbeddedRedisConfig {
    private final RedisProperties redisProperties;

    private RedisServer redisServer;

    @PostConstruct
    public void redisServer() throws IOException {
        redisServer = RedisServer.newRedisServer().port(redisProperties.getPort()).build();
        redisServer.start();
        log.info("embedded redis started on port {}", redisProperties.getPort());
    }

    @PreDestroy
    public void stopRedis() throws IOException {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}
