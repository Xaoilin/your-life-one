package com.yourlifeone.planner.gamification.api.command;

import com.yourlifeone.planner.gamification.domain.KnowledgeProgress;
import com.yourlifeone.planner.gamification.domain.PrayerOutcome;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

/**
 * Brings prayer XP in line with the prayer service, as queued: the API fetched the rewarded outcomes and the
 * knowledge counts in the request, so the consumer never reads another service's tables.
 *
 * @param date the prayer the app just recorded, whose reward the response reports; may be null
 * @param prayer its name; may be null
 * @param outcomes every prayer the prayer service recorded as prayed
 * @param knowledge the knowledge counts for badges; null when the knowledge service did not answer
 */
@Builder
public record SyncPrayerRewardsCommand(LocalDate date, String prayer, List<PrayerOutcome> outcomes,
        KnowledgeProgress knowledge) {
}
