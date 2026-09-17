package com.securefiles.infrastructure.mapper;

import com.securefiles.domain.user.model.User;
import com.securefiles.infrastructure.entity.UserEntity;
import java.util.Objects;

public final class UserEntityMapper {

    public UserEntity toEntity(User user) {
        Objects.requireNonNull(user, "user must not be null");
        return new UserEntity(
                user.id(),
                user.name(),
                user.normalizedName(),
                user.passwordHash(),
                user.roles(),
                user.createdAt(),
                user.updatedAt());
    }

    public User toDomain(UserEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        return User.restore(
                entity.getId(),
                entity.getName(),
                entity.getNormalizedName(),
                entity.getPasswordHash(),
                entity.getRoles(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}