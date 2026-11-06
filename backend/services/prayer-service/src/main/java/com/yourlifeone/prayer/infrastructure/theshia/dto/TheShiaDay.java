package com.yourlifeone.prayer.infrastructure.theshia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

/** UTC instants; the provider's different midnight convention is deliberately not imported. */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record TheShiaDay(
        LocalDate date, Instant fajr, Instant sunrise, Instant dhuhr, Instant asr,
        Instant sunset, Instant maghrib, Instant isha) {
}
