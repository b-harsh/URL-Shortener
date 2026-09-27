package com.harsh.shortener.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ShortenerMetrics {

    private final Counter urlCreations;
    private final Counter redirectLookups;
    private final Counter urlCreationRateLimitRejections;

    public ShortenerMetrics(MeterRegistry registry) {
        this.urlCreations = Counter.builder("shortener.urls.created")
                .description("Number of URLs successfully created")
                .register(registry);

        this.redirectLookups = Counter.builder("shortener.redirects")
                .description("Number of successful redirect URL lookups")
                .register(registry);

        this.urlCreationRateLimitRejections =
                Counter.builder("shortener.rate_limit.rejections")
                        .description("Number of rejected URL creation requests")
                        .register(registry);
    }

    public void incrementUrlCreations() {
        urlCreations.increment();
    }

    public void incrementRedirectLookups() {
        redirectLookups.increment();
    }

    public void incrementUrlCreationRateLimitRejections() {
        urlCreationRateLimitRejections.increment();
    }
}