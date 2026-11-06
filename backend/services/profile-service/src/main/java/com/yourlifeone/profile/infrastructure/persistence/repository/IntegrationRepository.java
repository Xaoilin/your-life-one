package com.yourlifeone.profile.infrastructure.persistence.repository;

import com.yourlifeone.profile.infrastructure.persistence.entity.IntegrationEntity;
import com.yourlifeone.profile.infrastructure.persistence.entity.IntegrationKey;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationRepository extends JpaRepository<IntegrationEntity, IntegrationKey> {
    List<IntegrationEntity> findByUserIdOrderByProvider(UUID userId);
}
