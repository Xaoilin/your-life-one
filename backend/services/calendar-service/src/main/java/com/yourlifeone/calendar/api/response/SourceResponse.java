package com.yourlifeone.calendar.api.response;

public record SourceResponse(
        String id, String accountId, String name, String color, boolean visible, String googleCalendarId,
        String accessRole, boolean writable) {
}
