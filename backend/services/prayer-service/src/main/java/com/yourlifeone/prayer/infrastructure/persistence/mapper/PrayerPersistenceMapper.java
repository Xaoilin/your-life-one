package com.yourlifeone.prayer.infrastructure.persistence.mapper;

import com.yourlifeone.prayer.domain.Outcome;
import com.yourlifeone.prayer.domain.PrayerProfile;
import com.yourlifeone.prayer.domain.ScheduleDay;
import com.yourlifeone.prayer.domain.Timing;
import com.yourlifeone.prayer.domain.Tracking;
import com.yourlifeone.prayer.infrastructure.persistence.entity.OutcomeEntity;
import com.yourlifeone.prayer.infrastructure.persistence.entity.PrayerProfileEntity;
import com.yourlifeone.prayer.infrastructure.persistence.entity.ScheduleDayEntity;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Converts between JPA entities and domain objects. */
@Mapper
public interface PrayerPersistenceMapper {
    @Mapping(target = "date", source = "prayerDate")
    @Mapping(target = "prayer", source = "prayerName")
    Outcome toDomain(OutcomeEntity entity);

    @Mapping(target = "prayerDate", source = "outcome.date")
    @Mapping(target = "prayerName", source = "outcome.prayer")
    @Mapping(target = "userId", source = "userId")
    OutcomeEntity toEntity(Outcome outcome, UUID userId);

    default PrayerProfile toDomain(PrayerProfileEntity entity) {
        var tracking = new Tracking(entity.getTrackingStartedAt(), entity.getActivationDate(),
                PrayerNameList.parse(entity.getActivationPrayers()));
        return PrayerProfile.builder()
                .enabled(entity.isEnabled())
                .reminderEnabled(entity.isReminderEnabled())
                .reminderMinutes(entity.getReminderMinutes())
                .tracking(tracking)
                .importedAt(entity.getImportedAt())
                .build();
    }

    /** Copies tracking state onto a profile row. */
    default void copyTracking(Tracking tracking, PrayerProfileEntity entity) {
        entity.setTrackingStartedAt(tracking.trackingStartedAt());
        entity.setActivationDate(tracking.activationDate());
        entity.setActivationPrayers(tracking.activationDate() == null
                ? null
                : PrayerNameList.format(tracking.activationPrayers()));
    }

    default ScheduleDay toDomain(ScheduleDayEntity entity) {
        Map<Timing, LocalTime> times = new EnumMap<>(Timing.class);
        times.put(Timing.FAJR, entity.getFajr());
        times.put(Timing.SUNRISE, entity.getSunrise());
        times.put(Timing.DHUHR, entity.getDhuhr());
        times.put(Timing.ASR, entity.getAsr());
        times.put(Timing.SUNSET, entity.getSunset());
        times.put(Timing.MAGHRIB, entity.getMaghrib());
        times.put(Timing.ISHA, entity.getIsha());
        times.put(Timing.MIDNIGHT, entity.getMidnight());
        return ScheduleDay.builder()
                .id(entity.getId())
                .city(entity.getCity())
                .country(entity.getCountry())
                .date(entity.getScheduleDate())
                .zone(ZoneId.of(entity.getTimeZone()))
                .hijriDate(entity.getHijriDate())
                .times(times)
                .fetchedAt(entity.getFetchedAt())
                .build();
    }

    default ScheduleDayEntity toEntity(ScheduleDay day, UUID id) {
        var times = day.times();
        return ScheduleDayEntity.builder()
                .id(id)
                .city(day.city())
                .country(day.country())
                .method(ScheduleDay.METHOD_ID)
                .scheduleDate(day.date())
                .timeZone(day.zone().getId())
                .hijriDate(day.hijriDate())
                .fajr(times.get(Timing.FAJR))
                .sunrise(times.get(Timing.SUNRISE))
                .dhuhr(times.get(Timing.DHUHR))
                .asr(times.get(Timing.ASR))
                .sunset(times.get(Timing.SUNSET))
                .maghrib(times.get(Timing.MAGHRIB))
                .isha(times.get(Timing.ISHA))
                .midnight(times.get(Timing.MIDNIGHT))
                .fetchedAt(day.fetchedAt())
                .build();
    }
}
