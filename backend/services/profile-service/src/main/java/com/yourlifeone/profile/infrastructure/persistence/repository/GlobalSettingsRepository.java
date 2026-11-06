package com.yourlifeone.profile.infrastructure.persistence.repository;

import com.yourlifeone.profile.infrastructure.persistence.entity.GlobalSettingsEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalSettingsRepository extends JpaRepository<GlobalSettingsEntity, UUID> {
}
