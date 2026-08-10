package com.caltalk.backend.ai;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OpenAiFallbackBudget {
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("yyyyMMddHH").withZone(ZoneOffset.UTC);
    private final StringRedisTemplate redis;
    private final int hourlyLimit;

    public OpenAiFallbackBudget(StringRedisTemplate redis,
            @Value("${caltalk.ai.openai.hourly-limit:20}") int hourlyLimit) {
        this.redis = redis;
        this.hourlyLimit = hourlyLimit;
    }

    public boolean tryAcquire() {
        if (hourlyLimit <= 0) return false;
        String key = "caltalk:ai:openai:hourly:" + HOUR.format(Instant.now());
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) redis.expire(key, Duration.ofHours(2));
        return count != null && count <= hourlyLimit;
    }
}
