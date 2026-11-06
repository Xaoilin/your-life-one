package com.yourlifeone.profile.infrastructure.persistence.repository;

import com.yourlifeone.profile.infrastructure.persistence.entity.AppPreferencesEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppPreferencesRepository extends JpaRepository<AppPreferencesEntity, UUID> {
}
