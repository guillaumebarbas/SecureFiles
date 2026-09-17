package com.securefiles.domain.user.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.securefiles.domain.user.model.User;
import com.securefiles.domain.user.model.UserException;
import com.securefiles.domain.user.model.UserRole;
import com.securefiles.domain.user.port.in.CreateUserCommand;
import com.securefiles.domain.user.port.out.PasswordHasher;
import com.securefiles.domain.user.port.out.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-09-17T10:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private CreateUserUseCase createUserUseCase;

    @BeforeEach
    void setUp() {
        createUserUseCase = new CreateUserUseCase(
                userRepository,
                passwordHasher,
                Clock.fixed(CREATED_AT, ZoneOffset.UTC),
                () -> USER_ID,
                Set.of(UserRole.UTILISATEUR));
    }

    @Test
    void create_shouldPersistNormalizedNameAndHashedPassword_whenRequestIsValid() {
        when(userRepository.existsByNormalizedName("alice martin")).thenReturn(false);
        when(passwordHasher.hash("mot-de-passe")).thenReturn("$2a$hashed-password");

        var result = createUserUseCase.create(new CreateUserCommand(
                " Alice Martin ",
                "mot-de-passe",
                Set.of(UserRole.UTILISATEUR)));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.name()).isEqualTo("Alice Martin");
        assertThat(result.roles()).containsExactly(UserRole.UTILISATEUR);
        assertThat(savedUser.name()).isEqualTo("Alice Martin");
        assertThat(savedUser.normalizedName()).isEqualTo("alice martin");
        assertThat(savedUser.passwordHash()).isEqualTo("$2a$hashed-password");
        assertThat(savedUser.roles()).containsExactly(UserRole.UTILISATEUR);
        assertThat(savedUser.createdAt()).isEqualTo(CREATED_AT);
        verify(passwordHasher).hash("mot-de-passe");
    }

    @Test
    void create_shouldRejectDuplicateNameBeforeHashing_whenNameAlreadyExists() {
        when(userRepository.existsByNormalizedName("alice martin")).thenReturn(true);

        assertThatThrownBy(() -> createUserUseCase.create(new CreateUserCommand(
                "Alice Martin",
                "mot-de-passe",
                Set.of(UserRole.UTILISATEUR))))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).code())
                .isEqualTo("USER_NAME_ALREADY_EXISTS");

        verify(passwordHasher, never()).hash(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_shouldRejectPrivilegedRoleForPublicRegistration() {
        assertThatThrownBy(() -> createUserUseCase.create(new CreateUserCommand(
                "Alice Martin",
                "mot-de-passe",
                Set.of(UserRole.UTILISATEUR, UserRole.ADMIN))))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).code())
                .isEqualTo("ROLE_NOT_ALLOWED");

        verifyNoInteractions(userRepository, passwordHasher);
    }
}