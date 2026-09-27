package com.harsh.shortener.dto;

import com.harsh.shortener.model.AppUser;

import java.time.Instant;

public record RegisterResponse(
        Long id,
        String email,
        Instant createdAt
) {
    public static RegisterResponse from(AppUser user) {
        return new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}