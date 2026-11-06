package com.yourlifeone.prayer.api.response;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record ScheduleResponse(
        LocalDate date,
        String hijriDate,
        String city,
        String country,
        String timezone,
        String method,
        List<TimingResponse> times,
        List<PrayerWindowResponse> windows) {
}
