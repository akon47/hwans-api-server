package com.hwans.apiserver.common.security.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 카운터를 이용한 간단한 분당 요청 제한 (기기 로그인 코드 발급 남용 방지용)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimiter {
    private static final String KEY_PREFIX = "rate-limit:";
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 같은 bucket/key 로 1분 안에 limitPerMinute 번까지 허용한다.
     *
     * @param bucket         용도 (예: device-code)
     * @param key            주체 (예: 클라이언트 IP)
     * @param limitPerMinute 분당 허용 횟수
     * @return 허용 여부
     */
    public boolean allow(String bucket, String key, int limitPerMinute) {
        var redisKey = KEY_PREFIX + bucket + ":" + key;
        try {
            var count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, Duration.ofMinutes(1));
            }
            return count == null || count <= limitPerMinute;
        } catch (Exception e) {
            // Redis 장애가 기능 자체를 막지는 않도록 한다.
            log.warn("rate limiter unavailable, allowing request: {}", e.getMessage());
            return true;
        }
    }
}
