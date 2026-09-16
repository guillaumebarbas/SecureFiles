package com.securefiles.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.security.Principal;
import java.util.Objects;

public final class LocalDevelopmentPrincipalRequest extends HttpServletRequestWrapper {

    private final Principal principal;

    public LocalDevelopmentPrincipalRequest(HttpServletRequest request, Principal principal) {
        super(Objects.requireNonNull(request, "request must not be null"));
        this.principal = Objects.requireNonNull(principal, "principal must not be null");
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }
}