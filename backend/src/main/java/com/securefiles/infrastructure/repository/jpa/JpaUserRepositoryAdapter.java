package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.domain.user.model.User;
import com.securefiles.domain.user.port.out.UserRepository;
import com.securefiles.infrastructure.mapper.UserEntityMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userRepository;
    private final UserEntityMapper userMapper;

    public JpaUserRepositoryAdapter(UserJpaRepository userRepository, UserEntityMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNormalizedName(String normalizedName) {
        return userRepository.existsByNormalizedName(normalizedName);
    }

    @Override
    @Transactional
    public void save(User user) {
        userRepository.save(userMapper.toEntity(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID userId) {
        return userRepository.findById(userId).map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByNormalizedName(String normalizedName) {
        return userRepository.findByNormalizedName(normalizedName).map(userMapper::toDomain);
    }
}