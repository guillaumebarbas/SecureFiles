package com.securefiles.infrastructure.security;

import com.securefiles.application.security.AuthenticatedUserPrincipal;
import com.securefiles.config.AuthenticationProperties;
import com.securefiles.domain.user.model.AuthenticationSession;
import com.securefiles.domain.user.port.out.AuthenticationSessionRepository;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public final class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final AuthenticationSessionRepository sessionRepository;
    private final AuthenticationProperties properties;
    private final Clock clock;

    public JwtAuthenticationFilter(
            JwtDecoder jwtDecoder,
            AuthenticationSessionRepository sessionRepository,
            AuthenticationProperties properties,
            Clock clock) {
        this.jwtDecoder = Objects.requireNonNull(jwtDecoder, "jwtDecoder must not be null");
        this.sessionRepository = Objects.requireNonNull(
                sessionRepository,
                "sessionRepository must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        resolveToken(request)
                .flatMap(this::authenticate)
                .ifPresent(authentication -> SecurityContextHolder.getContext().setAuthentication(authentication));
        filterChain.doFilter(request, response);
    }

    private Optional<Authentication> authenticate(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String subject = jwt.getSubject();
            String jwtId = jwt.getId();
            if (subject == null || jwtId == null) {
                return Optional.empty();
            }
            UUID userId = UUID.fromString(subject);
            UUID sessionId = UUID.fromString(jwtId);
            Optional<AuthenticationSession> session = sessionRepository.findActiveById(sessionId, clock.instant());
            if (session.isEmpty() || !session.get().userId().equals(userId)) {
                return Optional.empty();
            }
            AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(userId, sessionId);
            return Optional.of(UsernamePasswordAuthenticationToken.authenticated(
                    principal,
                    token,
                    authorities(jwt)));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private List<SimpleGrantedAuthority> authorities(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)))
                .toList();
    }

    private Optional<String> resolveToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (properties.cookieName().equals(cookie.getName()) && cookie.getValue() != null) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}