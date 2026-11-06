package com.yourlifeone.access.agent.api.request;

public record ApproveClientRequest(String clientId, String clientName) {
    public ApproveClientRequest {
        if (clientId == null || clientId.isBlank() || clientId.length() > 512 || !clientId.equals(clientId.trim())) {
            throw new IllegalArgumentException("A valid client ID is required.");
        }
        if (clientName == null || clientName.isBlank() || clientName.length() > 160 || !clientName.equals(clientName.trim())) {
            throw new IllegalArgumentException("A client name of at most 160 characters is required.");
        }
    }
}
