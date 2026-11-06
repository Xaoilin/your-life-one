package com.yourlifeone.prayer.infrastructure.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReminderId implements Serializable {
    private UUID userId;
    private LocalDate prayerDate;
    private String prayerName;
}
