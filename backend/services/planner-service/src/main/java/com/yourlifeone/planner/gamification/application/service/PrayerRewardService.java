package com.yourlifeone.planner.gamification.application.service;

import com.yourlifeone.planner.gamification.domain.KnowledgeProgress;
import com.yourlifeone.planner.gamification.domain.Levels;
import com.yourlifeone.planner.gamification.domain.PrayerOutcome;
import com.yourlifeone.planner.gamification.domain.PrayerRewardSync;
import com.yourlifeone.planner.gamification.domain.Profile;
import com.yourlifeone.planner.gamification.domain.Reward;
import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.PrayerRewardEntity;
import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.PrayerRewardId;
import com.yourlifeone.planner.gamification.infrastructure.persistence.repository.PrayerRewardRepository;
import com.yourlifeone.planner.shared.Timestamps;
import com.yourlifeone.planner.tasks.application.service.DayZoneService;
import com.yourlifeone.planner.tasks.application.service.TaskService;
import com.yourlifeone.planner.tasks.domain.Task;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Brings prayer XP in line with what the prayer service recorded: every prayer it recorded as prayed earns
 * XP exactly once (the receipt's primary key guarantees it), and a reward granted here is taken back if
 * that prayer's outcome is undone. The app never says how much a prayer is worth; it only asks for this.
 */
@Service
@RequiredArgsConstructor
public class PrayerRewardService {
    private static final String PRAYER = "prayer";
    private static final String DEFAULT_PRIORITY = "medium";
    private static final String GRANTED_HERE = "planner";

    private final PrayerRewardRepository receipts;
    private final GamificationService gamification;
    private final CompletionFactsFactory facts;
    private final TaskService tasks;
    private final DayZoneService zones;
    private final Clock clock;

    /**
     * Grants and takes back prayer rewards until they match the prayer service, then reports the reward for
     * one prayer (granted now or before) for the app's celebration.
     *
     * @param date the prayer the app just recorded, or null
     * @param prayer its name, or null
     * @param rewarded every prayer the prayer service recorded as prayed (fetched with the user's token)
     * @param knowledge the knowledge counts for badges; null when unknown (those badges wait)
     */
    @Transactional
    public PrayerRewardSync sync(UUID userId, LocalDate date, String prayer, List<PrayerOutcome> rewarded,
            KnowledgeProgress knowledge) {
        var profile = gamification.lock(userId);
        var recorded = rewarded.stream()
                .collect(Collectors.toMap(outcome -> new PrayerRewardId(userId, outcome.date(), outcome.prayer()),
                        Function.identity(), (first, second) -> first));
        var held = receipts.findByUserId(userId).stream()
                .collect(Collectors.toMap(receipt -> new PrayerRewardId(userId, receipt.getPrayerDate(),
                        receipt.getPrayerName()), Function.identity()));
        var changedTasks = new HashMap<String, Task>();
        for (var receipt : held.values()) {
            if (GRANTED_HERE.equals(receipt.getSource()) && !recorded.containsKey(key(userId, receipt))) {
                profile = takeBack(userId, profile, receipt, changedTasks);
            }
        }
        for (var outcome : recorded.values().stream().sorted((left, right) ->
                left.recordedAt().compareTo(right.recordedAt())).toList()) {
            if (!held.containsKey(new PrayerRewardId(userId, outcome.date(), outcome.prayer()))) {
                profile = grant(userId, profile, outcome, knowledge, changedTasks);
            }
        }
        gamification.save(userId, profile);
        return focus(userId, date, prayer, changedTasks);
    }

    private Profile grant(UUID userId, Profile profile, PrayerOutcome outcome, KnowledgeProgress knowledge,
            Map<String, Task> changedTasks) {
        var dayZones = zones.zones(userId);
        var prayerZone = dayZones.prayer() != null ? dayZones.prayer() : dayZones.app();
        var before = tasks.list(userId);
        var matching = before.stream().filter(task -> task.isPrayer() && outcome.prayer().equals(task.prayerName()))
                .toList();
        var task = matching.stream().filter(candidate -> candidate.id().equals(outcome.taskId())).findFirst()
                .or(() -> matching.stream().findFirst());
        var logId = task.map(Task::id).orElse(outcome.taskId() != null ? outcome.taskId() : canonicalLogId(outcome.prayer()));
        var today = LocalDate.ofInstant(clock.instant(), prayerZone);
        var completesTask = task.isPresent() && !task.get().completed() && outcome.date().equals(today);

        var dayOfCompletion = LocalDate.ofInstant(outcome.recordedAt(), prayerZone);
        var counted = facts.facts(before, task.map(Task::id).orElse(null), dayOfCompletion, outcome.recordedAt(),
                dayZones.app(), profile, knowledge);
        gamification.unlog(userId, outcome.date(), relevantLogIds(outcome, matching));
        var rewarded = gamification.reward(userId, profile, task.map(Task::priority).orElse(DEFAULT_PRIORITY), PRAYER,
                LocalDate.ofInstant(outcome.recordedAt(), dayZones.app()), counted, logId, outcome.date());
        if (completesTask) {
            var completed = tasks.store(userId, task.get().toBuilder()
                    .completed(true)
                    .completedAt(outcome.recordedAt())
                    .completedLocalDate(outcome.date())
                    .completionTimeZone(prayerZone.getId())
                    .lastReset(outcome.date())
                    .updatedAt(Timestamps.now(clock))
                    .build());
            changedTasks.put(outcome.prayer(), completed);
        }
        receipts.save(receipt(userId, outcome, task.map(Task::id).orElse(null), logId, rewarded.reward(), completesTask));
        return rewarded.profile();
    }

    private Profile takeBack(UUID userId, Profile profile, PrayerRewardEntity receipt, Map<String, Task> changedTasks) {
        var xp = receipt.getXpEarned();
        var after = profile;
        if (xp > 0) {
            var totalXp = Math.max(0, profile.totalXp() - xp);
            after = profile.toBuilder().totalXp(totalXp)
                    .totalTasksCompleted(Math.max(0, profile.totalTasksCompleted() - 1)).build();
            gamification.adjustTally(userId, receipt.getLogId(), -1);
        }
        gamification.unlog(userId, receipt.getPrayerDate(), List.of(receipt.getLogId()));
        if (receipt.isCompletedTask() && receipt.getTaskId() != null) {
            reopenPrayerTask(userId, receipt, changedTasks);
        }
        receipts.delete(receipt);
        return after;
    }

    /** Reopens the prayer habit this reward completed, if it still shows that completion. */
    private void reopenPrayerTask(UUID userId, PrayerRewardEntity receipt, Map<String, Task> changedTasks) {
        tasks.list(userId).stream()
                .filter(task -> task.id().equals(receipt.getTaskId()) && task.completed()
                        && receipt.getPrayerDate().equals(task.completedLocalDate()))
                .findFirst()
                .ifPresent(task -> changedTasks.put(receipt.getPrayerName(),
                        tasks.store(userId, task.reopened(Timestamps.now(clock)))));
    }

    private PrayerRewardSync focus(UUID userId, LocalDate date, String prayer, Map<String, Task> changedTasks) {
        var profile = gamification.profile(userId);
        if (date == null || prayer == null) return new PrayerRewardSync(null, null, profile);
        var reward = receipts.findById(new PrayerRewardId(userId, date, prayer))
                .filter(receipt -> receipt.getXpEarned() > 0)
                .map(PrayerRewardService::toReward)
                .orElse(null);
        return new PrayerRewardSync(reward, changedTasks.get(prayer), profile);
    }

    private PrayerRewardEntity receipt(UUID userId, PrayerOutcome outcome, String taskId, String logId, Reward reward,
            boolean completedTask) {
        return PrayerRewardEntity.builder()
                .userId(userId)
                .prayerDate(outcome.date())
                .prayerName(outcome.prayer())
                .source(GRANTED_HERE)
                .taskId(taskId)
                .logId(logId)
                .xpEarned(reward.xpEarned())
                .newBadges(reward.newBadges())
                .level(reward.level())
                .leveledUp(reward.leveledUp())
                .currentStreak(reward.currentStreak())
                .streakMilestone(reward.streakMilestone())
                .completedTask(completedTask)
                .recordedAt(outcome.recordedAt())
                .grantedAt(Timestamps.now(clock))
                .build();
    }

    private static Reward toReward(PrayerRewardEntity receipt) {
        return Reward.builder()
                .xpEarned(receipt.getXpEarned())
                .level(receipt.getLevel())
                .leveledUp(receipt.isLeveledUp())
                .title(Levels.titleForLevel(receipt.getLevel()))
                .newBadges(receipt.getNewBadges() == null ? List.of() : receipt.getNewBadges())
                .currentStreak(receipt.getCurrentStreak())
                .streakMilestone(receipt.isStreakMilestone())
                .build();
    }

    /** Every ID an earlier log entry for this prayer may have used; the new entry replaces them. */
    private static List<String> relevantLogIds(PrayerOutcome outcome, List<Task> matching) {
        var ids = new HashSet<String>();
        ids.add(canonicalLogId(outcome.prayer()));
        matching.forEach(task -> ids.add(task.id()));
        if (outcome.taskId() != null) ids.add(outcome.taskId());
        return new ArrayList<>(ids);
    }

    /** The app's log ID for a prayer with no habit, e.g. {@code prayer:fajr}. */
    private static String canonicalLogId(String prayer) {
        return PRAYER + ":" + prayer.toLowerCase(Locale.ROOT);
    }

    private static PrayerRewardId key(UUID userId, PrayerRewardEntity receipt) {
        return new PrayerRewardId(userId, receipt.getPrayerDate(), receipt.getPrayerName());
    }
}
