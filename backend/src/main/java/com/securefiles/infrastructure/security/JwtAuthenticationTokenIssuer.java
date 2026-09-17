package com.securefiles.infrastructure.security;

import com.securefiles.domain.user.model.AuthenticationSession;
import com.securefiles.domain.user.model.User;
import com.securefiles.domain.user.port.out.AuthenticationTokenIssuer;
import java.util.Objects;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

public final class JwtAuthenticationTokenIssuer implements AuthenticationTokenIssuer {

    private final JwtEncoder jwtEncoder;

    public JwtAuthenticationTokenIssuer(JwtEncoder jwtEncoder) {
        this.jwtEncoder = Objects.requireNonNull(jwtEncoder, "jwtEncoder must not be null");
    }

    @Override
    public String issue(User user, AuthenticationSession session) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(session, "session must not be null");
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .id(session.id().toString())
                .issuedAt(session.issuedAt())
                .expiresAt(session.expiresAt())
                .claim("roles", user.roles().stream().map(role -> role.value()).sorted().toList())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),
            claims)).getTokenValue();
    }
}