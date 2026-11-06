package com.yourlifeone.prayer.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Validates date ranges accepted by the API. */
final class DateRanges {
    static final int MAX_DAYS = 366;

    private DateRanges() {
    }

    static void require(LocalDate from, LocalDate to) {
        boolean backwards = from.isAfter(to);
        boolean tooLong = ChronoUnit.DAYS.between(from, to) >= MAX_DAYS;
        if (backwards || tooLong) {
            throw ApiException.badRequest("invalid_range",
                    "from must not be after to, and the range is at most " + MAX_DAYS + " days.");
        }
    }
}
