package com.yourlifeone.profile.api.response;

import java.util.List;

/** Every integration the user has a record for; offered providers without one are not listed. */
public record IntegrationsResponse(List<IntegrationResponse> integrations) {
}
