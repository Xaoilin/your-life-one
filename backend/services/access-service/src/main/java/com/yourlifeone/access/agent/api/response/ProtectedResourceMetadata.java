package com.yourlifeone.access.agent.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import lombok.Builder;

@Builder
public record ProtectedResourceMetadata(URI resource,
        @JsonProperty("authorization_servers") List<URI> authorizationServers,
        @JsonProperty("scopes_supported") List<String> scopesSupported,
        @JsonProperty("bearer_methods_supported") List<String> bearerMethodsSupported) { }
