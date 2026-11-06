package com.yourlifeone.profile.activity.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.profile.activity.config.ActivityIngestProperties;
import com.yourlifeone.profile.activity.domain.ProductUsageEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Refuses a batch unless every event is content-free and uses the configured taxonomy, as Supabase's
 * ingest function did: one invalid event refuses the whole batch and nothing is stored.
 */
@Component
public class ProductUsageEventValidator {
    private final ActivityIngestProperties properties;
    private final JsonMapper json;
    private final Pattern taxonomy;
    private final Pattern errorCode;
    private final Pattern target;
    private final Pattern release;

    public ProductUsageEventValidator(ActivityIngestProperties properties, JsonMapper json) {
        this.properties = properties;
        this.json = json;
        this.taxonomy = Pattern.compile(properties.taxonomyPattern());
        this.errorCode = Pattern.compile(properties.errorCodePattern());
        this.target = Pattern.compile(properties.targetPattern());
        this.release = Pattern.compile(properties.releasePattern());
    }

    public void validate(List<ProductUsageEvent> events) {
        if (events == null || events.isEmpty() || events.size() > properties.maxBatch()) {
            throw ApiException.badRequest("invalid_batch_size",
                    "Product usage batches must contain between 1 and " + properties.maxBatch() + " events.");
        }
        events.forEach(this::validate);
    }

    private void validate(ProductUsageEvent event) {
        if (!metadataIsSafe(event.metadata())) {
            refuse("invalid_metadata", "Product usage metadata must use the content-free allowlist.");
        }
        if (!taxonomyIsValid(event)) {
            refuse("invalid_taxonomy", "Product usage event taxonomy is invalid.");
        }
        if (event.surface() != null && !properties.surfaces().contains(event.surface())) {
            refuse("invalid_surface", "Product usage surface is invalid.");
        }
        if (event.outcome() != null && !properties.outcomes().contains(event.outcome())) {
            refuse("invalid_outcome", "Product usage outcome is invalid.");
        }
        if (event.durationMs() != null && (event.durationMs() < 0 || event.durationMs() > properties.maxDurationMs())) {
            refuse("invalid_duration", "Product usage duration is invalid.");
        }
        if (event.errorCode() != null && !errorCode.matcher(event.errorCode()).matches()) {
            refuse("invalid_error_code", "Product usage error code is invalid.");
        }
        if (event.target() != null && !target.matcher(event.target()).matches()) {
            refuse("invalid_target", "Product usage target is invalid.");
        }
    }

    private boolean taxonomyIsValid(ProductUsageEvent event) {
        return event.eventId() != null
                && event.sessionId() != null
                && event.occurredAt() != null
                && event.schemaVersion() != null && event.schemaVersion() == properties.schemaVersion()
                && event.sequence() != null && event.sequence() >= 1 && event.sequence() <= properties.maxSequence()
                && properties.kinds().contains(event.kind())
                && matches(taxonomy, event.feature())
                && matches(taxonomy, event.action())
                && matches(release, event.releaseVersion())
                && event.releaseVersion().length() <= properties.maxReleaseLength()
                && properties.deviceClasses().contains(event.deviceClass())
                && properties.inputKinds().contains(event.inputKind())
                && event.online() != null
                && event.reducedMotion() != null;
    }

    /** Allowlisted keys, scalar values, short strings and a small object overall. */
    private boolean metadataIsSafe(Map<String, Object> metadata) {
        if (metadata == null) return true;
        for (var entry : metadata.entrySet()) {
            if (!properties.metadataKeys().contains(entry.getKey()) || !isSafeValue(entry.getValue())) return false;
        }
        return json.writeValueAsString(metadata).getBytes(StandardCharsets.UTF_8).length
                <= properties.maxMetadataBytes();
    }

    private boolean isSafeValue(Object value) {
        return value == null
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof String text && text.length() <= properties.maxMetadataStringLength();
    }

    private static boolean matches(Pattern pattern, String value) {
        return value != null && pattern.matcher(value).matches();
    }

    private static void refuse(String code, String message) {
        throw ApiException.badRequest(code, message);
    }
}
