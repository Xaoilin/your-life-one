package com.yourlifeone.planner.tasks.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * A task's details; the ID is in the path. Completion is not part of it: completing and reopening are their
 * own writes. A prayer habit's title comes from its prayer, so it may be left blank.
 *
 * @param recurring how a habit repeats; its last reset day belongs to the service and is ignored
 */
public record TaskRequest(
        @Size(max = 300) String title,
        @Size(max = 4000) String description,
        @NotBlank @Size(max = 32) String priority,
        @NotBlank @Size(max = 32) String category,
        LocalDate dueDate,
        @Valid RecurrenceRequest recurring,
        @Size(max = 16) String prayerName,
        @Size(max = 64) String goalTag,
        @Size(max = 16) String emoji,
        @Size(max = 256) String projectId,
        @Size(max = 32) String workflowState,
        @Size(max = 1000) String blockedReason,
        @Min(0) Integer boardOrder) {
}
