package com.yourlifeone.planner.gamification.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.planner.gamification.api.command.GamificationCommandHandlers;
import com.yourlifeone.planner.gamification.api.command.SyncPrayerRewardsCommand;
import com.yourlifeone.planner.gamification.api.command.ResetCommand;
import com.yourlifeone.planner.gamification.api.mapper.GamificationMapper;
import com.yourlifeone.planner.gamification.api.request.PrayerRewardSyncRequest;
import com.yourlifeone.planner.gamification.api.response.GamificationResponse;
import com.yourlifeone.planner.gamification.api.response.PrayerRewardSyncResponse;
import com.yourlifeone.planner.gamification.application.service.GamificationService;
import com.yourlifeone.planner.gamification.application.service.KnowledgeProgressSource;
import com.yourlifeone.planner.gamification.application.service.PrayerOutcomeSource;
import java.util.function.Supplier;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** XP, level, streaks and badges: worked out by the service, only shown by the app. */
@RestController
@RequestMapping("/api/planner/v1")
@RequiredArgsConstructor
public class GamificationController {
    private final GamificationService gamification;
    private final GamificationMapper mapper;
    private final WriteCommandGateway commands;
    private final PrayerOutcomeSource prayerOutcomes;
    private final KnowledgeProgressSource knowledge;

    @GetMapping("/gamification")
    public GamificationResponse profile(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(gamification.profile(AuthenticatedUser.id(jwt)));
    }

    /** Starts the user's progress again; needs {@code confirm=true}. Daily momentum is kept. */
    @PostMapping("/gamification/reset")
    public ResponseEntity<GamificationResponse> reset(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @RequestParam(defaultValue = "false") boolean confirm) {
        if (!confirm) {
            throw ApiException.badRequest("confirmation_required", "Resetting progress needs confirm=true.");
        }
        return WriteCommandResponses.of(commands.send(GamificationCommandHandlers.RESET, AuthenticatedUser.id(jwt),
                idempotencyKey, new ResetCommand(true), GamificationResponse.class));
    }

    /**
     * Brings prayer XP in line with the prayer service after the app recorded, corrected or undid a prayer,
     * and reports what that prayer earned. The rewarded prayers are fetched from the prayer service first
     * (503 {@code prayer_unavailable} when it does not answer: no reward is taken back on missing data).
     */
    @PostMapping("/rewards/prayers/sync")
    public ResponseEntity<PrayerRewardSyncResponse> syncPrayerRewards(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody PrayerRewardSyncRequest request) {
        Supplier<SyncPrayerRewardsCommand> sync = () -> SyncPrayerRewardsCommand.builder()
                .date(request.date())
                .prayer(request.prayer())
                .outcomes(prayerOutcomes.rewarded(jwt))
                .knowledge(knowledge.progressOrNull(jwt))
                .build();
        return WriteCommandResponses.of(commands.sendWithContext(GamificationCommandHandlers.SYNC_PRAYER_REWARDS,
                AuthenticatedUser.id(jwt), idempotencyKey, request, sync, PrayerRewardSyncResponse.class));
    }
}
