package com.yourlifeone.prayer.api.controller;

import com.yourlifeone.prayer.api.response.PrayerHealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prayer")
public class PrayerHealthController {
    @GetMapping("/health")
    public PrayerHealthResponse health() {
        return new PrayerHealthResponse("UP", "prayer-service");
    }
}
