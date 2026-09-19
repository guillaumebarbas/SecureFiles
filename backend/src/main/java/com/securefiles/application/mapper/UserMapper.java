package com.securefiles.application.mapper;

import com.securefiles.application.dto.AuthenticateUserRequestDto;
import com.securefiles.application.dto.CreateUserRequestDto;
import com.securefiles.application.dto.UserResponseDto;
import com.securefiles.domain.user.model.UserException;
import com.securefiles.domain.user.model.UserRole;
import com.securefiles.domain.user.port.in.AuthenticateUserCommand;
import com.securefiles.domain.user.port.in.AuthenticationResult;
import com.securefiles.domain.user.port.in.CreateUserCommand;
import com.securefiles.domain.user.port.in.CreateUserResult;
import com.securefiles.domain.user.port.in.UserProfileResult;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class UserMapper {

    public CreateUserCommand toCreateCommand(CreateUserRequestDto request) {
        Objects.requireNonNull(request, "request must not be null");
        return new CreateUserCommand(request.name(), request.password(), toRoles(request.roles()));
    }

    public AuthenticateUserCommand toAuthenticateCommand(AuthenticateUserRequestDto request) {
        Objects.requireNonNull(request, "request must not be null");
        return new AuthenticateUserCommand(request.name(), request.password());
    }

    public UserResponseDto toResponse(CreateUserResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return toResponse(new UserProfileResult(result.userId(), result.name(), result.roles()));
    }

    public UserResponseDto toResponse(AuthenticationResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return toResponse(result.user());
    }

    public UserResponseDto toResponse(UserProfileResult result) {
        Objects.requireNonNull(result, "result must not be null");
        return new UserResponseDto(
                result.userId(),
                result.name(),
                result.roles().stream()
                    .map(role -> role.value())
                        .sorted()
                        .toList());
    }

    private Set<UserRole> toRoles(Set<String> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .map(this::toRole)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private UserRole toRole(String role) {
        if (role == null) {
            throw new UserException("ROLE_NOT_ALLOWED", "One or more requested roles are not allowed.");
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new UserException("ROLE_NOT_ALLOWED", "One or more requested roles are not allowed.");
        }
    }
}