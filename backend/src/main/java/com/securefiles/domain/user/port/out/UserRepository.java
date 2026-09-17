package com.securefiles.domain.user.port.out;

import com.securefiles.domain.user.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    boolean existsByNormalizedName(String normalizedName);

    void save(User user);

    Optional<User> findById(UUID userId);

    Optional<User> findByNormalizedName(String normalizedName);
}