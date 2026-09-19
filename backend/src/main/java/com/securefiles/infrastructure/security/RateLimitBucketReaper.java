package com.securefiles.infrastructure.security;

import java.time.Clock;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class RateLimitBucketReaper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitBucketReaper.class);

    private final JdbcUploadRateLimiter rateLimiter;
    private final Clock clock;

    public RateLimitBucketReaper(JdbcUploadRateLimiter rateLimiter, Clock clock) {
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Scheduled(fixedDelayString = "${securefiles.rate-limit.cleanup-interval-millis:300000}")
    public void deleteExpiredBuckets() {
        try {
            rateLimiter.deleteExpiredBuckets(clock.instant());
        } catch (RuntimeException exception) {
            LOGGER.warn("Expired rate limit buckets could not be removed", exception);
        }
    }
}