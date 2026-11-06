package com.yourlifeone.calendar.infrastructure.google.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleEventsResponse(List<GoogleEventResource> items, String nextPageToken) {
}
