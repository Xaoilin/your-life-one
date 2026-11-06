package com.yourlifeone.profile.activity.application.insight;

import com.yourlifeone.profile.activity.config.ActivityInsightsProperties;
import com.yourlifeone.profile.activity.domain.ErrorSummary;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** The most frequent coded errors: most frequent first; ties by code, then surface. */
@Component
@RequiredArgsConstructor
public class Errors {
    /** Surface shown for errors that name none. */
    public static final String APPLICATION = "application";

    private static final Comparator<ErrorSummary> MOST_FREQUENT_FIRST = Comparator
            .comparingLong(ErrorSummary::count).reversed()
            .thenComparing(ErrorSummary::code)
            .thenComparing(ErrorSummary::surface);

    private final ActivityInsightsProperties properties;

    public List<ErrorSummary> rank(List<ErrorSummary> errors) {
        return errors.stream().sorted(MOST_FREQUENT_FIRST).limit(properties.maxErrors()).toList();
    }
}
