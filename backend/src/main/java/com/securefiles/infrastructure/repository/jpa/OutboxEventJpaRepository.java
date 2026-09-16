package com.securefiles.infrastructure.repository.jpa;

import com.securefiles.infrastructure.entity.OutboxEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findTop100ByPublishedAtIsNullOrderByOccurredAtAsc();
}
