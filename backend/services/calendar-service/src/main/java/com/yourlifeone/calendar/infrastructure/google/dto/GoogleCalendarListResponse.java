package com.yourlifeone.calendar.infrastructure.google.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleCalendarListResponse(List<GoogleCalendarListEntry> items, String nextPageToken) {
}
