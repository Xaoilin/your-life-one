package com.yourlifeone.planner.tasks.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.planner.shared.CommandHandler;
import com.yourlifeone.planner.shared.IdCommand;
import com.yourlifeone.planner.shared.Zones;
import com.yourlifeone.planner.tasks.api.mapper.TasksMapper;
import com.yourlifeone.planner.tasks.api.request.UnlinkProjectRequest;
import com.yourlifeone.planner.tasks.application.service.RolloverService;
import com.yourlifeone.planner.tasks.application.service.TaskCompletionService;
import com.yourlifeone.planner.tasks.application.service.TaskService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The task writes the command queue performs. */
@Configuration(proxyBeanMethods = false)
public class TaskCommandHandlers {
    public static final String SAVE_TASK = "tasks.save-task";
    public static final String COMPLETE_TASK = "tasks.complete-task";
    public static final String REOPEN_TASK = "tasks.reopen-task";
    public static final String DELETE_TASK = "tasks.delete-task";
    public static final String UNLINK_PROJECT = "tasks.unlink-project";
    public static final String ROLLOVER = "tasks.rollover";

    @Bean
    WriteCommandHandler<SaveTaskCommand> saveTaskHandler(TaskService tasks, TasksMapper mapper) {
        return CommandHandler.ok(SAVE_TASK, SaveTaskCommand.class, (user, command) -> mapper.toResponse(
                tasks.save(user, mapper.toDomain(command.id(), command.task()))));
    }

    @Bean
    WriteCommandHandler<CompleteTaskCommand> completeTaskHandler(TaskCompletionService completions, TasksMapper mapper) {
        return CommandHandler.ok(COMPLETE_TASK, CompleteTaskCommand.class, (user, command) -> mapper.toResponse(
                completions.complete(user, command.id(), Zones.require("timeZone", command.completion().timeZone()),
                        command.knowledge())));
    }

    @Bean
    WriteCommandHandler<IdCommand> reopenTaskHandler(TaskService tasks, TasksMapper mapper) {
        return CommandHandler.ok(REOPEN_TASK, IdCommand.class, (user, command) -> mapper.toResponse(
                tasks.reopen(user, command.id())));
    }

    @Bean
    WriteCommandHandler<IdCommand> deleteTaskHandler(TaskService tasks) {
        return CommandHandler.noContent(DELETE_TASK, IdCommand.class, (user, command) -> {
            tasks.delete(user, command.id());
            return null;
        });
    }

    @Bean
    WriteCommandHandler<UnlinkProjectRequest> unlinkProjectHandler(TaskService tasks, TasksMapper mapper) {
        return CommandHandler.ok(UNLINK_PROJECT, UnlinkProjectRequest.class, (user, request) -> mapper.toTasksResponse(
                tasks.unlinkProject(user, request.projectId())));
    }

    @Bean
    WriteCommandHandler<RolloverCommand> rolloverHandler(RolloverService rollover, TasksMapper mapper) {
        return CommandHandler.ok(ROLLOVER, RolloverCommand.class, (user, command) -> mapper.toResponse(
                rollover.rollover(user, Zones.require("appTimeZone", command.request().appTimeZone()),
                        Zones.optional("prayerTimeZone", command.request().prayerTimeZone()), command.prayerOutcomes(),
                        command.knowledge())));
    }
}
