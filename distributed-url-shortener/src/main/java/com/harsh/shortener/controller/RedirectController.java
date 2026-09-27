
package com.harsh.shortener.controller;

import com.harsh.shortener.model.ShortUrl;
import com.harsh.shortener.service.UrlService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
public class RedirectController {

    private final UrlService urlService;

    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode
    ) {
        String originalUrl = urlService.getRedirectUrl(shortCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}