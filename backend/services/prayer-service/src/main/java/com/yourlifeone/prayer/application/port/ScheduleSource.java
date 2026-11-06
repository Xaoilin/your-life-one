package com.yourlifeone.prayer.application.port;

import com.yourlifeone.prayer.domain.ScheduleDay;
import java.time.LocalDate;

/** Outbound port for the external prayer-times provider. */
public interface ScheduleSource {
    /**
     * @return an unsaved timetable for the location's calendar date
     * @throws com.yourlifeone.commonservice.web.ApiException 400 for an unknown location, 503 when unavailable
     */
    ScheduleDay fetch(String city, String country, LocalDate date);
}
