package com.yourlifeone.prayer.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.application.model.OutcomeChange;
import com.yourlifeone.prayer.domain.Outcome;
import com.yourlifeone.prayer.domain.OutcomeStatus;
import com.yourlifeone.prayer.domain.OutcomeTiming;
import com.yourlifeone.prayer.domain.PrayerName;
import com.yourlifeone.prayer.infrastructure.persistence.mapper.PrayerPersistenceMapper;
import com.yourlifeone.prayer.infrastructure.persistence.repository.OutcomeRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Records, corrects and removes a user's prayer outcomes. The reward (XP) itself stays in the
 * React app; {@link OutcomeChange#firstReward()} tells it when to award.
 */
@Service
@RequiredArgsConstructor
public class OutcomeService {
    private static final Comparator<Outcome> BY_DATE_THEN_PRAYER =
            Comparator.comparing(Outcome::date).thenComparing(Outcome::prayer);

    private final OutcomeRepository outcomes;
    private final PrayerPersistenceMapper mapper;
    private final PrayerProfileService profiles;
    private final OutcomeTimingPolicy timingPolicy;
    private final Clock clock;

    /** Outcomes between two dates, ordered by date and then prayer. */
    public List<Outcome> list(UUID userId, LocalDate from, LocalDate to) {
        DateRanges.require(from, to);
        return outcomes.findByUserIdAndPrayerDateBetween(userId, from, to).stream()
                .map(mapper::toDomain)
                .sorted(BY_DATE_THEN_PRAYER)
                .toList();
    }

    /** Every prayer recorded as prayed (its reward receipt is set), oldest recording first. */
    public List<Outcome> rewarded(UUID userId) {
        return outcomes.findByUserIdAndRewardedTrueOrderByRecordedAtAscPrayerDateAsc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /** Records a prayer; 409 {@code outcome_exists} if that prayer on that date is already recorded. */
    public OutcomeChange record(
            UUID userId, LocalDate date, PrayerName prayer, OutcomeStatus status, String source, String taskId) {
        return record(userId, date, prayer, status, source, taskId, null);
    }

    public OutcomeChange record(UUID userId, LocalDate date, PrayerName prayer, OutcomeStatus status,
            String source, String taskId, OutcomeTiming timing) {
        requireWritable(status);
        var resolvedTiming = resolveTiming(status, timing);
        profiles.getOrCreate(userId);
        var now = clock.instant();
        var outcome = Outcome.builder()
                .id(UUID.randomUUID())
                .date(date)
                .prayer(prayer)
                .status(status)
                .timing(resolvedTiming)
                .recordedAt(now)
                .source(source)
                .taskId(taskId)
                .rewarded(status.isCompleted())
                .deadlineAt(timingPolicy.check(userId, date, prayer, status, now))
                .build();
        if (!insertIfAbsent(userId, outcome)) {
            throw ApiException.conflict("outcome_exists",
                    prayer.label() + " on " + date + " is already recorded; correct it with PATCH instead.");
        }
        return new OutcomeChange(outcome, outcome.rewarded());
    }

    /** Changes an outcome's status. The first completion earns the reward; the receipt is never cleared. */
    public OutcomeChange correct(UUID userId, UUID outcomeId, OutcomeStatus status, String source) {
        return correct(userId, outcomeId, status, source, null);
    }

    public OutcomeChange correct(UUID userId, UUID outcomeId, OutcomeStatus status, String source, OutcomeTiming timing) {
        requireWritable(status);
        var existing = outcomes.findByIdAndUserId(outcomeId, userId).map(mapper::toDomain).orElseThrow(OutcomeService::notFound);
        var resolvedTiming = timing == null && status == existing.status()
                ? existing.timing() : resolveTiming(status, timing);
        var now = clock.instant();
        var deadlineAt = timingPolicy.check(userId, existing.date(), existing.prayer(), status, now);
        var firstReward = status.isCompleted() && !existing.rewarded();
        var corrected = existing.toBuilder()
                .status(status)
                .timing(resolvedTiming)
                .recordedAt(now)
                .source(source == null ? existing.source() : source)
                .rewarded(existing.rewarded() || firstReward)
                .deadlineAt(deadlineAt == null ? existing.deadlineAt() : deadlineAt)
                .build();
        outcomes.save(mapper.toEntity(corrected, userId));
        return new OutcomeChange(corrected, firstReward);
    }

    /** Undo: removes the outcome together with its reward receipt. */
    public void delete(UUID userId, UUID outcomeId) {
        if (outcomes.deleteByIdAndUserId(outcomeId, userId) == 0) throw notFound();
    }

    /** @return false when the user already has an outcome for that date and prayer */
    boolean insertIfAbsent(UUID userId, Outcome outcome) {
        return outcomes.insertIfAbsent(mapper.toEntity(outcome, userId)) == 1;
    }

    private static OutcomeTiming resolveTiming(OutcomeStatus status, OutcomeTiming timing) {
        if (timing != null && !timing.matches(status)) {
            throw ApiException.badRequest("invalid_timing", "timing must match the outcome status.");
        }
        return timing == null ? OutcomeTiming.defaultFor(status) : timing;
    }

    private static void requireWritable(OutcomeStatus status) {
        if (status == OutcomeStatus.UNCLASSIFIED) {
            throw ApiException.badRequest("invalid_status", "status must be on_time, late or missed.");
        }
    }

    private static ApiException notFound() {
        return ApiException.notFound("outcome_not_found", "No such outcome.");
    }
}
