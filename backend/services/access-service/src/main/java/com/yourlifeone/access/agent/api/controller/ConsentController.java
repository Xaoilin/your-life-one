package com.yourlifeone.access.agent.api.controller;

import com.yourlifeone.access.agent.api.response.ConsentDetails;
import com.yourlifeone.access.agent.application.AgentConsent;
import com.yourlifeone.access.config.AccessProperties;
import com.yourlifeone.access.identity.domain.AccountPrincipal;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/** Spring owns the pending request and final consent POST; React retains the existing product consent screen. */
@RestController
@RequiredArgsConstructor
public class ConsentController {
    private final AgentConsent consent;
    private final AccessProperties access;

    @GetMapping("/oauth2/consent")
    public ResponseEntity<Void> page(@RequestParam String state, @AuthenticationPrincipal AccountPrincipal user) {
        consent.details(state, user);
        URI page = UriComponentsBuilder.fromUri(access.browserUrl().resolve("oauth/consent"))
                .queryParam("authorization_id", state).build().encode().toUri();
        return ResponseEntity.status(303).cacheControl(CacheControl.noStore()).location(page).build();
    }

    @GetMapping("/consent/{state}")
    public ResponseEntity<ConsentDetails> details(@PathVariable String state, @AuthenticationPrincipal AccountPrincipal user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(consent.details(state, user));
    }
}
