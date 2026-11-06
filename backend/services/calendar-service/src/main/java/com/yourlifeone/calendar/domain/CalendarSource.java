package com.yourlifeone.calendar.domain;

import java.util.Set;
import lombok.Builder;

/**
 * One calendar inside an account.
 *
 * @param googleCalendarId the mirrored Google calendar; null for local calendars
 * @param accessRole Google's access role (owner, writer, reader, freeBusyReader); null for local calendars
 */
@Builder(toBuilder = true)
public record CalendarSource(
        String id,
        String accountId,
        String name,
        String color,
        boolean visible,
        String googleCalendarId,
        String accessRole) {
    private static final Set<String> WRITABLE_ROLES = Set.of("owner", "writer");

    public boolean isGoogle() {
        return googleCalendarId != null;
    }

    /** Local calendars are always writable; Google calendars only with an owner or writer role. */
    public boolean isWritable() {
        return !isGoogle() || WRITABLE_ROLES.contains(accessRole);
    }
}
