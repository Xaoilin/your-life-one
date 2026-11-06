package com.yourlifeone.planner.tasks.application.service;

import com.yourlifeone.planner.gamification.application.service.GamificationService;
import com.yourlifeone.planner.gamification.application.service.PrayerRewardService;
import com.yourlifeone.planner.gamification.domain.KnowledgeProgress;
import com.yourlifeone.planner.gamification.domain.PrayerOutcome;
import com.yourlifeone.planner.shared.Timestamps;
import com.yourlifeone.planner.tasks.domain.Day;
import com.yourlifeone.planner.tasks.domain.HabitReset;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Starts a user's new day on the server: completed habits reopen, a missed streak ends, and any prayer the
 * prayer service recorded but not yet rewarded earns its XP (when the write carries the recorded prayers).
 * Safe to run any number of times a day.
 */
@Service
@RequiredArgsConstructor
public class RolloverService {
    private final TaskService tasks;
    private final GamificationService gamification;
    private final PrayerRewardService prayerRewards;
    private final DayZoneService zones;
    private final Clock clock;

    /**
     * @param app the app's time zone
     * @param prayer the prayer timetable's time zone; null while unknown (prayer habits wait)
     * @param prayerOutcomes every prayer the prayer service recorded as prayed; null when unknown, and then
     *     prayer rewards are left as they are
     * @param knowledge the knowledge counts for badges; null when unknown
     */
    @Transactional
    public Day rollover(UUID userId, ZoneId app, ZoneId prayer, List<PrayerOutcome> prayerOutcomes,
            KnowledgeProgress knowledge) {
        var dayZones = zones.remember(userId, app, prayer);
        gamification.lock(userId);
        var now = clock.instant();
        var appDate = LocalDate.ofInstant(now, dayZones.app());
        var prayerDate = dayZones.prayer() == null ? null : LocalDate.ofInstant(now, dayZones.prayer());
        for (var task : tasks.list(userId)) {
            var reopensOn = HabitReset.reopensOn(task, appDate, prayerDate);
            if (reopensOn != null) {
                tasks.store(userId, task.reopened(Timestamps.now(clock)).toBuilder().lastReset(reopensOn).build());
            }
        }
        gamification.breakStreakIfMissed(userId, appDate);
        if (prayerOutcomes != null) prayerRewards.sync(userId, null, null, prayerOutcomes, knowledge);
        return new Day(tasks.list(userId), gamification.profile(userId));
    }
}
