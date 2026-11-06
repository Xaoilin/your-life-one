package com.yourlifeone.prayer.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.prayer.api.command.CorrectOutcomeCommand;
import com.yourlifeone.prayer.api.command.CorrectOutcomeHandler;
import com.yourlifeone.prayer.api.command.CreateOutcomeHandler;
import com.yourlifeone.prayer.api.command.DeleteOutcomeCommand;
import com.yourlifeone.prayer.api.command.DeleteOutcomeHandler;
import com.yourlifeone.prayer.api.mapper.PrayerResponseMapper;
import com.yourlifeone.prayer.api.request.CorrectOutcomeRequest;
import com.yourlifeone.prayer.api.request.CreateOutcomeRequest;
import com.yourlifeone.prayer.api.response.OutcomeChangeResponse;
import com.yourlifeone.prayer.api.response.OutcomeResponse;
import com.yourlifeone.prayer.api.response.RewardedOutcomesResponse;
import com.yourlifeone.prayer.application.service.OutcomeService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
 * A user's prayer outcomes, addressed by id. Writes go through the prayer command queue; send an
 * {@code Idempotency-Key} so a retried write is applied once and returns the same response.
 */
@RestController
@RequestMapping("/api/prayer/v1/outcomes")
@RequiredArgsConstructor
public class PrayerOutcomeController {
    private final OutcomeService outcomes;
    private final PrayerResponseMapper responses;
    private final WriteCommandGateway commands;

    @GetMapping
    public List<OutcomeResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return responses.toResponses(outcomes.list(AuthenticatedUser.id(jwt), from, to));
    }

    /**
     * Every prayer recorded as prayed (its one-time reward receipt is set), whatever its date: the planner
     * rewards XP from these, forwarding the user's token.
     */
    @GetMapping("/rewarded")
    public RewardedOutcomesResponse rewarded(@AuthenticationPrincipal Jwt jwt) {
        return responses.toRewarded(outcomes.rewarded(AuthenticatedUser.id(jwt)));
    }

    /** Records a prayer; 409 {@code outcome_exists} if that prayer on that date is already recorded. */
    @PostMapping
    public ResponseEntity<OutcomeChangeResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody CreateOutcomeRequest request) {
        return WriteCommandResponses.of(commands.send(CreateOutcomeHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, request, OutcomeChangeResponse.class));
    }

    /** Corrects an outcome's status; the reward receipt is kept. */
    @PatchMapping("/{id}")
    public ResponseEntity<OutcomeChangeResponse> correct(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @PathVariable UUID id,
            @Valid @RequestBody CorrectOutcomeRequest request) {
        var command = CorrectOutcomeCommand.builder().outcomeId(id).status(request.status())
                .source(request.source()).timing(request.timing()).build();
        return WriteCommandResponses.of(commands.send(CorrectOutcomeHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, command, OutcomeChangeResponse.class));
    }

    /** Undo: removes the outcome, including its reward receipt. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @PathVariable UUID id) {
        return WriteCommandResponses.of(commands.send(DeleteOutcomeHandler.TYPE, AuthenticatedUser.id(jwt),
                idempotencyKey, new DeleteOutcomeCommand(id), Void.class));
    }
}
