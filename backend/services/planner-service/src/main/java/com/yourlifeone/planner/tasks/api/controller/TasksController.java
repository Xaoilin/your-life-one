package com.yourlifeone.planner.tasks.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.planner.gamification.application.service.KnowledgeProgressSource;
import com.yourlifeone.planner.gamification.application.service.PrayerOutcomeSource;
import com.yourlifeone.planner.shared.IdCommand;
import com.yourlifeone.planner.tasks.api.command.CompleteTaskCommand;
import com.yourlifeone.planner.tasks.api.command.RolloverCommand;
import com.yourlifeone.planner.tasks.api.command.SaveTaskCommand;
import com.yourlifeone.planner.tasks.api.command.TaskCommandHandlers;
import com.yourlifeone.planner.tasks.api.mapper.TasksMapper;
import com.yourlifeone.planner.tasks.api.request.CompleteTaskRequest;
import com.yourlifeone.planner.tasks.api.request.RolloverRequest;
import com.yourlifeone.planner.tasks.api.request.TaskRequest;
import com.yourlifeone.planner.tasks.api.request.UnlinkProjectRequest;
import com.yourlifeone.planner.tasks.api.response.CompletionResponse;
import com.yourlifeone.planner.tasks.api.response.DayResponse;
import com.yourlifeone.planner.tasks.api.response.TaskResponse;
import com.yourlifeone.planner.tasks.api.response.TasksResponse;
import com.yourlifeone.planner.tasks.application.service.TaskService;
import jakarta.validation.Valid;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tasks, habits and goals. Task IDs are chosen by the app, so a retried save never duplicates a task.
 * Completing, reopening and the daily reset are the service's own writes: it stamps the time and the date,
 * and awards XP.
 */
@RestController
@RequestMapping("/api/planner/v1")
@RequiredArgsConstructor
public class TasksController {
    private final TaskService tasks;
    private final TasksMapper mapper;
    private final WriteCommandGateway commands;
    private final PrayerOutcomeSource prayerOutcomes;
    private final KnowledgeProgressSource knowledge;

    @GetMapping("/tasks")
    public TasksResponse tasks(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toTasksResponse(tasks.list(AuthenticatedUser.id(jwt)));
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> save(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody TaskRequest request) {
        return WriteCommandResponses.of(commands.send(TaskCommandHandlers.SAVE_TASK, AuthenticatedUser.id(jwt),
                idempotencyKey, new SaveTaskCommand(id, request), TaskResponse.class));
    }

    @PostMapping("/tasks/{id}/complete")
    public ResponseEntity<CompletionResponse> complete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody CompleteTaskRequest request) {
        Supplier<CompleteTaskCommand> completion = () -> new CompleteTaskCommand(id, request,
                knowledge.progressOrNull(jwt));
        return WriteCommandResponses.of(commands.sendWithContext(TaskCommandHandlers.COMPLETE_TASK,
                AuthenticatedUser.id(jwt), idempotencyKey, new CompleteTaskCommand(id, request, null), completion,
                CompletionResponse.class));
    }

    @PostMapping("/tasks/{id}/reopen")
    public ResponseEntity<TaskResponse> reopen(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(TaskCommandHandlers.REOPEN_TASK, AuthenticatedUser.id(jwt),
                idempotencyKey, new IdCommand(id), TaskResponse.class));
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(TaskCommandHandlers.DELETE_TASK, AuthenticatedUser.id(jwt),
                idempotencyKey, new IdCommand(id), Void.class));
    }

    /** Takes a removed project's tasks off its board. */
    @PostMapping("/tasks/unlink-project")
    public ResponseEntity<TasksResponse> unlinkProject(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody UnlinkProjectRequest request) {
        return WriteCommandResponses.of(commands.send(TaskCommandHandlers.UNLINK_PROJECT, AuthenticatedUser.id(jwt),
                idempotencyKey, request, TasksResponse.class));
    }

    /**
     * Starts the user's day: reopens habits, ends a missed streak and rewards any recorded prayers (fetched from
     * the prayer service first; when it does not answer, the day still starts and prayer rewards wait).
     */
    @PostMapping("/day/rollover")
    public ResponseEntity<DayResponse> rollover(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody RolloverRequest request) {
        Supplier<RolloverCommand> rollover = () -> new RolloverCommand(request, prayerOutcomes.rewardedOrNull(jwt),
                knowledge.progressOrNull(jwt));
        return WriteCommandResponses.of(commands.sendWithContext(TaskCommandHandlers.ROLLOVER, AuthenticatedUser.id(jwt),
                idempotencyKey, request, rollover, DayResponse.class));
    }
}
