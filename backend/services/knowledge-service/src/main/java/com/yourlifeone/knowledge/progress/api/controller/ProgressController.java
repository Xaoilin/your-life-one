package com.yourlifeone.knowledge.progress.api.controller;

import com.yourlifeone.commonservice.security.AuthenticatedUser;
import com.yourlifeone.knowledge.progress.api.mapper.ProgressMapper;
import com.yourlifeone.knowledge.progress.api.response.ProgressResponse;
import com.yourlifeone.knowledge.progress.application.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The user's knowledge and lifestyle counts, which the planner service reads (forwarding the user's token)
 * for its badges. App tokens only: agents are refused, as on every path without an approval table.
 */
@RestController
@RequestMapping("/api/knowledge/v1/progress")
@RequiredArgsConstructor
public class ProgressController {
    private final ProgressService progress;
    private final ProgressMapper mapper;

    @GetMapping
    public ProgressResponse progress(@AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(progress.progress(AuthenticatedUser.id(jwt)));
    }
}
