package com.securefiles.domain.user.model;

public enum UserRole {
    DEVELOPPEUR("developpeur"),
    ADMIN("admin"),
    UTILISATEUR("utilisateur");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}