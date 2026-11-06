package com.yourlifeone.profile.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.profile.config.ViewPreferencesProperties;
import com.yourlifeone.profile.infrastructure.persistence.entity.ViewPreferenceEntity;
import com.yourlifeone.profile.infrastructure.persistence.repository.ViewPreferenceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ViewPreferencesServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");
    private final UUID user = UUID.randomUUID();
    private final ViewPreferenceRepository repository = mock(ViewPreferenceRepository.class);
    private final ViewPreferencesProperties properties = new ViewPreferencesProperties(Map.of(
            "prayerChallenge.period", List.of("day", "week", "month")));
    private final ViewPreferencesService service = new ViewPreferencesService(
            repository, properties, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void savesOnlyTheExplicitlyChosenKey() {
        var saved = service.save(user, "prayerChallenge.period", "week");
        verify(repository).upsert(user, "prayerChallenge.period", "week", NOW);
        assertThat(saved.key()).isEqualTo("prayerChallenge.period");
        assertThat(saved.value()).isEqualTo("week");
        assertThat(saved.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsUnsupportedKeysValuesAndFreeTextBeforeDatabaseWork() {
        assertThatThrownBy(() -> service.save(user, "search.query", "private text")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.save(user, "prayerChallenge.period", "year")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.save(user, "prayerChallenge.period", null)).isInstanceOf(ApiException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void readsOnlyStillAllowedValuesWithoutWritingDefaults() {
        var valid = ViewPreferenceEntity.builder().userId(user).preferenceKey("prayerChallenge.period")
                .preferenceValue("day").updatedAt(NOW).build();
        var retired = ViewPreferenceEntity.builder().userId(user).preferenceKey("retired.tab")
                .preferenceValue("secret").updatedAt(NOW).build();
        var invalid = ViewPreferenceEntity.builder().userId(user).preferenceKey("prayerChallenge.period")
                .preferenceValue("year").updatedAt(NOW).build();
        when(repository.findByUserIdOrderByPreferenceKey(user)).thenReturn(List.of(valid, retired, invalid));
        assertThat(service.values(user)).containsExactlyEntriesOf(Map.of("prayerChallenge.period", "day"));
    }

    @Test
    void startsEmptyUntilAnAccountMakesAChoice() {
        when(repository.findByUserIdOrderByPreferenceKey(user)).thenReturn(List.of());
        assertThat(service.values(user)).isEmpty();
    }
}
