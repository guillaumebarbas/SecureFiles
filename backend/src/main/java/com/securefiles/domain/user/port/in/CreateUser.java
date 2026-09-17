package com.securefiles.domain.user.port.in;

public interface CreateUser {

    CreateUserResult create(CreateUserCommand command);
}