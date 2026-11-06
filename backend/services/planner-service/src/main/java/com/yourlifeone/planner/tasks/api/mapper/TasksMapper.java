package com.yourlifeone.planner.tasks.api.mapper;

import com.yourlifeone.planner.gamification.api.mapper.GamificationMapper;
import com.yourlifeone.planner.tasks.api.request.TaskRequest;
import com.yourlifeone.planner.tasks.api.response.CompletionResponse;
import com.yourlifeone.planner.tasks.api.response.DayResponse;
import com.yourlifeone.planner.tasks.api.response.RecurrenceResponse;
import com.yourlifeone.planner.tasks.api.response.TaskResponse;
import com.yourlifeone.planner.tasks.api.response.TasksResponse;
import com.yourlifeone.planner.tasks.domain.Completion;
import com.yourlifeone.planner.tasks.domain.Day;
import com.yourlifeone.planner.tasks.domain.Task;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = GamificationMapper.class)
public interface TasksMapper {
    @Mapping(target = "recurring", expression = "java(recurrence(task))")
    TaskResponse toResponse(Task task);

    List<TaskResponse> toResponses(List<Task> tasks);

    CompletionResponse toResponse(Completion completion);

    DayResponse toResponse(Day day);

    default TasksResponse toTasksResponse(List<Task> tasks) {
        return new TasksResponse(toResponses(tasks));
    }

    /** A habit's recurrence with its last reset day; null for one-off tasks. */
    default RecurrenceResponse recurrence(Task task) {
        return task.recurrence() == null ? null : new RecurrenceResponse(task.recurrence(), task.lastReset());
    }

    @Mapping(target = "recurrence", source = "request.recurring.frequency")
    @Mapping(target = "completed", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "completedLocalDate", ignore = true)
    @Mapping(target = "completionTimeZone", ignore = true)
    @Mapping(target = "lastReset", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Task toDomain(String id, TaskRequest request);
}
