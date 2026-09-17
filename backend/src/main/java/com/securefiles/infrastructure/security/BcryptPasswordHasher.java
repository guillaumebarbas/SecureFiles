package com.securefiles.infrastructure.security;

import com.securefiles.domain.user.port.out.PasswordHasher;
import java.util.Objects;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public final class BcryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    public BcryptPasswordHasher() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(Objects.requireNonNull(rawPassword, "rawPassword must not be null"));
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(
                Objects.requireNonNull(rawPassword, "rawPassword must not be null"),
                Objects.requireNonNull(passwordHash, "passwordHash must not be null"));
    }
}