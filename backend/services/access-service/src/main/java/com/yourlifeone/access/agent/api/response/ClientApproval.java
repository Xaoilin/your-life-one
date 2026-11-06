package com.yourlifeone.access.agent.api.response;

import java.time.Instant;
import lombok.Builder;

@Builder
public record ClientApproval(String clientId, String clientName, Instant approvedAt, Instant revokedAt) { }
