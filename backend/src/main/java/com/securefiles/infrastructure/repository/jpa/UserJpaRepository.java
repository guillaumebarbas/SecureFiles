package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.infrastructure.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByNormalizedName(String normalizedName);

    Optional<UserEntity> findByNormalizedName(String normalizedName);
}