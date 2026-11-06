package com.yourlifeone.prayer.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Which timetable applied to a user on a date, so history keeps the location they were in. */
@Entity
@Table(schema = "prayer", name = "user_day")
@IdClass(UserDayId.class)
@Getter
@NoArgsConstructor
public class UserDayEntity {
    @Id
    private UUID userId;
    @Id
    private LocalDate day;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_day_id")
    private ScheduleDayEntity scheduleDay;
}
