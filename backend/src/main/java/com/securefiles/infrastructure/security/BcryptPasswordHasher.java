package com.securefiles.infrastructure.security;

import com.securefiles.domain.user.port.out.PasswordHasher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public final class BcryptPasswordHasher implements PasswordHasher {

    private static final String VERSIONED_HASH_PREFIX = "{bcrypt-sha256-v1}";

    private final PasswordEncoder passwordEncoder;

    public BcryptPasswordHasher() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public String hash(String rawPassword) {
        return VERSIONED_HASH_PREFIX + passwordEncoder.encode(preHash(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        String validatedRawPassword = Objects.requireNonNull(rawPassword, "rawPassword must not be null");
        String validatedPasswordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        if (validatedPasswordHash.startsWith(VERSIONED_HASH_PREFIX)) {
            return passwordEncoder.matches(
                    preHash(validatedRawPassword),
                    validatedPasswordHash.substring(VERSIONED_HASH_PREFIX.length()));
        }
        return passwordEncoder.matches(validatedRawPassword, validatedPasswordHash);
    }

    private String preHash(String rawPassword) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(Objects.requireNonNull(rawPassword, "rawPassword must not be null")
                            .getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String toHex(byte[] digest) {
        StringBuilder hexadecimal = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            hexadecimal.append(String.format("%02x", value));
        }
        return hexadecimal.toString();
    }
}