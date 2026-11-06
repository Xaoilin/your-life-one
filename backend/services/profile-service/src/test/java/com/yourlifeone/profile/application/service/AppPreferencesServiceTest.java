package com.yourlifeone.profile.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.profile.config.PreferencesProperties;
import com.yourlifeone.profile.domain.AppPreferences;
import com.yourlifeone.profile.infrastructure.persistence.entity.AppPreferencesEntity;
import com.yourlifeone.profile.infrastructure.persistence.repository.AppPreferencesRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppPreferencesServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");
    private static final UUID USER = UUID.randomUUID();
    private static final PreferencesProperties PROPERTIES = new PreferencesProperties(
            List.of("dark", "light"), "dark", List.of("month", "week", "agenda", "accounts"), 7, 365, 90, 3, 10);

    @Mock
    private AppPreferencesRepository repository;

    private AppPreferencesService service() {
        return new AppPreferencesService(repository, PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static AppPreferences.AppPreferencesBuilder valid() {
        return AppPreferences.builder().theme("light").dataRetentionDays(30).telemetry(true)
                .defaultCalendarTab("agenda").goalTags(List.of("Health"));
    }

    @Test
    void returnsTheConfiguredDefaultsUntilTheUserSaves() {
        when(repository.findById(USER)).thenReturn(Optional.empty());

        assertEquals(AppPreferences.builder().theme("dark").dataRetentionDays(90).telemetry(false)
                .goalTags(List.of()).build(), service().get(USER));
    }

    @Test
    void savesTrimmedTagsWithTheUpdateTime() {
        when(repository.findById(USER)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0, AppPreferencesEntity.class));

        var saved = service().update(USER, valid().goalTags(List.of(" Health ", "Work")).build());

        assertEquals(valid().goalTags(List.of("Health", "Work")).updatedAt(NOW).build(), saved);
    }

    @Test
    void refusesOptionsTheConfigurationDoesNotAllow() {
        var cases = List.of(
                new Object[] {valid().theme("neon").build(), "invalid_theme"},
                new Object[] {valid().defaultCalendarTab("year").build(), "invalid_calendar_tab"},
                new Object[] {valid().dataRetentionDays(6).build(), "invalid_data_retention"},
                new Object[] {valid().dataRetentionDays(366).build(), "invalid_data_retention"},
                new Object[] {valid().goalTags(List.of("a", "b", "c", "d")).build(), "too_many_goal_tags"},
                new Object[] {valid().goalTags(List.of("  ")).build(), "invalid_goal_tag"},
                new Object[] {valid().goalTags(List.of("x".repeat(11))).build(), "invalid_goal_tag"},
                new Object[] {valid().goalTags(List.of("Work", " Work")).build(), "duplicate_goal_tag"});
        for (var testCase : cases) {
            var error = assertThrows(ApiException.class, () -> service().update(USER, (AppPreferences) testCase[0]));
            assertEquals(testCase[1], error.code());
        }
    }

    @Test
    void aMissingTagListSavesNoTags() {
        when(repository.findById(USER)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0, AppPreferencesEntity.class));

        assertEquals(Collections.emptyList(), service().update(USER, valid().goalTags(null).build()).goalTags());
    }
}
