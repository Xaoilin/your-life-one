package com.yourlifeone.prayer.application.service;

import com.yourlifeone.prayer.domain.ScheduleDay;
import com.yourlifeone.prayer.infrastructure.persistence.entity.UserDayEntity;
import com.yourlifeone.prayer.infrastructure.persistence.mapper.PrayerPersistenceMapper;
import com.yourlifeone.prayer.infrastructure.persistence.repository.UserDayRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Remembers which timetable applied to each user on each date. */
@Service
@RequiredArgsConstructor
public class UserScheduleService {
    private final UserDayRepository userDays;
    private final PrayerPersistenceMapper mapper;

    public void remember(UUID userId, ScheduleDay day) {
        userDays.upsert(userId, day.date(), day.id());
    }

    /** Timetables known for the user between two dates, keyed and ordered by date. */
    public Map<LocalDate, ScheduleDay> knownDays(UUID userId, LocalDate from, LocalDate to) {
        var days = new TreeMap<LocalDate, ScheduleDay>();
        for (var userDay : userDays.findByUserIdAndDayBetween(userId, from, to)) {
            days.put(userDay.getDay(), toScheduleDay(userDay));
        }
        return days;
    }

    public Optional<ScheduleDay> knownDay(UUID userId, LocalDate date) {
        return Optional.ofNullable(knownDays(userId, date, date).get(date));
    }

    /** The user's most recent timetable, if any. */
    public Optional<ScheduleDay> latest(UUID userId) {
        return userDays.findFirstByUserIdOrderByDayDesc(userId).map(this::toScheduleDay);
    }

    private ScheduleDay toScheduleDay(UserDayEntity userDay) {
        return mapper.toDomain(userDay.getScheduleDay()).onDate(userDay.getDay());
    }
}
