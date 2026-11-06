package com.yourlifeone.knowledge.projects.api.command;

import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.knowledge.projects.api.mapper.ProjectsMapper;
import com.yourlifeone.knowledge.projects.api.request.NewProjectRequest;
import com.yourlifeone.knowledge.projects.api.request.ReorderProjectsRequest;
import com.yourlifeone.knowledge.projects.application.service.ProjectPageService;
import com.yourlifeone.knowledge.projects.application.service.ProjectService;
import com.yourlifeone.knowledge.shared.CommandHandler;
import com.yourlifeone.knowledge.shared.IdCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The project writes the command queue performs. Project writes answer with the whole catalogue, since
 * moving one project can renumber others; page writes answer with the page.
 */
@Configuration(proxyBeanMethods = false)
public class ProjectCommandHandlers {
    public static final String CREATE_PROJECT = "projects.create-project";
    public static final String UPDATE_REFERENCE = "projects.update-reference";
    public static final String SET_STATUS = "projects.set-status";
    public static final String SET_PINNED = "projects.set-pinned";
    public static final String SET_ARCHIVED = "projects.set-archived";
    public static final String REORDER = "projects.reorder";
    public static final String DELETE_PROJECT = "projects.delete-project";
    public static final String SAVE_PAGE = "projects.save-page";
    public static final String DELETE_PAGE = "projects.delete-page";

    @Bean
    WriteCommandHandler<NewProjectRequest> createProjectHandler(ProjectService projects, ProjectsMapper mapper) {
        return CommandHandler.ok(CREATE_PROJECT, NewProjectRequest.class, (user, request) -> mapper.toResponse(
                projects.create(user, mapper.toDomain(request), request.overviewPageId())));
    }

    @Bean
    WriteCommandHandler<UpdateReferenceCommand> updateProjectReferenceHandler(ProjectService projects,
            ProjectsMapper mapper) {
        return CommandHandler.ok(UPDATE_REFERENCE, UpdateReferenceCommand.class, (user, command) -> mapper.toResponse(
                projects.updateReference(user, command.id(), mapper.toDomain(command.reference()))));
    }

    @Bean
    WriteCommandHandler<SetStatusCommand> setProjectStatusHandler(ProjectService projects, ProjectsMapper mapper) {
        return CommandHandler.ok(SET_STATUS, SetStatusCommand.class, (user, command) -> mapper.toResponse(
                projects.setStatus(user, command.id(), command.status().status())));
    }

    @Bean
    WriteCommandHandler<SetPinnedCommand> setProjectPinnedHandler(ProjectService projects, ProjectsMapper mapper) {
        return CommandHandler.ok(SET_PINNED, SetPinnedCommand.class, (user, command) -> mapper.toResponse(
                projects.setPinned(user, command.id(), command.pinned().pinned())));
    }

    @Bean
    WriteCommandHandler<SetArchivedCommand> setProjectArchivedHandler(ProjectService projects, ProjectsMapper mapper) {
        return CommandHandler.ok(SET_ARCHIVED, SetArchivedCommand.class, (user, command) -> mapper.toResponse(
                projects.setArchived(user, command.id(), command.archived().archived())));
    }

    @Bean
    WriteCommandHandler<ReorderProjectsRequest> reorderProjectsHandler(ProjectService projects, ProjectsMapper mapper) {
        return CommandHandler.ok(REORDER, ReorderProjectsRequest.class, (user, request) -> mapper.toResponse(
                projects.reorder(user, request.section(), request.ids())));
    }

    @Bean
    WriteCommandHandler<DeleteProjectCommand> deleteProjectHandler(ProjectService projects, ProjectsMapper mapper) {
        return CommandHandler.ok(DELETE_PROJECT, DeleteProjectCommand.class, (user, command) -> mapper.toResponse(
                projects.delete(user, command.id(), command.confirmed())));
    }

    @Bean
    WriteCommandHandler<SavePageCommand> saveProjectPageHandler(ProjectPageService pages, ProjectsMapper mapper) {
        return CommandHandler.ok(SAVE_PAGE, SavePageCommand.class, (user, command) -> mapper.toResponse(
                pages.save(user, mapper.toDomain(command.id(), command.page()))));
    }

    @Bean
    WriteCommandHandler<IdCommand> deleteProjectPageHandler(ProjectPageService pages) {
        return CommandHandler.noContent(DELETE_PAGE, IdCommand.class, (user, command) -> {
            pages.delete(user, command.id());
            return null;
        });
    }
}
