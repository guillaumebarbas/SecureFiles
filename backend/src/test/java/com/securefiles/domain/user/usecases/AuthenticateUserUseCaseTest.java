package com.securefiles.domain.user.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.securefiles.domain.user.model.User;
import com.securefiles.domain.user.model.UserException;
import com.securefiles.domain.user.model.UserRole;
import com.securefiles.domain.user.port.in.AuthenticateUserCommand;
import com.securefiles.domain.user.port.out.AuthenticationSessionRepository;
import com.securefiles.domain.user.port.out.AuthenticationTokenIssuer;
import com.securefiles.domain.user.port.out.PasswordHasher;
import com.securefiles.domain.user.port.out.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESSION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant ISSUED_AT = Instant.parse("2026-09-17T10:00:00Z");
    private static final User USER = User.restore(
            USER_ID,
            "Alice Martin",
            "alice martin",
            "$2a$hashed-password",
            Set.of(UserRole.UTILISATEUR, UserRole.DEVELOPPEUR),
            ISSUED_AT,
            ISSUED_AT);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private AuthenticationSessionRepository sessionRepository;

    @Mock
    private AuthenticationTokenIssuer tokenIssuer;

    private AuthenticateUserUseCase authenticateUserUseCase;

    @BeforeEach
    void setUp() {
        authenticateUserUseCase = new AuthenticateUserUseCase(
                userRepository,
                passwordHasher,
                sessionRepository,
                tokenIssuer,
                Clock.fixed(ISSUED_AT, ZoneOffset.UTC),
                () -> SESSION_ID,
                Duration.ofDays(30));
    }

    @Test
    void authenticate_shouldIssueThirtyDayTokenAndCreateSession_whenCredentialsAreValid() {
        when(userRepository.findByNormalizedName("alice martin")).thenReturn(Optional.of(USER));
        when(passwordHasher.matches("mot-de-passe", USER.passwordHash())).thenReturn(true);
        when(tokenIssuer.issue(eq(USER), any())).thenReturn("jwt-token");

        var result = authenticateUserUseCase.authenticate(
                new AuthenticateUserCommand(" Alice Martin ", "mot-de-passe"));

        ArgumentCaptor<com.securefiles.domain.user.model.AuthenticationSession> sessionCaptor =
                ArgumentCaptor.forClass(com.securefiles.domain.user.model.AuthenticationSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        var session = sessionCaptor.getValue();

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.user().userId()).isEqualTo(USER_ID);
        assertThat(result.user().roles())
                .containsExactlyInAnyOrder(UserRole.UTILISATEUR, UserRole.DEVELOPPEUR);
        assertThat(session.id()).isEqualTo(SESSION_ID);
        assertThat(session.userId()).isEqualTo(USER_ID);
        assertThat(session.expiresAt()).isEqualTo(ISSUED_AT.plus(Duration.ofDays(30)));
        verify(tokenIssuer).issue(eq(USER), eq(session));
    }

    @Test
    void authenticate_shouldRejectWithSameCode_whenUserDoesNotExist() {
        when(userRepository.findByNormalizedName("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticateUserUseCase.authenticate(
                new AuthenticateUserCommand("unknown", "mot-de-passe")))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).code())
                .isEqualTo("INVALID_CREDENTIALS");

        verify(passwordHasher, never()).matches(any(), any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void authenticate_shouldRejectWithSameCode_whenPasswordDoesNotMatch() {
        when(userRepository.findByNormalizedName("alice martin")).thenReturn(Optional.of(USER));
        when(passwordHasher.matches("mauvais", USER.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> authenticateUserUseCase.authenticate(
                new AuthenticateUserCommand("alice martin", "mauvais")))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).code())
                .isEqualTo("INVALID_CREDENTIALS");

        verify(sessionRepository, never()).save(any());
        verify(tokenIssuer, never()).issue(any(), any());
    }
}