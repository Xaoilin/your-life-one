package com.yourlifeone.finance.review.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.commonservice.web.WriteCommandResponses;
import com.yourlifeone.finance.review.api.command.ReviewCommandHandlers;
import com.yourlifeone.finance.review.api.mapper.ReviewMapper;
import com.yourlifeone.finance.review.api.request.SaveReviewRequest;
import com.yourlifeone.finance.review.api.response.ReviewEnvelopeResponse;
import com.yourlifeone.finance.review.application.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The dated banking review; AI agents approved for Finance may read and replace it. */
@RestController
@RequestMapping("/api/finance/v1/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviews;
    private final ReviewMapper mapper;
    private final WriteCommandGateway commands;

    @GetMapping
    public ReviewEnvelopeResponse review(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toEnvelope(reviews.review(AuthenticatedUser.id(jwt)));
    }

    @PutMapping
    public ResponseEntity<ReviewEnvelopeResponse> save(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IdempotencyKeys.HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody SaveReviewRequest request) {
        return WriteCommandResponses.of(commands.send(ReviewCommandHandlers.SAVE_REVIEW, AuthenticatedUser.id(jwt),
                idempotencyKey, request, ReviewEnvelopeResponse.class));
    }
}
