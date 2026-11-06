package com.yourlifeone.life.api.controller;

import com.yourlifeone.commonservice.security.AuthenticatedUser;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Approval-only probes; the existing security filter and domain interceptor authorize every request. */
@RestController
@RequestMapping("/api/life/v1")
public class AgentAccessController {
    @GetMapping({"/inventory/access", "/jobs/access"})
    public ResponseEntity<Void> access(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser.id(jwt);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
