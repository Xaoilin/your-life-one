package com.yourlifeone.calendar.api.response;

import java.util.List;

/** Everything the calendar page shows: every account and calendar, and the events in the range. */
public record CalendarResponse(List<AccountResponse> accounts, List<SourceResponse> sources, List<EventResponse> events) {
}
