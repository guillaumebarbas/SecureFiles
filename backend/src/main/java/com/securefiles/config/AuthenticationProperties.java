package com.securefiles.config;

import java.time.Duration;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "securefiles.auth")
public record AuthenticationProperties(
        Duration tokenLifetime,
        String jwtSecret,
        String cookieName,
        boolean secureCookie,
        boolean allowEphemeralKey,
        Set<String> registrationRoles) {

    public AuthenticationProperties {
        tokenLifetime = tokenLifetime == null ? Duration.ofDays(30) : tokenLifetime;
        jwtSecret = jwtSecret == null ? "" : jwtSecret;
        cookieName = cookieName == null || cookieName.isBlank() ? "SECUREFILES_AUTH" : cookieName;
        registrationRoles = registrationRoles == null || registrationRoles.isEmpty()
                ? Set.of("utilisateur")
                : Set.copyOf(registrationRoles);
        if (tokenLifetime.isZero() || tokenLifetime.isNegative()) {
            throw new IllegalArgumentException("tokenLifetime must be positive");
        }
        if (tokenLifetime.compareTo(Duration.ofDays(30)) != 0) {
            throw new IllegalArgumentException("tokenLifetime must be exactly 30 days");
        }
    }
}