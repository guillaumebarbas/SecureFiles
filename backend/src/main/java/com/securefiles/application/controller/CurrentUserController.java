package com.securefiles.application.controller;

import com.securefiles.application.dto.UserResponseDto;
import com.securefiles.application.mapper.UserMapper;
import com.securefiles.application.security.AuthenticatedUserPrincipal;
import com.securefiles.domain.user.port.in.GetCurrentUser;
import com.securefiles.domain.user.port.in.GetCurrentUserCommand;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public final class CurrentUserController {

    private final GetCurrentUser getCurrentUser;
    private final UserMapper userMapper;

    public CurrentUserController(GetCurrentUser getCurrentUser, UserMapper userMapper) {
        this.getCurrentUser = Objects.requireNonNull(getCurrentUser, "getCurrentUser must not be null");
        this.userMapper = Objects.requireNonNull(userMapper, "userMapper must not be null");
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> get(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(userMapper.toResponse(getCurrentUser.get(new GetCurrentUserCommand(principal.userId()))));
    }
}