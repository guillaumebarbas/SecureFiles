package com.securefiles.application.controller;

import com.securefiles.application.dto.AuthenticateUserRequestDto;
import com.securefiles.application.dto.CreateUserRequestDto;
import com.securefiles.application.dto.UserResponseDto;
import com.securefiles.application.mapper.UserMapper;
import com.securefiles.application.security.AuthenticatedUserPrincipal;
import com.securefiles.config.AuthenticationProperties;
import com.securefiles.domain.user.port.in.AuthenticateUser;
import com.securefiles.domain.user.port.in.AuthenticationResult;
import com.securefiles.domain.user.port.in.CreateUser;
import com.securefiles.domain.user.port.in.LogoutUser;
import java.time.Duration;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public final class UserAuthenticationController {

    private final CreateUser createUser;
    private final AuthenticateUser authenticateUser;
    private final LogoutUser logoutUser;
    private final UserMapper userMapper;
    private final AuthenticationProperties authenticationProperties;

    public UserAuthenticationController(
            CreateUser createUser,
            AuthenticateUser authenticateUser,
            LogoutUser logoutUser,
            UserMapper userMapper,
            AuthenticationProperties authenticationProperties) {
        this.createUser = Objects.requireNonNull(createUser, "createUser must not be null");
        this.authenticateUser = Objects.requireNonNull(authenticateUser, "authenticateUser must not be null");
        this.logoutUser = Objects.requireNonNull(logoutUser, "logoutUser must not be null");
        this.userMapper = Objects.requireNonNull(userMapper, "userMapper must not be null");
        this.authenticationProperties = Objects.requireNonNull(
                authenticationProperties,
                "authenticationProperties must not be null");
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto register(@RequestBody CreateUserRequestDto request) {
        return userMapper.toResponse(createUser.create(userMapper.toCreateCommand(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponseDto> login(@RequestBody AuthenticateUserRequestDto request) {
        AuthenticationResult result = authenticateUser.authenticate(userMapper.toAuthenticateCommand(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authenticationCookie(result.accessToken()).toString())
                .body(userMapper.toResponse(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            logoutUser.logout(new com.securefiles.domain.user.port.in.LogoutUserCommand(principal.sessionId()));
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearedAuthenticationCookie().toString())
                .build();
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie authenticationCookie(String accessToken) {
        return ResponseCookie.from(authenticationProperties.cookieName(), accessToken)
                .httpOnly(true)
                .secure(authenticationProperties.secureCookie())
                .sameSite("Lax")
                .path("/")
                .maxAge(authenticationProperties.tokenLifetime())
                .build();
    }

    private ResponseCookie clearedAuthenticationCookie() {
        return ResponseCookie.from(authenticationProperties.cookieName(), "")
                .httpOnly(true)
                .secure(authenticationProperties.secureCookie())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}