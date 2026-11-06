package com.yourlifeone.profile.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.profile.config.IntegrationProperties;
import com.yourlifeone.profile.infrastructure.persistence.entity.IntegrationEntity;
import com.yourlifeone.profile.infrastructure.persistence.repository.IntegrationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");
    private static final UUID USER = UUID.randomUUID();

    @Mock
    private IntegrationRepository repository;

    private IntegrationService service() {
        return new IntegrationService(repository, new IntegrationProperties(List.of("google")),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void savesAnOfferedProvidersRecordWithoutABlankError() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0, IntegrationEntity.class));

        var saved = service().save(USER, "google", "connected", NOW, "  ");

        assertEquals("connected", saved.status());
        assertEquals(NOW, saved.updatedAt());
        assertNull(saved.lastError());
    }

    @Test
    void refusesProvidersThatAreNotOfferedAndUnknownStatuses() {
        var unknown = assertThrows(ApiException.class, () -> service().save(USER, "github", "connected", null, null));
        assertEquals(HttpStatus.NOT_FOUND, unknown.status());

        var invalid = assertThrows(ApiException.class, () -> service().save(USER, "google", "mocked", null, null));
        assertEquals("invalid_status", invalid.code());
    }
}
