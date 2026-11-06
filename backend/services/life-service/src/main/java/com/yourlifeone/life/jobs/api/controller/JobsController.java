package com.yourlifeone.life.jobs.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.life.jobs.api.command.AddApplicationHandler;
import com.yourlifeone.life.jobs.api.command.AddHistoryCommand;
import com.yourlifeone.life.jobs.api.command.AddHistoryHandler;
import com.yourlifeone.life.jobs.api.command.RemoveApplicationCommand;
import com.yourlifeone.life.jobs.api.command.RemoveApplicationHandler;
import com.yourlifeone.life.jobs.api.command.UpdateApplicationCommand;
import com.yourlifeone.life.jobs.api.command.UpdateApplicationHandler;
import com.yourlifeone.life.jobs.api.mapper.JobsMapper;
import com.yourlifeone.life.jobs.api.request.ApplicationPatchRequest;
import com.yourlifeone.life.jobs.api.request.HistoryEntryRequest;
import com.yourlifeone.life.jobs.api.request.NewApplicationRequest;
import com.yourlifeone.life.jobs.api.response.ApplicationChangeResponse;
import com.yourlifeone.life.jobs.api.response.ApplicationDetailResponse;
import com.yourlifeone.life.jobs.api.response.ApplicationPageResponse;
import com.yourlifeone.life.jobs.api.response.JobsResponse;
import com.yourlifeone.life.jobs.application.service.JobApplicationQueries;
import com.yourlifeone.life.jobs.domain.ApplicationSearch;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The job tracker. The React app reads it whole ({@code GET /jobs}); agents search it page by page.
 * Writes change one application at a time and never touch another.
 */
@RestController
@RequestMapping("/api/life/v1/jobs")
@RequiredArgsConstructor
public class JobsController {
    private final JobApplicationQueries queries;
    private final JobsMapper mapper;
    private final WriteCommandGateway commands;

    @GetMapping
    public JobsResponse all(@AuthenticationPrincipal Jwt jwt) {
        return new JobsResponse(mapper.toResponses(queries.all(AuthenticatedUser.id(jwt))));
    }

    @GetMapping("/applications")
    public ApplicationPageResponse search(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String workType,
            @RequestParam(required = false) String remoteStatus,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        var search = ApplicationSearch.builder().query(query).status(status).workType(workType)
                .remoteStatus(remoteStatus).limit(limit).offset(offset).build();
        return mapper.toResponse(queries.search(AuthenticatedUser.id(jwt), search));
    }

    @GetMapping("/applications/{id}")
    public ApplicationDetailResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return new ApplicationDetailResponse(mapper.toResponse(queries.get(AuthenticatedUser.id(jwt), id)));
    }

    @PostMapping("/applications")
    public ResponseEntity<ApplicationChangeResponse> add(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody NewApplicationRequest request) {
        return WriteCommandResponses.of(commands.send(AddApplicationHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, request, ApplicationChangeResponse.class));
    }

    @PatchMapping("/applications/{id}")
    public ResponseEntity<ApplicationChangeResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody ApplicationPatchRequest request) {
        return WriteCommandResponses.of(commands.send(UpdateApplicationHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, new UpdateApplicationCommand(id, request), ApplicationChangeResponse.class));
    }

    @PostMapping("/applications/{id}/history")
    public ResponseEntity<ApplicationChangeResponse> addHistory(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody HistoryEntryRequest request) {
        return WriteCommandResponses.of(commands.send(AddHistoryHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, new AddHistoryCommand(id, request), ApplicationChangeResponse.class));
    }

    /** Removal must be confirmed explicitly ({@code confirm=true}); closing an application keeps its history. */
    @DeleteMapping("/applications/{id}")
    public ResponseEntity<ApplicationChangeResponse> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean confirm,
            @RequestParam(required = false) Instant expectedUpdatedAt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        if (!confirm) {
            throw ApiException.badRequest("confirmation_required", "Removing an application must be confirmed.");
        }
        return WriteCommandResponses.of(commands.send(RemoveApplicationHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, new RemoveApplicationCommand(id, expectedUpdatedAt), ApplicationChangeResponse.class));
    }
}
