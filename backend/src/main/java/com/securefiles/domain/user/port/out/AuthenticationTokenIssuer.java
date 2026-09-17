package com.securefiles.domain.user.port.out;

import com.securefiles.domain.user.model.AuthenticationSession;
import com.securefiles.domain.user.model.User;

public interface AuthenticationTokenIssuer {

    String issue(User user, AuthenticationSession session);
}