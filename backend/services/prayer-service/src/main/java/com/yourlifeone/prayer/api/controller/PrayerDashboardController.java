package com.yourlifeone.prayer.api.controller;

import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.prayer.api.mapper.PrayerResponseMapper;
import com.yourlifeone.prayer.api.mapper.ScheduleResponseMapper;
import com.yourlifeone.prayer.api.response.DashboardResponse;
import com.yourlifeone.prayer.api.response.ScheduleResponse;
import com.yourlifeone.prayer.api.response.StatsResponse;
import com.yourlifeone.prayer.application.service.DashboardService;
import com.yourlifeone.prayer.application.service.ScheduleService;
import com.yourlifeone.prayer.application.service.StatsService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Prayer reads for the landing page. City and country come from the user's global settings. */
@RestController
@RequestMapping("/api/prayer/v1")
@RequiredArgsConstructor
public class PrayerDashboardController {
    private final DashboardService dashboards;
    private final ScheduleService schedules;
    private final StatsService stats;
    private final PrayerResponseMapper responses;
    private final ScheduleResponseMapper scheduleResponses;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @AuthenticationPrincipal Jwt jwt, @RequestParam String city, @RequestParam String country) {
        return responses.toResponse(dashboards.dashboard(AuthenticatedUser.id(jwt), city, country));
    }

    /** Timetable for a date (default: today in the location's time zone). */
    @GetMapping("/schedule")
    public ScheduleResponse schedule(
            @RequestParam String city,
            @RequestParam String country,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var schedule = date == null ? schedules.today(city, country) : schedules.forDate(city, country, date);
        return scheduleResponses.toResponse(schedule);
    }

    @GetMapping("/stats")
    public StatsResponse stats(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return responses.toResponse(stats.stats(AuthenticatedUser.id(jwt), from, to));
    }
}
