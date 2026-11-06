package com.yourlifeone.calendar.infrastructure.google.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/** The fields Your Life One sets when creating or changing a Google event. */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleEventBody(
        String summary, String description, String location, GoogleEventTime start, GoogleEventTime end) {
}
