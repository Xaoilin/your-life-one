package com.yourlifeone.profile.activity.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.profile.activity.domain.ActivityFilters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityFiltersResolverTest {
    private final ActivityFiltersResolver resolver =
            new ActivityFiltersResolver(ActivityTestProperties.INGEST, ActivityTestProperties.INSIGHTS);

    @Test
    void missingBlankOrAllMatchesEveryEventOverTheDefaultWindow() {
        assertEquals(ActivityFilters.builder().rangeDays(30).build(), resolver.resolve(null, "all", " ", null, "all"));
    }

    @Test
    void keepsConfiguredValues() {
        assertEquals(new ActivityFilters(7, "error", "calendar", "failure", "calendar_sync"),
                resolver.resolve(7, "error", "calendar", "failure", "calendar_sync"));
    }

    @Test
    void refusesWindowsAndValuesTheConfigurationDoesNotAllow() {
        assertEquals("invalid_range", assertThrows(ApiException.class,
                () -> resolver.resolve(14, null, null, null, null)).code());
        assertEquals("invalid_filter", assertThrows(ApiException.class,
                () -> resolver.resolve(7, "click", null, null, null)).code());
        assertEquals("invalid_filter", assertThrows(ApiException.class,
                () -> resolver.resolve(7, null, "chatroom", null, null)).code());
        assertEquals("invalid_filter", assertThrows(ApiException.class,
                () -> resolver.resolve(7, null, null, "maybe", null)).code());
        assertEquals("invalid_filter", assertThrows(ApiException.class,
                () -> resolver.resolve(7, null, null, null, "Not A Key")).code());
    }
}
