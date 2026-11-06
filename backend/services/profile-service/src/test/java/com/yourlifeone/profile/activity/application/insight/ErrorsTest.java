package com.yourlifeone.profile.activity.application.insight;

import com.yourlifeone.profile.activity.domain.ErrorSummary;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorsTest {
    private final Errors errors = new Errors(InsightTestProperties.DEFAULTS);

    @Test
    void listsTheMostFrequentFirstThenByCodeAndSurface() {
        var ranked = errors.rank(List.of(
                new ErrorSummary("timeout", 2, "calendar"),
                new ErrorSummary("render_failed", 5, "dashboard"),
                new ErrorSummary("auth_expired", 2, "profile"),
                new ErrorSummary("auth_expired", 2, "application")));

        assertEquals(List.of(
                new ErrorSummary("render_failed", 5, "dashboard"),
                new ErrorSummary("auth_expired", 2, "application"),
                new ErrorSummary("auth_expired", 2, "profile"),
                new ErrorSummary("timeout", 2, "calendar")), ranked);
    }

    @Test
    void keepsTheTopEight() {
        var many = IntStream.range(0, 12).mapToObj(index -> new ErrorSummary("code_" + index, index, "tasks")).toList();

        assertEquals(InsightTestProperties.MAX_ERRORS, errors.rank(many).size());
        assertEquals("code_11", errors.rank(many).getFirst().code());
    }
}
