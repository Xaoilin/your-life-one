package com.yourlifeone.access.agent.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yourlifeone.access.identity.api.response.SessionUser;
import lombok.Builder;

@Builder
public record ConsentDetails(@JsonProperty("authorization_id") String authorizationId,
        @JsonProperty("redirect_uri") String redirectUri, String scope, ConsentClient client, SessionUser user) { }
