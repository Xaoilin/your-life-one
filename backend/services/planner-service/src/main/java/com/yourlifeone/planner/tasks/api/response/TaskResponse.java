package com.yourlifeone.planner.tasks.api.response;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        String id,
        String title,
        String description,
        boolean completed,
        Instant completedAt,
        LocalDate completedLocalDate,
        String completionTimeZone,
        String priority,
        String category,
        LocalDate dueDate,
        RecurrenceResponse recurring,
        String prayerName,
        String goalTag,
        String emoji,
        String projectId,
        String workflowState,
        String blockedReason,
        Integer boardOrder,
        Instant createdAt,
        Instant updatedAt) {
}
