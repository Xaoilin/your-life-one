package com.yourlifeone.access.agent.api.controller;

import com.yourlifeone.access.agent.api.request.ApproveClientRequest;
import com.yourlifeone.access.agent.api.response.ClientApproval;
import com.yourlifeone.access.agent.application.DomainApprovals;
import com.yourlifeone.access.identity.domain.AccountPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Browser session and CSRF protect approval changes; bearer agent tokens cannot grant themselves access. */
@RestController
@RequestMapping("/agents/{domain}/clients")
@RequiredArgsConstructor
public class DomainApprovalController {
    private final DomainApprovals approvals;

    @GetMapping
    public ResponseEntity<List<ClientApproval>> list(@AuthenticationPrincipal AccountPrincipal user, @PathVariable String domain) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(approvals.list(domain, user.userId()));
    }

    @PostMapping
    public ClientApproval approve(@AuthenticationPrincipal AccountPrincipal user, @PathVariable String domain,
            @RequestBody ApproveClientRequest request) {
        return approvals.approve(domain, user.userId(), request);
    }

    @DeleteMapping("/{clientId}")
    public ClientApproval revoke(@AuthenticationPrincipal AccountPrincipal user, @PathVariable String domain,
            @PathVariable String clientId) {
        return approvals.revoke(domain, user.userId(), clientId);
    }
}
