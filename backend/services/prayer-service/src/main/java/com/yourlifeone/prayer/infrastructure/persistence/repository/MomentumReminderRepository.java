package com.yourlifeone.prayer.infrastructure.persistence.repository;

import com.yourlifeone.prayer.infrastructure.persistence.entity.MomentumReminderEntity;
import com.yourlifeone.prayer.infrastructure.persistence.entity.MomentumReminderId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MomentumReminderRepository extends JpaRepository<MomentumReminderEntity, MomentumReminderId> {
    List<MomentumReminderEntity> findByUserId(UUID userId);
}
