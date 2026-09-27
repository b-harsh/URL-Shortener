package com.harsh.shortener.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int WINDOW_SECONDS = 15 * 60;

    private static final String INCREMENT_SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> incrementScript;

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.incrementScript = new DefaultRedisScript<>();
        this.incrementScript.setScriptText(INCREMENT_SCRIPT);
        this.incrementScript.setResultType(Long.class);
    }

    public boolean isBlocked(String email) {
        String count = redisTemplate.opsForValue().get(keyFor(email));

        return count != null
                && Long.parseLong(count) >= MAX_FAILED_ATTEMPTS;
    }

    public void recordFailure(String email) {
        redisTemplate.execute(
                incrementScript,
                List.of(keyFor(email)),
                String.valueOf(WINDOW_SECONDS)
        );
    }

    public void reset(String email) {
        redisTemplate.delete(keyFor(email));
    }

    private String keyFor(String email) {
        return "rate-limit:login:" + sha256(email);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }
}