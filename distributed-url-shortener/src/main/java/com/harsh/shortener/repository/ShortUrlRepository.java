package com.harsh.shortener.repository;

import com.harsh.shortener.model.ShortUrl;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    Page<ShortUrl> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<ShortUrl> findByShortCodeAndOwner_Email(
            String shortCode,
            String email
    );

    Page<ShortUrl> findAllByOwner_Email(
            String email,
            Pageable pageable
    );
}