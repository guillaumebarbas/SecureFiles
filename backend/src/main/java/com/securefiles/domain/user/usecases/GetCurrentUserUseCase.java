package com.securefiles.domain.user.usecases;

import com.securefiles.domain.user.model.UserException;
import com.securefiles.domain.user.port.in.GetCurrentUser;
import com.securefiles.domain.user.port.in.GetCurrentUserCommand;
import com.securefiles.domain.user.port.in.UserProfileResult;
import com.securefiles.domain.user.port.out.UserRepository;
import java.util.Objects;

public final class GetCurrentUserUseCase implements GetCurrentUser {

    private final UserRepository userRepository;

    public GetCurrentUserUseCase(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @Override
    public UserProfileResult get(GetCurrentUserCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return userRepository.findById(command.userId())
                .map(UserProfileResult::new)
                .orElseThrow(() -> new UserException("USER_NOT_FOUND", "The user was not found."));
    }
}