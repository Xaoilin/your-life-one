package com.yourlifeone.knowledge.knowledge.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.knowledge.knowledge.api.command.KnowledgeCommandHandlers;
import com.yourlifeone.knowledge.knowledge.api.command.SaveEntryCommand;
import com.yourlifeone.knowledge.knowledge.api.command.SaveTopicCommand;
import com.yourlifeone.knowledge.knowledge.api.mapper.KnowledgeMapper;
import com.yourlifeone.knowledge.knowledge.api.request.EntryRequest;
import com.yourlifeone.knowledge.knowledge.api.request.TopicRequest;
import com.yourlifeone.knowledge.knowledge.api.response.EntryResponse;
import com.yourlifeone.knowledge.knowledge.api.response.KnowledgeResponse;
import com.yourlifeone.knowledge.knowledge.api.response.TopicResponse;
import com.yourlifeone.knowledge.knowledge.application.service.KnowledgeQueries;
import com.yourlifeone.knowledge.shared.IdCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The knowledge base. IDs are chosen by the app, so a retried save never duplicates a record. */
@RestController
@RequestMapping("/api/knowledge/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {
    private final KnowledgeQueries queries;
    private final KnowledgeMapper mapper;
    private final WriteCommandGateway commands;

    @GetMapping
    public KnowledgeResponse knowledgeBase(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(queries.knowledgeBase(AuthenticatedUser.id(jwt)));
    }

    @PutMapping("/topics/{id}")
    public ResponseEntity<TopicResponse> saveTopic(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody TopicRequest request) {
        return WriteCommandResponses.of(commands.send(KnowledgeCommandHandlers.SAVE_TOPIC, AuthenticatedUser.id(jwt),
                idempotencyKey, new SaveTopicCommand(id, request), TopicResponse.class));
    }

    /** Deletes the topic and every entry in it. */
    @DeleteMapping("/topics/{id}")
    public ResponseEntity<Void> deleteTopic(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(KnowledgeCommandHandlers.DELETE_TOPIC, AuthenticatedUser.id(jwt),
                idempotencyKey, new IdCommand(id), Void.class));
    }

    @PutMapping("/entries/{id}")
    public ResponseEntity<EntryResponse> saveEntry(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody EntryRequest request) {
        return WriteCommandResponses.of(commands.send(KnowledgeCommandHandlers.SAVE_ENTRY, AuthenticatedUser.id(jwt),
                idempotencyKey, new SaveEntryCommand(id, request), EntryResponse.class));
    }

    @DeleteMapping("/entries/{id}")
    public ResponseEntity<Void> deleteEntry(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey) {
        return WriteCommandResponses.of(commands.send(KnowledgeCommandHandlers.DELETE_ENTRY, AuthenticatedUser.id(jwt),
                idempotencyKey, new IdCommand(id), Void.class));
    }
}
