CREATE INDEX idx_short_urls_user_created_at
    ON short_urls(user_id, created_at DESC);

DROP INDEX idx_short_urls_user_id;