package com.yourlifeone.life.health.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.life.health.config.HealthProperties;
import com.yourlifeone.life.health.domain.FastFoodEntry;
import com.yourlifeone.life.health.infrastructure.persistence.mapper.FastFoodPersistenceMapper;
import com.yourlifeone.life.health.infrastructure.persistence.repository.FastFoodEntryRepository;
import com.yourlifeone.life.shared.AllowedValues;
import com.yourlifeone.life.shared.RecordIds;
import com.yourlifeone.life.shared.Timestamps;
import com.yourlifeone.life.shared.UserScopedId;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The fast-food journal. A save replaces the whole entry; each delete removes exactly one entry. */
@Service
@RequiredArgsConstructor
public class FastFoodService {
    private final FastFoodEntryRepository repository;
    private final FastFoodPersistenceMapper persistence;
    private final HealthProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<FastFoodEntry> list(UUID userId) {
        return repository.findByUserIdOrderByDateDescCreatedAtDesc(userId).stream().map(persistence::toDomain).toList();
    }

    /** Creates the entry, or replaces it keeping its creation time. */
    @Transactional
    public FastFoodEntry save(UUID userId, FastFoodEntry requested) {
        RecordIds.require(requested.id());
        AllowedValues.require("rating", requested.rating(), properties.ratings());
        var symptoms = requested.symptoms().stream().distinct().toList();
        symptoms.forEach(symptom -> AllowedValues.require("symptoms", symptom, properties.symptoms()));
        var now = Timestamps.now(clock);
        var createdAt = repository.findById(new UserScopedId(userId, requested.id()))
                .map(existing -> existing.getCreatedAt()).orElse(now);
        var entry = requested.toBuilder()
                .venue(requested.venue().trim())
                .order(blankToNull(requested.order()))
                .symptoms(symptoms)
                .notes(requested.notes() == null ? "" : requested.notes())
                .createdAt(createdAt)
                .updatedAt(now)
                .build();
        return persistence.toDomain(repository.save(persistence.toEntity(userId, entry)));
    }

    @Transactional
    public void delete(UUID userId, String id) {
        var key = new UserScopedId(userId, RecordIds.require(id));
        if (!repository.existsById(key)) {
            throw ApiException.notFound("entry_not_found", "This journal entry does not exist.");
        }
        repository.deleteById(key);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
