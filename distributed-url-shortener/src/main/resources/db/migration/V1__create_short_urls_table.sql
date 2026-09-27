
CREATE TABLE short_urls (
                            id BIGSERIAL PRIMARY KEY,
                            short_code VARCHAR(32) UNIQUE NOT NULL,
                            original_url TEXT NOT NULL,
                            is_active BOOLEAN NOT NULL DEFAULT TRUE,
                            expires_at TIMESTAMPTZ,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_short_urls_created_at
    ON short_urls(created_at DESC);