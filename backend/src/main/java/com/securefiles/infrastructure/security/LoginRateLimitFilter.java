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
import org.springframework.web.filter.OncePerRequestFilter;

public final class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private final JdbcUploadRateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final Clock clock;

    public LoginRateLimitFilter(
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
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            if (rateLimiter.allow(
                    bucketKey(request),
                    clock.instant(),
                    properties.loginWindow(),
                    properties.loginRequestsPerWindow())) {
                filterChain.doFilter(request, response);
                return;
            }
            writeError(response, HttpStatus.TOO_MANY_REQUESTS.value(), "LOGIN_RATE_LIMIT_EXCEEDED");
        } catch (RuntimeException exception) {
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "LOGIN_RATE_LIMIT_UNAVAILABLE");
        }
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod()) && LOGIN_PATH.equals(request.getRequestURI());
    }

    private String bucketKey(HttpServletRequest request) {
        return "login:ip:" + request.getRemoteAddr();
    }

    private void writeError(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setHeader("Retry-After", Long.toString(Math.max(1, properties.loginWindow().toSeconds())));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"code\":\"" + code + "\",\"message\":\"The login request cannot be accepted.\"}");
    }
}