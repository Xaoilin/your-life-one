package com.yourlifeone.prayer.api.response;

import com.yourlifeone.prayer.domain.Pillar;
import com.yourlifeone.prayer.domain.PrayerName;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record MomentumReminderResponse(Pillar pillar, boolean enabled, List<PrayerName> afterPrayers,
        LocalDate completedOn) {
}
