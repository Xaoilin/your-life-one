package com.yourlifeone.planner.tasks.api.command;

import com.yourlifeone.planner.gamification.domain.KnowledgeProgress;
import com.yourlifeone.planner.gamification.domain.PrayerOutcome;
import com.yourlifeone.planner.tasks.api.request.RolloverRequest;
import java.util.List;

/**
 * Starts the user's day, as queued.
 *
 * @param prayerOutcomes every prayer the prayer service recorded as prayed, fetched with the user's token;
 *     null when they are unknown (the service's own daily reset has no token, or the prayer service did not
 *     answer), and then prayer rewards are left as they are
 * @param knowledge the knowledge counts for badges; null when unknown
 */
public record RolloverCommand(RolloverRequest request, List<PrayerOutcome> prayerOutcomes, KnowledgeProgress knowledge) {
}
