package com.yourlifeone.prayer.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A cached provider timetable for one location, calculation method and date. */
@Entity
@Table(schema = "prayer", name = "schedule_day")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDayEntity {
    @Id
    private UUID id;
    private String city;
    private String country;
    private int method;
    private LocalDate scheduleDate;
    private String timeZone;
    private String hijriDate;
    private LocalTime fajr;
    private LocalTime sunrise;
    private LocalTime dhuhr;
    private LocalTime asr;
    private LocalTime sunset;
    private LocalTime maghrib;
    private LocalTime isha;
    private LocalTime midnight;
    private Instant fetchedAt;
}
