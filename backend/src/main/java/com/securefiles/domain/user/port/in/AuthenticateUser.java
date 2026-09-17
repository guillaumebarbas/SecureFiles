package com.securefiles.domain.user.port.in;

public interface AuthenticateUser {

    AuthenticationResult authenticate(AuthenticateUserCommand command);
}