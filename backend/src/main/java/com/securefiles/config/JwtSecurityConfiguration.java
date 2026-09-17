package com.securefiles.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.securefiles.domain.user.port.out.AuthenticationTokenIssuer;
import com.securefiles.infrastructure.security.JwtAuthenticationFilter;
import com.securefiles.infrastructure.security.JwtAuthenticationTokenIssuer;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(AuthenticationProperties.class)
public class JwtSecurityConfiguration {

    @Bean
    public SecretKey jwtSecretKey(AuthenticationProperties properties) {
        byte[] secret = decodeSecret(properties);
        if (secret.length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public AuthenticationTokenIssuer authenticationTokenIssuer(JwtEncoder jwtEncoder) {
        return new JwtAuthenticationTokenIssuer(jwtEncoder);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtDecoder jwtDecoder,
            com.securefiles.domain.user.port.out.AuthenticationSessionRepository sessionRepository,
            AuthenticationProperties properties,
            java.time.Clock clock) {
        return new JwtAuthenticationFilter(jwtDecoder, sessionRepository, properties, clock);
    }

    private byte[] decodeSecret(AuthenticationProperties properties) {
        if (properties.jwtSecret().isBlank()) {
            if (!properties.allowEphemeralKey()) {
                throw new IllegalArgumentException("JWT_SECRET must be configured");
            }
            byte[] generatedSecret = new byte[32];
            new SecureRandom().nextBytes(generatedSecret);
            return generatedSecret;
        }
        try {
            return Base64.getDecoder().decode(properties.jwtSecret());
        } catch (IllegalArgumentException exception) {
            return properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        }
    }
}