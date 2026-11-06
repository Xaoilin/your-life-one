package com.yourlifeone.planner.gamification.application.service;

import com.yourlifeone.planner.gamification.domain.CompletionFacts;
import com.yourlifeone.planner.gamification.domain.Levels;
import com.yourlifeone.planner.gamification.domain.Profile;
import com.yourlifeone.planner.gamification.domain.RewardCalculator;
import com.yourlifeone.planner.gamification.domain.Rewarded;
import com.yourlifeone.planner.gamification.domain.Streaks;
import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.DailyLogEntity;
import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.DailyLogId;
import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.HabitTallyEntity;
import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.HabitTallyId;
import com.yourlifeone.planner.gamification.infrastructure.persistence.mapper.GamificationPersistenceMapper;
import com.yourlifeone.planner.gamification.infrastructure.persistence.repository.DailyLogRepository;
import com.yourlifeone.planner.gamification.infrastructure.persistence.repository.HabitTallyRepository;
import com.yourlifeone.planner.gamification.infrastructure.persistence.repository.PrayerRewardRepository;
import com.yourlifeone.planner.gamification.infrastructure.persistence.repository.ProfileRepository;
import com.yourlifeone.planner.shared.Timestamps;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * XP, level, streak and badges. Every change locks the user's profile first, so rewards arriving at once
 * are applied one after another and none is lost.
 */
@Service
@RequiredArgsConstructor
public class GamificationService {
    private final ProfileRepository profiles;
    private final HabitTallyRepository tallies;
    private final DailyLogRepository dailyLog;
    private final PrayerRewardRepository prayerRewards;
    private final GamificationPersistenceMapper persistence;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Profile profile(UUID userId) {
        return profiles.findById(userId).map(entity -> persistence.toDomain(entity, tallies(userId)))
                .orElseGet(Profile::empty);
    }

    /** The user's profile (created if missing), locked until the caller's transaction ends. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Profile lock(UUID userId) {
        profiles.insertIfAbsent(userId, Timestamps.now(clock));
        return persistence.toDomain(profiles.lockByUserId(userId).orElseThrow(), tallies(userId));
    }

    /** True when this habit was already rewarded on that day: a habit earns XP at most once a day. */
    @Transactional(readOnly = true)
    public boolean rewardedOn(UUID userId, LocalDate day, String taskId) {
        return dailyLog.existsById(new DailyLogId(userId, day, taskId));
    }

    /**
     * Rewards one completion on a locked profile and stores the result. A habit's reward is logged against
     * {@code habitLogId} on {@code logDay}, so the same habit earns nothing more that day.
     *
     * @param habitLogId null for a one-off task or goal
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Rewarded reward(UUID userId, Profile locked, String priority, String category, LocalDate streakDay,
            CompletionFacts facts, String habitLogId, LocalDate logDay) {
        var rewarded = RewardCalculator.reward(locked, priority, category, streakDay, facts);
        if (habitLogId != null) {
            adjustTally(userId, habitLogId, 1);
            log(userId, logDay, habitLogId);
        }
        save(userId, rewarded.profile());
        return rewarded;
    }

    /** Stores a locked profile's new totals (its tallies are stored separately). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(UUID userId, Profile profile) {
        var entity = persistence.toEntity(userId, profile.toBuilder().level(Levels.levelFromXp(profile.totalXp())).build());
        entity.setUpdatedAt(Timestamps.now(clock));
        profiles.save(entity);
    }

    /** Adds {@code delta} completions to a habit's tally; a tally that reaches zero is removed. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void adjustTally(UUID userId, String taskId, int delta) {
        var id = new HabitTallyId(userId, taskId);
        var count = tallies.findById(id).map(HabitTallyEntity::getCount).orElse(0) + delta;
        if (count > 0) {
            tallies.save(HabitTallyEntity.builder().userId(userId).taskId(taskId).count(count).build());
        } else {
            tallies.deleteById(id);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(UUID userId, LocalDate day, String taskId) {
        dailyLog.save(new DailyLogEntity(userId, day, taskId));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void unlog(UUID userId, LocalDate day, Collection<String> taskIds) {
        dailyLog.deleteByUserIdAndDayAndTaskIdIn(userId, day, taskIds);
    }

    /** Ends a streak whose last completion was before yesterday. Returns true when it changed anything. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean breakStreakIfMissed(UUID userId, LocalDate today) {
        var profile = lock(userId);
        if (profile.currentStreak() <= 0 || !Streaks.isBroken(profile, today)) return false;
        save(userId, profile.toBuilder().currentStreak(0).build());
        return true;
    }

    /**
     * Starts the user's progress again from nothing: XP, level, streaks, badges, tallies and the daily log.
     * Prayer reward receipts are kept as history, so no prayer already recorded earns XP again.
     */
    @Transactional
    public Profile reset(UUID userId) {
        lock(userId);
        save(userId, Profile.empty());
        tallies.deleteByUserId(userId);
        dailyLog.deleteByUserId(userId);
        prayerRewards.markHistory(userId);
        return Profile.empty();
    }

    private Map<String, Integer> tallies(UUID userId) {
        List<HabitTallyEntity> rows = tallies.findByUserId(userId);
        return rows.stream().collect(Collectors.toMap(HabitTallyEntity::getTaskId, HabitTallyEntity::getCount));
    }
}
