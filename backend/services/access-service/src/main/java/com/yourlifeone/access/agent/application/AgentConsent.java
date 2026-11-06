package com.yourlifeone.access.agent.application;

import com.yourlifeone.access.agent.api.response.ConsentClient;
import com.yourlifeone.access.agent.api.response.ConsentDetails;
import com.yourlifeone.access.identity.api.response.SessionUser;
import com.yourlifeone.access.identity.domain.AccountPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AgentConsent {
    private final OAuth2AuthorizationService authorizations;
    private final RegisteredClientRepository clients;

    public ConsentDetails details(String state, AccountPrincipal user) {
        var authorization = authorizations.findByToken(state, new OAuth2TokenType("state"));
        if (authorization == null || !user.getName().equals(authorization.getPrincipalName())) throw unavailable();
        OAuth2AuthorizationRequest request = authorization.getAttribute(OAuth2AuthorizationRequest.class.getName());
        var client = clients.findById(authorization.getRegisteredClientId());
        if (request == null || client == null) throw unavailable();
        return ConsentDetails.builder().authorizationId(state).redirectUri(request.getRedirectUri())
                .scope(String.join(" ", request.getScopes())).client(new ConsentClient(client.getClientId(), client.getClientName()))
                .user(new SessionUser(user.userId(), user.email())).build();
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "This authorization request is unavailable or expired.");
    }
}
