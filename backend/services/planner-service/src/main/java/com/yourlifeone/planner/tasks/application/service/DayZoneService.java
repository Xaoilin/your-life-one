package com.yourlifeone.planner.tasks.application.service;

import com.yourlifeone.planner.shared.Timestamps;
import com.yourlifeone.planner.tasks.domain.DayZones;
import com.yourlifeone.planner.tasks.infrastructure.persistence.entity.DayZoneEntity;
import com.yourlifeone.planner.tasks.infrastructure.persistence.repository.DayZoneRepository;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The time zones each user's days are counted in, as the app last reported them. */
@Service
@RequiredArgsConstructor
public class DayZoneService {
    private final DayZoneRepository zones;
    private final Clock clock;

    /** The user's zones; UTC for the app and no prayer zone until the app has reported them. */
    @Transactional(readOnly = true)
    public DayZones zones(UUID userId) {
        return zones.findById(userId).map(DayZoneService::toDomain).orElse(new DayZones(ZoneOffset.UTC, null));
    }

    /** Remembers the zones; an unknown prayer zone keeps the one reported before. */
    @Transactional
    public DayZones remember(UUID userId, ZoneId app, ZoneId prayer) {
        var existing = zones.findById(userId);
        var prayerZone = prayer != null ? prayer.getId() : existing.map(DayZoneEntity::getPrayerTimeZone).orElse(null);
        var saved = zones.save(DayZoneEntity.builder().userId(userId).appTimeZone(app.getId())
                .prayerTimeZone(prayerZone).updatedAt(Timestamps.now(clock)).build());
        return toDomain(saved);
    }

    /** Every user whose zones are known, for the daily reset. */
    @Transactional(readOnly = true)
    public Map<UUID, DayZones> all() {
        List<DayZoneEntity> rows = zones.findAll();
        return rows.stream().collect(Collectors.toMap(DayZoneEntity::getUserId, DayZoneService::toDomain));
    }

    private static DayZones toDomain(DayZoneEntity entity) {
        return new DayZones(ZoneId.of(entity.getAppTimeZone()),
                entity.getPrayerTimeZone() == null ? null : ZoneId.of(entity.getPrayerTimeZone()));
    }
}
