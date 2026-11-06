package com.yourlifeone.knowledge.projects.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.knowledge.projects.api.command.DeleteProjectCommand;
import com.yourlifeone.knowledge.projects.api.command.ProjectCommandHandlers;
import com.yourlifeone.knowledge.projects.api.command.SavePageCommand;
import com.yourlifeone.knowledge.projects.api.command.SetArchivedCommand;
import com.yourlifeone.knowledge.projects.api.command.SetPinnedCommand;
import com.yourlifeone.knowledge.projects.api.command.SetStatusCommand;
import com.yourlifeone.knowledge.projects.api.command.UpdateReferenceCommand;
import com.yourlifeone.knowledge.projects.api.mapper.ProjectsMapper;
import com.yourlifeone.knowledge.projects.api.request.ArchivedRequest;
import com.yourlifeone.knowledge.projects.api.request.NewProjectRequest;
import com.yourlifeone.knowledge.projects.api.request.PageRequest;
import com.yourlifeone.knowledge.projects.api.request.PinnedRequest;
import com.yourlifeone.knowledge.projects.api.request.ProjectReferenceRequest;
import com.yourlifeone.knowledge.projects.api.request.ProjectStatusRequest;
import com.yourlifeone.knowledge.projects.api.request.ReorderProjectsRequest;
import com.yourlifeone.knowledge.projects.api.response.PageResponse;
import com.yourlifeone.knowledge.projects.api.response.ProjectCatalogueResponse;
import com.yourlifeone.knowledge.projects.api.response.ProjectMatchesResponse;
import com.yourlifeone.knowledge.projects.application.service.ProjectQueries;
import com.yourlifeone.knowledge.shared.IdCommand;
import jakarta.validation.Valid;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The project catalogue and wiki. IDs are chosen by the app, so a retried create never duplicates a
 * record. {@code /resolve} is also open to approved Inventory agents.
 */
@RestController
@RequestMapping("/api/knowledge/v1/projects")
@RequiredArgsConstructor
public class ProjectsController {
    private final ProjectQueries queries;
    private final ProjectsMapper mapper;
    private final WriteCommandGateway commands;

    @GetMapping
    public ProjectCatalogueResponse catalogue(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(queries.catalogue(AuthenticatedUser.id(jwt)));
    }

    /** Projects whose name contains the query, whose catalogue key equals it, or with an equal tag. */
    @GetMapping("/resolve")
    public ProjectMatchesResponse resolve(@AuthenticationPrincipal Jwt jwt, @RequestParam String query) {
        return mapper.toMatchesResponse(queries.resolve(AuthenticatedUser.id(jwt), query));
    }

    @PostMapping
    public ResponseEntity<ProjectCatalogueResponse> create(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody NewProjectRequest request) {
        return send(jwt, ProjectCommandHandlers.CREATE_PROJECT, idempotencyKey, request);
    }

    @PutMapping("/{id}/reference")
    public ResponseEntity<ProjectCatalogueResponse> updateReference(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String id, @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody ProjectReferenceRequest request) {
        return send(jwt, ProjectCommandHandlers.UPDATE_REFERENCE, idempotencyKey, new UpdateReferenceCommand(id, request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ProjectCatalogueResponse> setStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody ProjectStatusRequest request) {
        return send(jwt, ProjectCommandHandlers.SET_STATUS, idempotencyKey, new SetStatusCommand(id, request));
    }

    @PutMapping("/{id}/pinned")
    public ResponseEntity<ProjectCatalogueResponse> setPinned(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @RequestBody PinnedRequest request) {
        return send(jwt, ProjectCommandHandlers.SET_PINNED, idempotencyKey, new SetPinnedCommand(id, request));
    }

    @PutMapping("/{id}/archived")
    public ResponseEntity<ProjectCatalogueResponse> setArchived(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @RequestBody ArchivedRequest request) {
        return send(jwt, ProjectCommandHandlers.SET_ARCHIVED, idempotencyKey, new SetArchivedCommand(id, request));
    }

    @PostMapping("/order")
    public ResponseEntity<ProjectCatalogueResponse> reorder(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody ReorderProjectsRequest request) {
        return send(jwt, ProjectCommandHandlers.REORDER, idempotencyKey, request);
    }

    /** Deletes one project with its wiki pages; {@code confirm=true} is required. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ProjectCatalogueResponse> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean confirm,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return send(jwt, ProjectCommandHandlers.DELETE_PROJECT, idempotencyKey, new DeleteProjectCommand(id, confirm));
    }

    @PutMapping("/pages/{id}")
    public ResponseEntity<PageResponse> savePage(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody PageRequest request) {
        return WriteCommandResponses.of(commands.send(ProjectCommandHandlers.SAVE_PAGE, AuthenticatedUser.id(jwt),
                idempotencyKey, new SavePageCommand(id, request), PageResponse.class));
    }

    @DeleteMapping("/pages/{id}")
    public ResponseEntity<Void> deletePage(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(ProjectCommandHandlers.DELETE_PAGE, AuthenticatedUser.id(jwt),
                idempotencyKey, new IdCommand(id), Void.class));
    }

    private ResponseEntity<ProjectCatalogueResponse> send(Jwt jwt, String type, String idempotencyKey, Object payload) {
        UUID user = AuthenticatedUser.id(jwt);
        return WriteCommandResponses.of(commands.send(type, user, idempotencyKey, payload, ProjectCatalogueResponse.class));
    }
}
