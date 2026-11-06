package com.yourlifeone.profile.api.response;

public record ProfileDatabaseHealthResponse(String status, String service, String database) {
}
