package com.yourlifeone.planner.tasks.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.planner.shared.AllowedValues;
import com.yourlifeone.planner.shared.RecordIds;
import com.yourlifeone.planner.shared.Texts;
import com.yourlifeone.planner.tasks.config.TasksProperties;
import com.yourlifeone.planner.tasks.domain.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Normalises a task the app saves, the way the app shows it: a prayer habit is named after its prayer and
 * repeats daily, a daily habit repeats, one-off fields stay off habits and only board tasks keep board state.
 */
@Component
@RequiredArgsConstructor
public class TaskRules {
    private final TasksProperties properties;

    public Task normalise(Task requested) {
        RecordIds.require(requested.id());
        var category = AllowedValues.require("category", requested.category(), properties.categories());
        var prayer = Task.PRAYER.equals(category)
                ? AllowedValues.require("prayerName", requested.prayerName(), properties.prayerNames()) : null;
        var oneOff = !Task.DAILY.equals(category) && !Task.PRAYER.equals(category);
        var projectId = oneOff ? Texts.blankToNull(requested.projectId()) : null;
        var onBoard = Task.ONE_OFF.equals(category) && projectId != null;
        var title = prayer != null ? prayer + " Prayer" : Texts.trimmed(requested.title());
        if (title.isEmpty()) throw ApiException.badRequest("invalid_title", "A task needs a title.");
        return requested.toBuilder()
                .category(category)
                .priority(AllowedValues.require("priority", requested.priority(), properties.priorities()))
                .title(title)
                .description(Texts.trimmed(requested.description()))
                .prayerName(prayer)
                .recurrence(recurrence(category, requested.recurrence()))
                .dueDate(oneOff ? requested.dueDate() : null)
                .goalTag(Task.GOAL.equals(category) ? Texts.blankToNull(requested.goalTag()) : null)
                .emoji(Task.DAILY.equals(category) ? Texts.blankToNull(requested.emoji()) : null)
                .projectId(projectId)
                .workflowState(onBoard ? workflowState(requested.workflowState()) : null)
                .blockedReason(onBoard ? Texts.blankToNull(requested.blockedReason()) : null)
                .boardOrder(onBoard ? requested.boardOrder() : null)
                .build();
    }

    private String recurrence(String category, String requested) {
        if (Task.PRAYER.equals(category)) return properties.frequencies().getFirst();
        if (Task.DAILY.equals(category)) {
            return requested == null ? properties.frequencies().getFirst()
                    : AllowedValues.require("recurring.frequency", requested, properties.frequencies());
        }
        return AllowedValues.optional("recurring.frequency", requested, properties.frequencies());
    }

    private String workflowState(String requested) {
        return requested == null ? properties.workflowStates().getFirst()
                : AllowedValues.require("workflowState", requested, properties.workflowStates());
    }
}
