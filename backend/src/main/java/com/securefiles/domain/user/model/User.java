package com.securefiles.domain.user.model;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class User {

    private final UUID id;
    private final String name;
    private final String normalizedName;
    private final String passwordHash;
    private final Set<UserRole> roles;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(
            UUID id,
            String name,
            String normalizedName,
            String passwordHash,
            Set<UserRole> roles,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = requireName(name);
        this.normalizedName = requireName(normalizedName);
        this.passwordHash = requireText(passwordHash, "passwordHash");
        this.roles = immutableRoles(roles);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }

    public static User create(
            UUID id,
            String name,
            String passwordHash,
            Set<UserRole> roles,
            Instant createdAt) {
        String displayName = requireName(name);
        return new User(
                id,
                displayName,
                normalizeName(displayName),
                passwordHash,
                roles,
                createdAt,
                createdAt);
    }

    public static User restore(
            UUID id,
            String name,
            String normalizedName,
            String passwordHash,
            Set<UserRole> roles,
            Instant createdAt,
            Instant updatedAt) {
        return new User(id, name, normalizedName, passwordHash, roles, createdAt, updatedAt);
    }

    public static String normalizeName(String name) {
        return requireName(name).toLowerCase(Locale.ROOT);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String normalizedName() {
        return normalizedName;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Set<UserRole> roles() {
        return roles;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static Set<UserRole> immutableRoles(Set<UserRole> roles) {
        Objects.requireNonNull(roles, "roles must not be null");
        if (roles.isEmpty() || roles.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("roles must contain at least one non-null role");
        }
        EnumSet<UserRole> copiedRoles = EnumSet.copyOf(roles);
        return Collections.unmodifiableSet(copiedRoles);
    }

    private static String requireName(String value) {
        String name = requireText(value, "name").trim();
        if (name.length() > 255 || name.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("name must be a valid text value");
        }
        return name;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}