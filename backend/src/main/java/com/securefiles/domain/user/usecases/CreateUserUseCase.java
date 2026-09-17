package com.securefiles.domain.user.usecases;

import com.securefiles.domain.user.model.User;
import com.securefiles.domain.user.model.UserException;
import com.securefiles.domain.user.model.UserRole;
import com.securefiles.domain.user.port.in.CreateUser;
import com.securefiles.domain.user.port.in.CreateUserCommand;
import com.securefiles.domain.user.port.in.CreateUserResult;
import com.securefiles.domain.user.port.out.PasswordHasher;
import com.securefiles.domain.user.port.out.UserRepository;
import java.time.Clock;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.UUID;

public final class CreateUserUseCase implements CreateUser {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;
    private final Supplier<UUID> idGenerator;
    private final Set<UserRole> allowedRoles;

    public CreateUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            Clock clock,
            Supplier<UUID> idGenerator,
            Set<UserRole> allowedRoles) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.allowedRoles = Set.copyOf(Objects.requireNonNull(allowedRoles, "allowedRoles must not be null"));
        if (this.allowedRoles.isEmpty()) {
            throw new IllegalArgumentException("allowedRoles must not be empty");
        }
    }

    @Override
    public CreateUserResult create(CreateUserCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String normalizedName = validateName(command.name());
        validatePassword(command.password());
        validateRoles(command.roles());
        ensureNameIsAvailable(normalizedName);

        User user = createUser(command, normalizedName);
        userRepository.save(user);
        return new CreateUserResult(user.id(), user.name(), user.roles());
    }

    private String validateName(String name) {
        try {
            return User.normalizeName(name);
        } catch (IllegalArgumentException exception) {
            throw new UserException("INVALID_USER_NAME", "The user name is invalid.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 8 || password.length() > 255) {
            throw new UserException("INVALID_PASSWORD", "The password is invalid.");
        }
    }

    private void validateRoles(Set<UserRole> roles) {
        if (roles.isEmpty() || !allowedRoles.containsAll(roles)) {
            throw new UserException("ROLE_NOT_ALLOWED", "One or more requested roles are not allowed.");
        }
    }

    private void ensureNameIsAvailable(String normalizedName) {
        if (userRepository.existsByNormalizedName(normalizedName)) {
            throw new UserException("USER_NAME_ALREADY_EXISTS", "The user name is already in use.");
        }
    }

    private User createUser(CreateUserCommand command, String normalizedName) {
        UUID userId = Objects.requireNonNull(idGenerator.get(), "generated user id must not be null");
        String passwordHash = Objects.requireNonNull(
                passwordHasher.hash(command.password()),
                "password hash must not be null");
        return User.restore(
                userId,
                command.name().trim(),
                normalizedName,
                passwordHash,
                command.roles(),
                clock.instant(),
                clock.instant());
    }
}