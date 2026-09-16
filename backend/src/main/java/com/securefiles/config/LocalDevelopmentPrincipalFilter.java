package com.securefiles.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Objects;
import org.springframework.web.filter.OncePerRequestFilter;

public final class LocalDevelopmentPrincipalFilter extends OncePerRequestFilter {

    private final Principal principal;

    public LocalDevelopmentPrincipalFilter(String ownerId) {
        this.principal = new LocalDevelopmentPrincipal(Objects.requireNonNull(ownerId, "ownerId must not be null"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (request.getUserPrincipal() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(new LocalDevelopmentPrincipalRequest(request, principal), response);
    }
}