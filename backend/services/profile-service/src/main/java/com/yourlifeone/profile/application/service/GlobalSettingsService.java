package com.yourlifeone.profile.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.profile.config.ProfileProperties;
import com.yourlifeone.profile.domain.GlobalSettings;
import com.yourlifeone.profile.infrastructure.persistence.entity.GlobalSettingsEntity;
import com.yourlifeone.profile.infrastructure.persistence.repository.GlobalSettingsRepository;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GlobalSettingsService {
    private final GlobalSettingsRepository settings;
    private final ProfileProperties properties;
    private final Clock clock;

    /** The user's settings, or the configured defaults until they save some. */
    public GlobalSettings get(UUID userId) {
        return settings.findById(userId).map(GlobalSettingsService::toDomain).orElseGet(this::defaults);
    }

    public GlobalSettings update(UUID userId, String city, String country, String timeZone) {
        var entity = GlobalSettingsEntity.builder()
                .userId(userId)
                .city(city.trim())
                .country(country.trim())
                .timeZone(timeZone == null || timeZone.isBlank() ? null : requireRegionZone(timeZone.trim()))
                .updatedAt(clock.instant())
                .build();
        return toDomain(settings.save(entity));
    }

    private GlobalSettings defaults() {
        return GlobalSettings.builder().city(properties.defaultCity()).country(properties.defaultCountry()).build();
    }

    /** Accepts only named IANA regions such as Europe/London, not fixed offsets. */
    static String requireRegionZone(String timeZone) {
        try {
            var zone = ZoneId.of(timeZone);
            boolean namedRegion = !(zone instanceof ZoneOffset) && ZoneId.getAvailableZoneIds().contains(zone.getId());
            if (namedRegion) return zone.getId();
        } catch (DateTimeException exception) {
            // Reported below as a bounded validation error.
        }
        throw ApiException.badRequest("invalid_time_zone", "timeZone must be an IANA time zone such as Europe/London.");
    }

    private static GlobalSettings toDomain(GlobalSettingsEntity entity) {
        return GlobalSettings.builder()
                .city(entity.getCity())
                .country(entity.getCountry())
                .timeZone(entity.getTimeZone())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
