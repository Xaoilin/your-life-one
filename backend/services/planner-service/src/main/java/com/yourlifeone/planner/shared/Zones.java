package com.yourlifeone.planner.shared;

import com.yourlifeone.commonservice.web.ApiException;
import java.time.DateTimeException;
import java.time.ZoneId;

/** IANA time zones the app sends, such as {@code Europe/London}. */
public final class Zones {
    private Zones() {
    }

    public static ZoneId require(String field, String zone) {
        if (zone == null || zone.isBlank() || zone.length() > 64) throw invalid(field);
        try {
            return ZoneId.of(zone);
        } catch (DateTimeException exception) {
            throw invalid(field);
        }
    }

    /** Null stays null. */
    public static ZoneId optional(String field, String zone) {
        return zone == null ? null : require(field, zone);
    }

    private static ApiException invalid(String field) {
        return ApiException.badRequest("invalid_time_zone", field + " must be an IANA time zone such as Europe/London.");
    }
}
