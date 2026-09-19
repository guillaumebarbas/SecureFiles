package com.securefiles.infrastructure.security;

import com.securefiles.config.RateLimitProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUploadRateLimiter {

    private static final String UPSERT_BUCKET = """
            insert into api_rate_limit_bucket(bucket_key, window_started_millis, request_count)
            values (?, ?, 1)
            on conflict (bucket_key) do update
               set window_started_millis = case
                   when api_rate_limit_bucket.window_started_millis + ? <= ? then ?
                   else api_rate_limit_bucket.window_started_millis
                   end,
                   request_count = case
                   when api_rate_limit_bucket.window_started_millis + ? <= ? then 1
                   else api_rate_limit_bucket.request_count + 1
                   end
            returning request_count
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RateLimitProperties properties;

    public JdbcUploadRateLimiter(JdbcTemplate jdbcTemplate, RateLimitProperties properties) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public boolean allow(String bucketKey, Instant now) {
        return allow(
                bucketKey,
                now,
                properties.uploadWindow(),
                properties.uploadRequestsPerWindow());
    }

    public boolean allow(
            String bucketKey,
            Instant now,
            Duration window,
            int requestsPerWindow) {
        Objects.requireNonNull(bucketKey, "bucketKey must not be null");
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(window, "window must not be null");
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        if (requestsPerWindow < 1) {
            throw new IllegalArgumentException("requestsPerWindow must be positive");
        }
        long nowMillis = now.toEpochMilli();
        long windowMillis = window.toMillis();
        Integer requestCount = jdbcTemplate.queryForObject(
                UPSERT_BUCKET,
                Integer.class,
                bucketKey,
                nowMillis,
                windowMillis,
                nowMillis,
                nowMillis,
                windowMillis,
                nowMillis);
        return requestCount != null && requestCount <= requestsPerWindow;
    }

    public int deleteExpiredBuckets(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        long expirationMillis = now
            .minus(longestConfiguredWindow().multipliedBy(2))
                .toEpochMilli();
        return jdbcTemplate.update(
                "delete from api_rate_limit_bucket where window_started_millis < ?",
                expirationMillis);
    }

    private Duration longestConfiguredWindow() {
        if (properties.uploadWindow().compareTo(properties.loginWindow()) >= 0) {
            return properties.uploadWindow();
        }
        return properties.loginWindow();
    }
}