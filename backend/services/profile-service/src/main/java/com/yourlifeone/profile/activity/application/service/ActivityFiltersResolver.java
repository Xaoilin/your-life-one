package com.yourlifeone.profile.activity.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.profile.activity.config.ActivityIngestProperties;
import com.yourlifeone.profile.activity.config.ActivityInsightsProperties;
import com.yourlifeone.profile.activity.domain.ActivityFilters;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Turns the Activity page's query parameters into filters. A missing, blank or {@code all} value matches
 * every event; any other value must be one the configuration allows.
 */
@Component
public class ActivityFiltersResolver {
    static final String ALL = "all";

    private final ActivityIngestProperties ingest;
    private final ActivityInsightsProperties insights;
    private final Pattern taxonomy;

    public ActivityFiltersResolver(ActivityIngestProperties ingest, ActivityInsightsProperties insights) {
        this.ingest = ingest;
        this.insights = insights;
        this.taxonomy = Pattern.compile(ingest.taxonomyPattern());
    }

    public ActivityFilters resolve(Integer rangeDays, String kind, String surface, String outcome, String feature) {
        var days = rangeDays == null ? insights.defaultRangeDays() : rangeDays;
        if (!insights.rangeDays().contains(days)) {
            throw ApiException.badRequest("invalid_range", "rangeDays must be one of " + insights.rangeDays() + ".");
        }
        var featureFilter = optional(feature);
        if (featureFilter != null && !taxonomy.matcher(featureFilter).matches()) {
            throw ApiException.badRequest("invalid_filter", "feature is not a valid feature key.");
        }
        return ActivityFilters.builder()
                .rangeDays(days)
                .kind(allowed("kind", kind, ingest.kinds()))
                .surface(allowed("surface", surface, ingest.surfaces()))
                .outcome(allowed("outcome", outcome, ingest.outcomes()))
                .feature(featureFilter)
                .build();
    }

    private static String allowed(String name, String value, List<String> allowed) {
        var filter = optional(value);
        if (filter != null && !allowed.contains(filter)) {
            throw ApiException.badRequest("invalid_filter", name + " must be all or one of " + allowed + ".");
        }
        return filter;
    }

    private static String optional(String value) {
        return value == null || value.isBlank() || ALL.equals(value) ? null : value;
    }
}
