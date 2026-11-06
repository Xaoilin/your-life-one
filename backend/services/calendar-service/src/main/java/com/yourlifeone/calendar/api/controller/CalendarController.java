package com.yourlifeone.calendar.api.controller;

import com.yourlifeone.calendar.api.mapper.CalendarResponseMapper;
import com.yourlifeone.calendar.api.response.CalendarResponse;
import com.yourlifeone.calendar.api.response.EventResponse;
import com.yourlifeone.calendar.api.response.SyncResponse;
import com.yourlifeone.calendar.application.service.AccountService;
import com.yourlifeone.calendar.application.service.CalendarSyncService;
import com.yourlifeone.calendar.application.service.EventService;
import com.yourlifeone.calendar.application.service.SourceService;
import com.yourlifeone.commonservice.security.AuthenticatedUser;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reading the calendar, and mirroring Google accounts into it. */
@RestController
@RequestMapping("/api/calendar/v1")
@RequiredArgsConstructor
public class CalendarController {
    private final AccountService accounts;
    private final SourceService sources;
    private final EventService events;
    private final CalendarSyncService sync;
    private final CalendarResponseMapper responses;

    /** Every account and calendar, and the events overlapping the dates {@code from}..{@code to}. */
    @GetMapping("/calendar")
    public CalendarResponse calendar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var userId = AuthenticatedUser.id(jwt);
        return new CalendarResponse(
                responses.toAccountResponses(accounts.list(userId)),
                responses.toSourceResponses(sources.list(userId)),
                responses.toEventResponses(events.list(userId, from, to)));
    }

    @GetMapping("/events")
    public List<EventResponse> events(@AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return responses.toEventResponses(events.list(AuthenticatedUser.id(jwt), from, to));
    }

    /**
     * Mirrors Google calendars and events now. Per-account problems (such as a credential needing
     * reconnection) are reported in the body with HTTP 200, never as an authentication failure.
     *
     * @param accountId one account to sync; every Google account when absent
     */
    @PostMapping("/sync")
    public SyncResponse sync(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String accountId) {
        return new SyncResponse(responses.toSyncResponses(
                sync.sync(AuthenticatedUser.id(jwt), accountId)));
    }
}
