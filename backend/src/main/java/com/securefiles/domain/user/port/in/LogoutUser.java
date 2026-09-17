package com.securefiles.domain.user.port.in;

public interface LogoutUser {

    void logout(LogoutUserCommand command);
}