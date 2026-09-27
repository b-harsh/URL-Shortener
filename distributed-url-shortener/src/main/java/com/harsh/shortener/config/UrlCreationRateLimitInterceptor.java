package com.harsh.shortener.config;

import com.harsh.shortener.service.RateLimitService;
import com.harsh.shortener.service.ShortenerMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.security.Principal;

@Component
public class UrlCreationRateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final ShortenerMetrics shortenerMetrics;

    public UrlCreationRateLimitInterceptor(
            RateLimitService rateLimitService,
            ShortenerMetrics shortenerMetrics
    ) {
        this.rateLimitService = rateLimitService;
        this.shortenerMetrics = shortenerMetrics;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {

        Principal principal = request.getUserPrincipal();

        if (principal == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        boolean allowed = rateLimitService.isAllowed(principal.getName());

        if (!allowed) {
            shortenerMetrics.incrementUrlCreationRateLimitRejections();

            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("""
            {
              "error": "RATE_LIMIT_EXCEEDED",
              "message": "Too many URL creation requests. Try again later."
            }
            """);
            return false;
        }

        return true;
    }
}