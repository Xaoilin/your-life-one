package com.yourlifeone.planner.tasks.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.planner.gamification.application.service.CompletionFactsFactory;
import com.yourlifeone.planner.gamification.application.service.GamificationService;
import com.yourlifeone.planner.gamification.domain.KnowledgeProgress;
import com.yourlifeone.planner.gamification.domain.Reward;
import com.yourlifeone.planner.shared.Timestamps;
import com.yourlifeone.planner.tasks.domain.Completion;
import com.yourlifeone.planner.tasks.domain.Task;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Completes a task and rewards it, in one transaction: the service stamps the time and the app-time-zone
 * date, and a habit earns XP at most once a day. Prayer habits complete through the prayer service.
 */
@Service
@RequiredArgsConstructor
public class TaskCompletionService {
    private final TaskService tasks;
    private final GamificationService gamification;
    private final CompletionFactsFactory facts;
    private final Clock clock;

    /** @param knowledge the knowledge counts for badges; null when unknown (those badges wait) */
    @Transactional
    public Completion complete(UUID userId, String id, ZoneId zone, KnowledgeProgress knowledge) {
        // Locked first, so a second completion of the same task waits and then sees it done.
        var profile = gamification.lock(userId);
        var task = tasks.find(userId, id);
        if (task.isPrayer()) {
            throw ApiException.conflict("prayer_task", "Record the prayer to complete a prayer habit.");
        }
        if (task.completed()) {
            throw ApiException.conflict("task_already_completed", "This task is already done.");
        }
        var before = tasks.list(userId);
        var now = Timestamps.now(clock);
        var day = LocalDate.ofInstant(now, zone);
        var completed = tasks.store(userId, task.toBuilder()
                .completed(true)
                .completedAt(now)
                .completedLocalDate(day)
                .completionTimeZone(zone.getId())
                .lastReset(task.recurrence() == null ? task.lastReset() : day)
                .updatedAt(now)
                .build());
        Reward reward = null;
        if (!(task.isHabit() && gamification.rewardedOn(userId, day, task.id()))) {
            var counted = facts.facts(before, task.id(), day, now, zone, profile, knowledge);
            reward = gamification.reward(userId, profile, task.priority(), task.category(), day, counted,
                    task.isHabit() ? task.id() : null, day).reward();
        }
        return new Completion(completed, reward, gamification.profile(userId));
    }
}
