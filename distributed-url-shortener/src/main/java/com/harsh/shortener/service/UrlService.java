package com.harsh.shortener.service;

import com.harsh.shortener.dto.CreateUrlRequest;
import com.harsh.shortener.model.AppUser;
import com.harsh.shortener.model.ShortUrl;
import com.harsh.shortener.repository.AppUserRepository;
import com.harsh.shortener.repository.ShortUrlRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class UrlService {

    private final EntityManager entityManager;
    private final Base62Service base62Service;
    private final ShortUrlRepository shortUrlRepository;
    private final UrlCacheService urlCacheService;
    private final AppUserRepository appUserRepository;
    private final ShortenerMetrics shortenerMetrics;

    public UrlService(
            EntityManager entityManager,
            Base62Service base62Service,
            ShortUrlRepository shortUrlRepository,
            UrlCacheService urlCacheService,
            AppUserRepository appUserRepository,
            ShortenerMetrics shortenerMetrics
    ) {
        this.entityManager = entityManager;
        this.base62Service = base62Service;
        this.shortUrlRepository = shortUrlRepository;
        this.urlCacheService = urlCacheService;
        this.appUserRepository = appUserRepository;
        this.shortenerMetrics = shortenerMetrics;
    }

    @Transactional
    public ShortUrl createUrl(CreateUrlRequest request, String email) {

        String originalUrl = request.originalUrl().trim();

        validateUrl(originalUrl);

        if (request.expiresAt() != null
                && !request.expiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Expiration time must be in the future"
            );
        }

        AppUser owner = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        Long id = ((Number) entityManager
                .createNativeQuery("SELECT nextval('short_urls_id_seq')")
                .getSingleResult())
                .longValue();

        String shortCode = base62Service.encode(id);

        ShortUrl shortUrl = new ShortUrl(
                id,
                originalUrl,
                shortCode
        );

        shortUrl.setExpiresAt(request.expiresAt());
        shortUrl.setOwner(owner);

        entityManager.persist(shortUrl);

        shortenerMetrics.incrementUrlCreations();

        return shortUrl;
    }

    @Transactional(readOnly = true)
    public ShortUrl getByShortCode(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Short URL not found"
                ));

        validateActiveUrl(shortUrl);

        return shortUrl;
    }

    private void validateUrl(String originalUrl) {
        try {
            URI uri = URI.create(originalUrl);
            String scheme = uri.getScheme();

            if (scheme == null
                    || !(scheme.equalsIgnoreCase("http")
                    || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }

        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid HTTP or HTTPS URL is required"
            );
        }
    }

    @Transactional(readOnly = true)
    public ShortUrl getDetails(String shortCode) {
        return getOwnedUrl(shortCode, currentUserEmail());
    }

    @Transactional(readOnly = true)
    public ShortUrl getDetails(String shortCode, String email) {
        return getOwnedUrl(shortCode, email);
    }

    @Transactional
    public ShortUrl deactivateUrl(String shortCode) {
        return deactivateUrl(shortCode, currentUserEmail());
    }

    @Transactional
    public ShortUrl deactivateUrl(String shortCode, String email) {
        ShortUrl shortUrl = getOwnedUrl(shortCode, email);

        shortUrl.setActive(false);
        evictAfterCommit(shortCode);

        return shortUrl;
    }

    @Transactional
    public ShortUrl reactivateUrl(String shortCode) {
        return reactivateUrl(shortCode, currentUserEmail());
    }

    @Transactional
    public ShortUrl reactivateUrl(String shortCode, String email) {
        ShortUrl shortUrl = getOwnedUrl(shortCode, email);

        shortUrl.setActive(true);
        evictAfterCommit(shortCode);

        return shortUrl;
    }

    @Transactional(readOnly = true)
    public Page<ShortUrl> getAllUrls(int page, int size) {
        return getAllUrls(page, size, currentUserEmail());
    }

    @Transactional(readOnly = true)
    public Page<ShortUrl> getAllUrls(
            int page,
            int size,
            String email
    ) {
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and 100"
            );
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return shortUrlRepository.findAllByOwner_Email(
                email,
                pageRequest
        );
    }

    private ShortUrl getOwnedUrl(String shortCode, String email) {
        return shortUrlRepository
                .findByShortCodeAndOwner_Email(shortCode, email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Short URL not found"
                ));
    }

    private String currentUserEmail() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        return authentication.getName();
    }

    @Transactional(readOnly = true)
    public String getRedirectUrl(String shortCode) {
        Optional<String> cachedUrl = urlCacheService.get(shortCode);

        if (cachedUrl.isPresent()) {
            shortenerMetrics.incrementRedirectLookups();
            return cachedUrl.get();
        }

        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Short URL not found"
                ));

        validateActiveUrl(shortUrl);

        Instant now = Instant.now();

        Duration ttl = shortUrl.getExpiresAt() == null
                ? Duration.ofHours(1)
                : Duration.between(now, shortUrl.getExpiresAt());

        urlCacheService.put(
                shortCode,
                shortUrl.getOriginalUrl(),
                ttl
        );

        shortenerMetrics.incrementRedirectLookups();

        return shortUrl.getOriginalUrl();
    }

    private void validateActiveUrl(ShortUrl shortUrl) {
        Instant now = Instant.now();

        if (!shortUrl.isActive()
                || (shortUrl.getExpiresAt() != null
                && !shortUrl.getExpiresAt().isAfter(now))) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Short URL is inactive or expired"
            );
        }
    }

    private void evictAfterCommit(String shortCode) {
        if (TransactionSynchronizationManager
                .isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            urlCacheService.evict(shortCode);
                        }
                    }
            );
        } else {
            urlCacheService.evict(shortCode);
        }
    }
}