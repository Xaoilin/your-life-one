package com.yourlifeone.planner.api.response;

public record PlannerDatabaseHealthResponse(String status, String service, String database) {
}
