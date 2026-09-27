package com.harsh.shortener.controller;

import com.harsh.shortener.dto.UrlDetailsResponse;
import com.harsh.shortener.model.ShortUrl;
import com.harsh.shortener.service.UrlService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlManagementController {

    private final UrlService urlService;

    public UrlManagementController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<UrlDetailsResponse> getDetails(
            @PathVariable String shortCode
    ) {
        ShortUrl shortUrl = urlService.getDetails(shortCode);
        return ResponseEntity.ok(UrlDetailsResponse.from(shortUrl));
    }

    @PatchMapping("/{shortCode}/deactivate")
    public ResponseEntity<UrlDetailsResponse> deactivateUrl(
            @PathVariable String shortCode
    ) {
        ShortUrl shortUrl = urlService.deactivateUrl(shortCode);
        return ResponseEntity.ok(UrlDetailsResponse.from(shortUrl));
    }

    @PatchMapping("/{shortCode}/reactivate")
    public ResponseEntity<UrlDetailsResponse> reactivateUrl(
            @PathVariable String shortCode
    ) {
        ShortUrl shortUrl = urlService.reactivateUrl(shortCode);
        return ResponseEntity.ok(UrlDetailsResponse.from(shortUrl));
    }

    @GetMapping
    public ResponseEntity<Page<UrlDetailsResponse>> getAllUrls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<UrlDetailsResponse> response = urlService
                .getAllUrls(page, size)
                .map(UrlDetailsResponse::from);

        return ResponseEntity.ok(response);
    }
}