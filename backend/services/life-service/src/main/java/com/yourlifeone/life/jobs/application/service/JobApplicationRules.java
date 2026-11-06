package com.yourlifeone.life.jobs.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.life.jobs.config.JobsProperties;
import com.yourlifeone.life.jobs.domain.HistoryEntry;
import com.yourlifeone.life.jobs.domain.JobApplication;
import com.yourlifeone.life.shared.AllowedValues;
import com.yourlifeone.life.shared.RecordIds;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The job tracker's rules: defaults for a new application, allowed values, evidence links, and that
 * remote eligibility is only confirmed for a known region.
 */
@Component
@RequiredArgsConstructor
public class JobApplicationRules {
    private static final Pattern WEB_LINK = Pattern.compile("^https?://[^\\s/]+\\S*$");
    private static final Pattern CREDENTIALS_IN_LINK = Pattern.compile("^https?://[^/]*@.*");
    private static final String UNKNOWN = "unknown";
    private static final String CONFIRMED = "confirmed";

    private final JobsProperties properties;

    /** Fills a new application's defaults, then checks it. Timestamps are left to the caller. */
    public JobApplication withDefaults(JobApplication requested) {
        var history = requested.history() == null ? List.<HistoryEntry>of() : requested.history();
        var application = requested.toBuilder()
                .id(requested.id() == null ? UUID.randomUUID().toString() : requested.id())
                .company(requested.company().trim())
                .role(requested.role().trim())
                .url(blankToNull(requested.url()))
                .workType(orDefault(requested.workType(), UNKNOWN))
                .remoteRegion(orDefault(requested.remoteRegion(), UNKNOWN))
                .remoteStatus(orDefault(requested.remoteStatus(), "needs_verification"))
                .remoteEvidence(orDefault(blankToNull(requested.remoteEvidence()), properties.defaultRemoteEvidence()))
                .remoteCaveat(blankToNull(requested.remoteCaveat()))
                .compensation(blankToNull(requested.compensation()))
                .status(orDefault(requested.status(), "lead"))
                .nextAction(orDefault(blankToNull(requested.nextAction()), properties.defaultNextAction()))
                .notes(requested.notes() == null ? "" : requested.notes())
                .history(history.stream().map(this::entryWithDefaults).toList())
                .build();
        requireValid(application);
        return application;
    }

    public HistoryEntry entryWithDefaults(HistoryEntry entry) {
        var filled = entry.toBuilder()
                .id(entry.id() == null || entry.id().isBlank() ? UUID.randomUUID().toString() : entry.id().trim())
                .summary(entry.summary().trim())
                .details(entry.details() == null ? "" : entry.details())
                .evidenceUrl(blankToNull(entry.evidenceUrl()))
                .build();
        RecordIds.require(filled.id());
        AllowedValues.require("kind", filled.kind(), properties.historyKinds());
        requireLink("evidenceUrl", filled.evidenceUrl());
        return filled;
    }

    public void requireValid(JobApplication application) {
        RecordIds.require(application.id());
        requireText("company", application.company());
        requireText("role", application.role());
        AllowedValues.require("workType", application.workType(), properties.workTypes());
        AllowedValues.require("remoteRegion", application.remoteRegion(), properties.remoteRegions());
        AllowedValues.require("remoteStatus", application.remoteStatus(), properties.remoteStatuses());
        AllowedValues.require("status", application.status(), properties.statuses());
        requireText("remoteEvidence", application.remoteEvidence());
        requireText("nextAction", application.nextAction());
        requireLink("url", application.url());
        if (CONFIRMED.equals(application.remoteStatus()) && UNKNOWN.equals(application.remoteRegion())) {
            throw ApiException.badRequest("unconfirmed_remote_region",
                    "Confirmed remote eligibility requires a known remote region.");
        }
        requireDistinctHistory(application.history());
    }

    public void requireClearable(List<String> fields) {
        for (var field : fields) {
            if (!properties.clearableFields().contains(field)) {
                throw ApiException.badRequest("not_clearable",
                        "Only " + properties.clearableFields() + " may be cleared.");
            }
        }
    }

    /** A repeated entry ID or evidence link within one application is refused. */
    private void requireDistinctHistory(List<HistoryEntry> history) {
        if (history.size() > properties.maxHistory()) {
            throw ApiException.badRequest("too_much_history",
                    "An application keeps at most " + properties.maxHistory() + " history entries.");
        }
        var ids = new HashSet<String>();
        var links = new HashSet<String>();
        for (var entry : history) {
            if (!ids.add(entry.id()) || (entry.evidenceUrl() != null && !links.add(entry.evidenceUrl()))) {
                throw ApiException.badRequest("duplicate_history", "History repeats an entry ID or evidence link.");
            }
        }
    }

    private static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("invalid_request", field + " must not be blank");
        }
    }

    private static void requireLink(String field, String value) {
        if (value != null && (!WEB_LINK.matcher(value).matches() || CREDENTIALS_IN_LINK.matcher(value).matches())) {
            throw ApiException.badRequest("invalid_link",
                    field + " must be an HTTP or HTTPS link without embedded credentials.");
        }
    }

    private static String orDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
