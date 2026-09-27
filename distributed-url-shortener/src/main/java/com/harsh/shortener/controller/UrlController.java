package com.harsh.shortener.controller;

import com.harsh.shortener.dto.CreateUrlRequest;
import com.harsh.shortener.service.RateLimitService;
import com.harsh.shortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {

    private final UrlService urlService;
    private final RateLimitService rateLimitService;

    public UrlController(
            UrlService urlService,
            RateLimitService rateLimitService) {

        this.urlService = urlService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    public ResponseEntity<?> createUrl(
            @Valid @RequestBody CreateUrlRequest request,
            Principal principal) {

        String userEmail = principal.getName();

        if (!rateLimitService.isAllowed(userEmail)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .body(Map.of(
                            "error", "Too many requests",
                            "message", "URL creation rate limit exceeded"
                    ));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(urlService.createUrl(request, userEmail));
    }
}