package com.yourlifeone.profile.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.profile.config.ViewPreferencesProperties;
import com.yourlifeone.profile.domain.ViewPreference;
import com.yourlifeone.profile.infrastructure.persistence.repository.ViewPreferenceRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewPreferencesService {
    private final ViewPreferenceRepository preferences;
    private final ViewPreferencesProperties properties;
    private final Clock clock;

    /** No default rows are created by a read; retired or invalid choices fall back in the UI. */
    @Transactional(readOnly = true)
    public Map<String, String> values(UUID user) {
        var result = new LinkedHashMap<String, String>();
        for (var preference : preferences.findByUserIdOrderByPreferenceKey(user)) {
            if (properties.allows(preference.getPreferenceKey(), preference.getPreferenceValue())) {
                result.put(preference.getPreferenceKey(), preference.getPreferenceValue());
            }
        }
        return Map.copyOf(result);
    }

    @Transactional
    public ViewPreference save(UUID user, String key, String value) {
        if (!properties.allows(key, value)) {
            throw ApiException.badRequest("invalid_view_preference", "That display preference or value is not supported.");
        }
        var now = clock.instant();
        preferences.upsert(user, key, value, now);
        return new ViewPreference(key, value, now);
    }
}
