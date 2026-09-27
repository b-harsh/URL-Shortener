package com.harsh.shortener.dto;

import com.harsh.shortener.model.ShortUrl;

import java.time.Instant;

public record UrlDetailsResponse(
        Long id,
        String shortCode,
        String originalUrl,
        boolean active,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static UrlDetailsResponse from(ShortUrl shortUrl) {
        return new UrlDetailsResponse(
                shortUrl.getId(),
                shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.isActive(),
                shortUrl.getExpiresAt(),
                shortUrl.getCreatedAt(),
                shortUrl.getUpdatedAt()
        );
    }
}