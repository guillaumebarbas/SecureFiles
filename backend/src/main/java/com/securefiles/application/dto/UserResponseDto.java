package com.securefiles.application.dto;

import java.util.List;
import java.util.UUID;

public record UserResponseDto(UUID userId, String name, List<String> roles) {
}