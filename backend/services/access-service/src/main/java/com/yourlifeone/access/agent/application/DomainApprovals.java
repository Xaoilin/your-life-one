package com.yourlifeone.access.agent.application;

import com.yourlifeone.access.agent.api.request.ApproveClientRequest;
import com.yourlifeone.access.agent.api.response.ClientApproval;
import com.yourlifeone.access.agent.infrastructure.DomainApprovalRepository;
import com.yourlifeone.access.mcp.domain.McpDomain;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DomainApprovals {
    private final DomainApprovalRepository approvals;
    private final RegisteredClientRepository clients;

    public List<ClientApproval> list(String domain, UUID user) {
        return approvals.list(domain(domain), user);
    }

    @Transactional
    public ClientApproval approve(String domain, UUID user, ApproveClientRequest request) {
        if (clients.findByClientId(request.clientId()) == null) {
            throw new IllegalArgumentException("Register the agent with this product before approving it.");
        }
        return approvals.approve(domain(domain), user, request.clientId(), request.clientName());
    }

    @Transactional
    public ClientApproval revoke(String domain, UUID user, String clientId) {
        var area = domain(domain);
        var revoked = approvals.revoke(area, user, clientId);
        // Preserve Settings' existing Inventory behavior: also end this agent's OAuth connection.
        if (area == McpDomain.INVENTORY) {
            var client = clients.findByClientId(clientId);
            if (client != null) approvals.revokeGrant(user, client.getId());
        }
        return revoked;
    }

    private McpDomain domain(String value) {
        try {
            return McpDomain.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Unknown agent approval domain.");
        }
    }
}
