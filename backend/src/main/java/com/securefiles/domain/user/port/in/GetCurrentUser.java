package com.securefiles.domain.user.port.in;

public interface GetCurrentUser {

    UserProfileResult get(GetCurrentUserCommand command);
}