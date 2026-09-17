package com.securefiles.application.dto;

import java.util.Set;

public record CreateUserRequestDto(String name, String password, Set<String> roles) {
}