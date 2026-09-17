package com.securefiles.domain.user.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.securefiles.domain.user.model.User;
import com.securefiles.domain.user.model.UserException;
import com.securefiles.domain.user.model.UserRole;
import com.securefiles.domain.user.port.in.GetCurrentUserCommand;
import com.securefiles.domain.user.port.out.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private UserRepository userRepository;

    @Test
    void get_shouldReturnPublicProfileWithoutPasswordHash_whenUserExists() {
        User user = User.restore(
                USER_ID,
                "Alice Martin",
                "alice martin",
                "$2a$hashed-password",
                Set.of(UserRole.ADMIN, UserRole.UTILISATEUR),
                Instant.parse("2026-09-17T10:00:00Z"),
                Instant.parse("2026-09-17T10:00:00Z"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        var result = new GetCurrentUserUseCase(userRepository).get(new GetCurrentUserCommand(USER_ID));

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.name()).isEqualTo("Alice Martin");
        assertThat(result.roles()).containsExactlyInAnyOrder(UserRole.ADMIN, UserRole.UTILISATEUR);
    }

    @Test
    void get_shouldRejectUnknownUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new GetCurrentUserUseCase(userRepository)
                .get(new GetCurrentUserCommand(USER_ID)))
                .isInstanceOf(UserException.class)
                .extracting(exception -> ((UserException) exception).code())
                .isEqualTo("USER_NOT_FOUND");
    }
}