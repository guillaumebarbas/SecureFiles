package com.securefiles.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.securefiles.application.dto.AuthenticateUserRequestDto;
import com.securefiles.application.dto.CreateUserRequestDto;
import com.securefiles.application.dto.UserResponseDto;
import com.securefiles.domain.user.model.UserRole;
import com.securefiles.domain.user.port.in.AuthenticateUserCommand;
import com.securefiles.domain.user.port.in.CreateUserCommand;
import com.securefiles.domain.user.port.in.UserProfileResult;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserMapperTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final UserMapper mapper = new UserMapper();

    @Test
    void toCreateCommand_shouldMapRequestedRoles() {
        CreateUserRequestDto request = new CreateUserRequestDto(
                "Alice Martin",
                "mot-de-passe",
                Set.of("developpeur", "utilisateur"));

        CreateUserCommand command = mapper.toCreateCommand(request);

        assertThat(command.name()).isEqualTo("Alice Martin");
        assertThat(command.password()).isEqualTo("mot-de-passe");
        assertThat(command.roles()).containsExactlyInAnyOrder(
                UserRole.DEVELOPPEUR,
                UserRole.UTILISATEUR);
    }

    @Test
    void toAuthenticateCommand_shouldMapCredentials() {
        AuthenticateUserCommand command = mapper.toAuthenticateCommand(
                new AuthenticateUserRequestDto("Alice Martin", "mot-de-passe"));

        assertThat(command).isEqualTo(new AuthenticateUserCommand("Alice Martin", "mot-de-passe"));
    }

    @Test
    void toResponse_shouldExposeAllRolesInStableOrder() {
        UserResponseDto response = mapper.toResponse(new UserProfileResult(
                USER_ID,
                "Alice Martin",
                Set.of(UserRole.UTILISATEUR, UserRole.DEVELOPPEUR)));

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.name()).isEqualTo("Alice Martin");
        assertThat(response.roles()).containsExactly("developpeur", "utilisateur");
    }
}