package com.yourlifeone.live.api.controller;

import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.live.application.service.LiveStreams;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The signed-in user's live-update stream (server-sent events): {@code change} events name what changed so
 * the tab reloads it, and prayer reminders arrive as {@code prayer.reminder} changes. The app reads it with
 * its Supabase token in the Authorization header.
 */
@RestController
@RequiredArgsConstructor
public class LiveEventsController {
    private final LiveStreams streams;

    @GetMapping(path = "/api/live/v1/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@AuthenticationPrincipal Jwt jwt) {
        return streams.open(AuthenticatedUser.id(jwt), jwt.getExpiresAt());
    }
}
