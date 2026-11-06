package com.yourlifeone.profile.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.profile.config.ProfileProperties;
import com.yourlifeone.profile.domain.GlobalSettings;
import com.yourlifeone.profile.infrastructure.persistence.entity.GlobalSettingsEntity;
import com.yourlifeone.profile.infrastructure.persistence.repository.GlobalSettingsRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSettingsServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-25T12:00:00Z");
    private static final UUID USER = UUID.randomUUID();

    @Mock
    private GlobalSettingsRepository repository;

    private GlobalSettingsService service() {
        return new GlobalSettingsService(repository, new ProfileProperties("Bedford", "United Kingdom"),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void saveReturnsItsInput() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0, GlobalSettingsEntity.class));
    }

    @Test
    void returnsTheConfiguredDefaultsUntilTheUserSavesSettings() {
        when(repository.findById(USER)).thenReturn(Optional.empty());

        assertEquals(GlobalSettings.builder().city("Bedford").country("United Kingdom").build(), service().get(USER));
    }

    @Test
    void savesTrimmedValuesWithTheUpdateTime() {
        saveReturnsItsInput();

        var saved = service().update(USER, " London ", " United Kingdom ", " Europe/London ");

        assertEquals(new GlobalSettings("London", "United Kingdom", "Europe/London", NOW), saved);
    }

    @Test
    void blankTimeZoneFollowsTheDevice() {
        saveReturnsItsInput();

        assertNull(service().update(USER, "London", "United Kingdom", "  ").timeZone());
    }

    @Test
    void rejectsFixedOffsetsAndUnknownZones() {
        for (var zone : new String[] {"+01:00", "UTC+1", "Mars/Olympus"}) {
            var error = assertThrows(ApiException.class, () -> service().update(USER, "London", "UK", zone));
            assertEquals("invalid_time_zone", error.code());
        }
    }
}
