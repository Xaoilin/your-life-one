package com.yourlifeone.profile.activity.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * What a product-usage event may contain ({@code app.profile.activity.ingest.*}). Every text value is a
 * stable taxonomy key, never user content.
 *
 * @param schemaVersion the only event schema accepted
 * @param maxBatch most events in one request
 * @param maxSequence highest position of an event within its session
 * @param maxDurationMs longest duration an event may report
 * @param kinds event kinds, e.g. {@code navigation}
 * @param surfaces app surfaces an event may name, e.g. {@code calendar}
 * @param outcomes outcomes an event may report, e.g. {@code success}
 * @param deviceClasses e.g. {@code mobile}
 * @param inputKinds e.g. {@code pointer}
 * @param metadataKeys the only metadata keys accepted; values must be scalars
 * @param maxMetadataBytes largest metadata object, as compact JSON
 * @param maxMetadataStringLength longest metadata string value
 * @param taxonomyPattern pattern for {@code feature} and {@code action}
 * @param errorCodePattern pattern for {@code errorCode}
 * @param targetPattern pattern for {@code target}
 * @param releasePattern pattern for {@code releaseVersion}
 * @param maxReleaseLength longest {@code releaseVersion}
 */
@ConfigurationProperties("app.profile.activity.ingest")
public record ActivityIngestProperties(
        int schemaVersion,
        int maxBatch,
        int maxSequence,
        int maxDurationMs,
        List<String> kinds,
        List<String> surfaces,
        List<String> outcomes,
        List<String> deviceClasses,
        List<String> inputKinds,
        List<String> metadataKeys,
        int maxMetadataBytes,
        int maxMetadataStringLength,
        String taxonomyPattern,
        String errorCodePattern,
        String targetPattern,
        String releasePattern,
        int maxReleaseLength) {
    public ActivityIngestProperties {
        kinds = copy(kinds);
        surfaces = copy(surfaces);
        outcomes = copy(outcomes);
        deviceClasses = copy(deviceClasses);
        inputKinds = copy(inputKinds);
        metadataKeys = copy(metadataKeys);
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
