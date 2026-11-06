package com.yourlifeone.knowledge.knowledge.infrastructure.persistence.repository;

import com.yourlifeone.knowledge.knowledge.infrastructure.persistence.entity.EntryEntity;
import com.yourlifeone.knowledge.shared.UserScopedId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryRepository extends JpaRepository<EntryEntity, UserScopedId> {
    List<EntryEntity> findByUserIdOrderByCreatedAtAscIdAsc(UUID userId);

    long countByUserId(UUID userId);
}
