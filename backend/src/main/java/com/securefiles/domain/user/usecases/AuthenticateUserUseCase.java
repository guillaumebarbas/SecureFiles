package com.securefiles.domain.user.usecases;

import com.securefiles.domain.user.model.AuthenticationSession;
import com.securefiles.domain.user.model.User;
import com.securefiles.domain.user.model.UserException;
import com.securefiles.domain.user.port.in.AuthenticateUser;
import com.securefiles.domain.user.port.in.AuthenticateUserCommand;
import com.securefiles.domain.user.port.in.AuthenticationResult;
import com.securefiles.domain.user.port.in.UserProfileResult;
import com.securefiles.domain.user.port.out.AuthenticationSessionRepository;
import com.securefiles.domain.user.port.out.AuthenticationTokenIssuer;
import com.securefiles.domain.user.port.out.PasswordHasher;
import com.securefiles.domain.user.port.out.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class AuthenticateUserUseCase implements AuthenticateUser {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AuthenticationSessionRepository sessionRepository;
    private final AuthenticationTokenIssuer tokenIssuer;
    private final Clock clock;
    private final Supplier<UUID> sessionIdGenerator;
    private final Duration tokenLifetime;

    public AuthenticateUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            AuthenticationSessionRepository sessionRepository,
            AuthenticationTokenIssuer tokenIssuer,
            Clock clock,
            Supplier<UUID> sessionIdGenerator,
            Duration tokenLifetime) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher must not be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.tokenIssuer = Objects.requireNonNull(tokenIssuer, "tokenIssuer must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.sessionIdGenerator = Objects.requireNonNull(sessionIdGenerator, "sessionIdGenerator must not be null");
        this.tokenLifetime = Objects.requireNonNull(tokenLifetime, "tokenLifetime must not be null");
        if (tokenLifetime.isZero() || tokenLifetime.isNegative()) {
            throw new IllegalArgumentException("tokenLifetime must be positive");
        }
    }

    @Override
    public AuthenticationResult authenticate(AuthenticateUserCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        User user = findUser(command.name());
        verifyPassword(command.password(), user);

        Instant issuedAt = clock.instant();
        AuthenticationSession session = createSession(user, issuedAt);
        sessionRepository.save(session);
        try {
            String accessToken = tokenIssuer.issue(user, session);
            return new AuthenticationResult(
                    new UserProfileResult(user),
                    accessToken,
                    session.id(),
                    session.expiresAt());
        } catch (RuntimeException exception) {
            sessionRepository.revoke(session.id(), clock.instant());
            throw exception;
        }
    }

    private User findUser(String name) {
        String normalizedName = normalizeName(name);
        Optional<User> user = userRepository.findByNormalizedName(normalizedName);
        return user.orElseThrow(this::invalidCredentials);
    }

    private void verifyPassword(String password, User user) {
        if (!passwordHasher.matches(password, user.passwordHash())) {
            throw invalidCredentials();
        }
    }

    private AuthenticationSession createSession(User user, Instant issuedAt) {
        UUID sessionId = Objects.requireNonNull(
                sessionIdGenerator.get(),
                "generated session id must not be null");
        return AuthenticationSession.start(
                sessionId,
                user.id(),
                issuedAt,
                issuedAt.plus(tokenLifetime));
    }

    private String normalizeName(String name) {
        try {
            return User.normalizeName(name);
        } catch (IllegalArgumentException exception) {
            throw invalidCredentials();
        }
    }

    private UserException invalidCredentials() {
        return new UserException("INVALID_CREDENTIALS", "The user name or password is invalid.");
    }
}