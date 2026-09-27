package com.harsh.shortener.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RateLimitService {

    private static final int WINDOW_SECONDS = 60;
    private static final long MAX_REQUESTS = 10;

    private static final String INCREMENT_SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> script;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.script = new DefaultRedisScript<>();
        this.script.setScriptText(INCREMENT_SCRIPT);
        this.script.setResultType(Long.class);
    }

    public boolean isAllowed(String userEmail) {
        String key = "rate-limit:url-create:" + userEmail;

        Long requestCount = redisTemplate.execute(
                script,
                List.of(key),
                String.valueOf(WINDOW_SECONDS)
        );

        return requestCount != null && requestCount <= MAX_REQUESTS;
    }
}