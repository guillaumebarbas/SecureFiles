package com.securefiles.domain.user.port.in;

import com.securefiles.domain.user.model.User;
import com.securefiles.domain.user.model.UserRole;
import java.util.Set;
import java.util.UUID;

public record UserProfileResult(UUID userId, String name, Set<UserRole> roles) {

    public UserProfileResult(User user) {
        this(user.id(), user.name(), user.roles());
    }

    public UserProfileResult {
        if (userId == null || name == null || roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("user profile values are incomplete");
        }
        roles = Set.copyOf(roles);
    }
}