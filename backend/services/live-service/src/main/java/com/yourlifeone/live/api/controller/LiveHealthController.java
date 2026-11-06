package com.yourlifeone.live.api.controller;

import com.yourlifeone.live.api.response.LiveHealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/live/health")
public class LiveHealthController {
    private static final String SERVICE = "live-service";

    @GetMapping
    public LiveHealthResponse health() {
        return new LiveHealthResponse("UP", SERVICE);
    }
}
