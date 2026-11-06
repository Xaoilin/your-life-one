package com.yourlifeone.planner.tasks.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.planner.shared.RecordIds;
import com.yourlifeone.planner.shared.Timestamps;
import com.yourlifeone.planner.shared.UserScopedId;
import com.yourlifeone.planner.tasks.domain.Task;
import com.yourlifeone.planner.tasks.infrastructure.persistence.mapper.TasksPersistenceMapper;
import com.yourlifeone.planner.tasks.infrastructure.persistence.repository.TaskRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tasks, habits and goals. Every write names one task; completion is owned by the service (see
 * {@link TaskCompletionService}), so saving a task's details never changes whether it is done.
 */
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository tasks;
    private final TasksPersistenceMapper persistence;
    private final TaskRules rules;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Task> list(UUID userId) {
        return tasks.findByUserIdOrderByCreatedAtAscIdAsc(userId).stream().map(persistence::toDomain).toList();
    }

    /** Creates the task, or replaces its details keeping its completion, reset day and creation time. */
    @Transactional
    public Task save(UUID userId, Task requested) {
        var normalised = rules.normalise(requested);
        requireOneHabitPerPrayer(userId, normalised);
        var existing = tasks.findById(new UserScopedId(userId, normalised.id())).map(persistence::toDomain);
        var now = Timestamps.now(clock);
        var saved = normalised.toBuilder()
                .completed(existing.map(Task::completed).orElse(false))
                .completedAt(existing.map(Task::completedAt).orElse(null))
                .completedLocalDate(existing.map(Task::completedLocalDate).orElse(null))
                .completionTimeZone(existing.map(Task::completionTimeZone).orElse(null))
                .lastReset(normalised.recurrence() == null ? null : existing.map(Task::lastReset).orElse(null))
                .createdAt(existing.map(Task::createdAt).orElse(now))
                .updatedAt(now)
                .build();
        return store(userId, saved);
    }

    /** Reopens a completed one-off task or goal. A habit stays done for the rest of its day. */
    @Transactional
    public Task reopen(UUID userId, String id) {
        var task = find(userId, id);
        if (task.isHabit() && task.completed()) {
            throw ApiException.conflict("habit_locked", "A habit stays done until the next day.");
        }
        return store(userId, task.reopened(Timestamps.now(clock)));
    }

    @Transactional
    public void delete(UUID userId, String id) {
        var key = new UserScopedId(userId, RecordIds.require(id));
        if (!tasks.existsById(key)) throw notFound();
        tasks.deleteById(key);
    }

    /** Takes a removed project's tasks off its board, as stored (never from the caller's copy). */
    @Transactional
    public List<Task> unlinkProject(UUID userId, String projectId) {
        var now = Timestamps.now(clock);
        for (var entity : tasks.findByUserIdAndProjectId(userId, RecordIds.require(projectId))) {
            store(userId, persistence.toDomain(entity).toBuilder().projectId(null).workflowState(null)
                    .blockedReason(null).boardOrder(null).updatedAt(now).build());
        }
        return list(userId);
    }

    /** A prayer has one habit, so it is never completed or rewarded twice through two tasks. */
    private void requireOneHabitPerPrayer(UUID userId, Task task) {
        if (!task.isPrayer()) return;
        var taken = list(userId).stream().anyMatch(other -> other.isPrayer()
                && task.prayerName().equals(other.prayerName()) && !other.id().equals(task.id()));
        if (taken) {
            throw ApiException.conflict("prayer_habit_exists", task.prayerName() + " already has a prayer habit.");
        }
    }

    @Transactional(readOnly = true)
    public Task find(UUID userId, String id) {
        return tasks.findById(new UserScopedId(userId, RecordIds.require(id))).map(persistence::toDomain)
                .orElseThrow(TaskService::notFound);
    }

    @Transactional
    public Task store(UUID userId, Task task) {
        return persistence.toDomain(tasks.save(persistence.toEntity(userId, task)));
    }

    private static ApiException notFound() {
        return ApiException.notFound("task_not_found", "This task does not exist.");
    }
}
