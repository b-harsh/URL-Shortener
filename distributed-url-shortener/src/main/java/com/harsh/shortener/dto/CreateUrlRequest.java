
package com.harsh.shortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateUrlRequest(

        @NotBlank(message = "URL is required")
        @Size(max = 2048, message = "URL is too long")
        String originalUrl,

        Instant expiresAt

) {
}