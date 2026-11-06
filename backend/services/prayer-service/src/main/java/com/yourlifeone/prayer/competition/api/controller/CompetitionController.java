package com.yourlifeone.prayer.competition.api.controller;

import com.yourlifeone.commonservice.ratelimit.RateLimitExceededException;
import com.yourlifeone.commonservice.ratelimit.UserRateLimiter;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.prayer.competition.api.response.CompetitionResponse;
import com.yourlifeone.prayer.competition.api.response.CompetitionSummary;
import com.yourlifeone.prayer.competition.application.CompetitionService;
import com.yourlifeone.prayer.competition.config.CompetitionProperties;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prayer/v1/competition")
@RequiredArgsConstructor
public class CompetitionController {
    private final CompetitionService competition;
    private final CompetitionProperties properties;
    private final UserRateLimiter limiter;

    @GetMapping
    public ResponseEntity<CompetitionResponse> comparison(@AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var clientId = jwt.getClaimAsString("client_id");
        if (clientId != null && !clientId.isBlank()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "agent_not_approved", "Prayer comparison agent access is not available.");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(competition.comparison(AuthenticatedUser.id(jwt), from, to));
    }

    @GetMapping("/summary")
    public ResponseEntity<CompetitionSummary> sharedSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var decision = limiter.tryAcquire("prayer-competition-summary");
        if (!decision.allowed()) throw new RateLimitExceededException(decision.retryAfterSeconds());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(competition.summary(properties.ownerId(), from, to));
    }
}
