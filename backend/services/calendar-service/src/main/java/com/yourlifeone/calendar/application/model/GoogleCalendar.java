package com.yourlifeone.calendar.application.model;

import lombok.Builder;

/** One entry of a Google account's calendar list. */
@Builder
public record GoogleCalendar(
        String id, String summary, String backgroundColor, boolean primary, String accessRole, Boolean selected) {
}
