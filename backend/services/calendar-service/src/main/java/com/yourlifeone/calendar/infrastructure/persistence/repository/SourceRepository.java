package com.yourlifeone.calendar.infrastructure.persistence.repository;

import com.yourlifeone.calendar.infrastructure.persistence.entity.SourceEntity;
import com.yourlifeone.calendar.infrastructure.persistence.entity.UserScopedId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepository extends JpaRepository<SourceEntity, UserScopedId> {
    List<SourceEntity> findByUserIdOrderByCreatedAtAscIdAsc(UUID userId);

    List<SourceEntity> findByUserIdAndAccountId(UUID userId, String accountId);

    Optional<SourceEntity> findByUserIdAndId(UUID userId, String id);
}
