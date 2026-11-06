package com.yourlifeone.access.identity.api.controller;

import com.yourlifeone.access.identity.api.response.CsrfResponse;
import com.yourlifeone.access.identity.api.response.SessionResponse;
import com.yourlifeone.access.identity.application.BrowserAccessTokens;
import com.yourlifeone.access.identity.domain.AccountPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BrowserSessionController {
    private final BrowserAccessTokens tokens;

    @GetMapping("/session")
    public ResponseEntity<SessionResponse> session(Authentication authentication) {
        SessionResponse session = new SessionResponse(null, null, null);
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AccountPrincipal account) {
            session = tokens.issue(account);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(session);
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(token.getHeaderName(), token.getToken()));
    }

    @GetMapping("/signin")
    public ResponseEntity<Void> signIn() {
        return ResponseEntity.status(303).cacheControl(CacheControl.noStore())
                .header("Location", "oauth2/authorization/google").build();
    }
}
