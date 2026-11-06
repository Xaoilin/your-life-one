package com.yourlifeone.profile.infrastructure.persistence.repository;

import com.yourlifeone.profile.infrastructure.persistence.entity.ViewPreferenceEntity;
import com.yourlifeone.profile.infrastructure.persistence.entity.ViewPreferenceKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ViewPreferenceRepository extends JpaRepository<ViewPreferenceEntity, ViewPreferenceKey> {
    List<ViewPreferenceEntity> findByUserIdOrderByPreferenceKey(UUID userId);

    /** Atomic first-save/update avoids the select-then-insert race between two devices. */
    @Modifying
    @Query(value = """
            INSERT INTO profile.view_preferences (user_id, preference_key, preference_value, updated_at)
            VALUES (:user, :key, :value, :updatedAt)
            ON CONFLICT (user_id, preference_key)
            DO UPDATE SET preference_value = EXCLUDED.preference_value, updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    void upsert(@Param("user") UUID user, @Param("key") String key,
            @Param("value") String value, @Param("updatedAt") Instant updatedAt);
}
