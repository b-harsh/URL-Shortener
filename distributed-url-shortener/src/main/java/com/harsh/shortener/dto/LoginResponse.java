package com.harsh.shortener.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}