package com.yourlifeone.profile.activity.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.profile.activity.config.ActivityIngestProperties;
import com.yourlifeone.profile.activity.domain.ProductUsageEvent;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductUsageEventValidatorTest {
    private final ProductUsageEventValidator validator =
            new ProductUsageEventValidator(ActivityTestProperties.INGEST, JsonMapper.builder().build());

    private static ProductUsageEvent event(UnaryOperator<ProductUsageEvent.ProductUsageEventBuilder> change) {
        return change.apply(ProductUsageEvent.builder()
                .eventId(UUID.randomUUID())
                .schemaVersion(1)
                .sessionId(UUID.randomUUID())
                .sequence(1)
                .kind("navigation")
                .occurredAt(Instant.parse("2026-09-27T10:00:00Z"))
                .surface("calendar")
                .feature("navigation")
                .action("viewed")
                .releaseVersion("0.2.203")
                .deviceClass("desktop")
                .inputKind("pointer")
                .online(true)
                .reducedMotion(false)
                .metadata(Map.of("previousSurface", "dashboard", "retryCount", 2))).build();
    }

    private String refusal(ProductUsageEvent event) {
        return assertThrows(ApiException.class, () -> validator.validate(List.of(event))).code();
    }

    @Test
    void acceptsAContentFreeEventAndOptionalFieldsLeftOut() {
        assertDoesNotThrow(() -> validator.validate(List.of(event(e -> e),
                event(e -> e.surface(null).metadata(null).releaseVersion("1.0.0-rc.1")),
                event(e -> e.kind("error").outcome("failure").errorCode("calendar:sync_failed").target("google-calendar")
                        .durationMs(1_800_000)))));
    }

    @Test
    void acceptsSoftwareEventsUsingTheShippedConfiguration() throws Exception {
        var sources = new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        var properties = new Binder(ConfigurationPropertySources.from(sources))
                .bind("app.profile.activity.ingest", ActivityIngestProperties.class).get();
        var configuredValidator = new ProductUsageEventValidator(properties, JsonMapper.builder().build());

        assertDoesNotThrow(() -> configuredValidator.validate(List.of(event(e -> e.surface("software")))));
    }

    @Test
    void refusesEmptyAndOversizedBatches() {
        assertEquals("invalid_batch_size", assertThrows(ApiException.class, () -> validator.validate(List.of())).code());
        var tooMany = Collections.nCopies(ActivityTestProperties.MAX_BATCH + 1, event(e -> e));
        assertEquals("invalid_batch_size", assertThrows(ApiException.class, () -> validator.validate(tooMany)).code());
    }

    @Test
    void refusesMetadataOutsideTheContentFreeAllowlist() {
        assertEquals("invalid_metadata", refusal(event(e -> e.metadata(Map.of("accessToken", "forbidden")))));
        assertEquals("invalid_metadata", refusal(event(e -> e.metadata(Map.of("previousSurface", Map.of("a", 1))))));
        assertEquals("invalid_metadata", refusal(event(e -> e.metadata(Map.of("previousSurface", "x".repeat(97))))));
        var nullValue = new HashMap<String, Object>();
        nullValue.put("visibilityState", null);
        assertDoesNotThrow(() -> validator.validate(List.of(event(e -> e.metadata(nullValue)))));
    }

    @Test
    void refusesAnythingOutsideTheTaxonomy() {
        assertEquals("invalid_taxonomy", refusal(event(e -> e.schemaVersion(2))));
        assertEquals("invalid_taxonomy", refusal(event(e -> e.sequence(0))));
        assertEquals("invalid_taxonomy", refusal(event(e -> e.kind("click"))));
        assertEquals("invalid_taxonomy", refusal(event(e -> e.feature("Private note"))));
        assertEquals("invalid_taxonomy", refusal(event(e -> e.action("a".repeat(65)))));
        assertEquals("invalid_taxonomy", refusal(event(e -> e.releaseVersion("latest"))));
        assertEquals("invalid_taxonomy", refusal(event(e -> e.deviceClass("watch"))));
        assertEquals("invalid_taxonomy", refusal(event(e -> e.inputKind("gesture"))));
        assertEquals("invalid_surface", refusal(event(e -> e.surface("chatroom"))));
        assertEquals("invalid_outcome", refusal(event(e -> e.outcome("maybe"))));
        assertEquals("invalid_duration", refusal(event(e -> e.durationMs(-1))));
        assertEquals("invalid_error_code", refusal(event(e -> e.errorCode("Raw message"))));
        assertEquals("invalid_target", refusal(event(e -> e.target("https://example.com"))));
    }
}
