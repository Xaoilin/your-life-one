package com.yourlifeone.prayer.application.model;

import com.yourlifeone.prayer.domain.RecordedPrayers;
import com.yourlifeone.prayer.domain.ScheduleDay;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/** Immutable decision inputs owned by one account evaluation, never retained across passes. */
public record ReminderInputs(List<ScheduleDay> timetables, RecordedPrayers recorded, ZoneId zone) {
    public ReminderInputs {
        timetables = List.copyOf(timetables);
    }

    public Optional<ScheduleDay> today() {
        return timetables.isEmpty() ? Optional.empty() : Optional.of(timetables.getLast());
    }

    /** An unusual retained receipt outside this pass's days must read its own outcome range. */
    public boolean covers(LocalDate from, LocalDate to) {
        return !timetables.isEmpty() && !from.isBefore(timetables.getFirst().date())
                && !to.isAfter(timetables.getLast().date());
    }
}
