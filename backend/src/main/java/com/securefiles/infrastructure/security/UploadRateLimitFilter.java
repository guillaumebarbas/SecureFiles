package com.securefiles.infrastructure.security;

import com.securefiles.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Objects;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public final class UploadRateLimitFilter extends OncePerRequestFilter {

    private static final String UPLOAD_PATH = "/api/v1/files";

    private final JdbcUploadRateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final Clock clock;

    public UploadRateLimitFilter(
            JdbcUploadRateLimiter rateLimiter,
            RateLimitProperties properties,
            Clock clock) {
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!isUploadRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            if (rateLimiter.allow(bucketKey(request), clock.instant())) {
                filterChain.doFilter(request, response);
                return;
            }
            writeError(response, HttpStatus.TOO_MANY_REQUESTS.value(), "RATE_LIMIT_EXCEEDED");
        } catch (RuntimeException exception) {
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "RATE_LIMIT_UNAVAILABLE");
        }
    }

    private boolean isUploadRequest(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod()) && UPLOAD_PATH.equals(request.getRequestURI());
    }

    private String bucketKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return "user:" + authentication.getName();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private void writeError(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setHeader("Retry-After", Long.toString(Math.max(1, properties.uploadWindow().toSeconds())));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"code\":\"" + code + "\",\"message\":\"The upload request cannot be accepted.\"}");
    }
}