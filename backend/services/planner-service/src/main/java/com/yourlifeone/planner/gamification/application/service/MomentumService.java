package com.yourlifeone.planner.gamification.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.planner.gamification.config.MomentumProperties;
import com.yourlifeone.planner.gamification.domain.MomentumPillar;
import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.MomentumEntity;
import com.yourlifeone.planner.gamification.infrastructure.persistence.entity.MomentumId;
import com.yourlifeone.planner.gamification.infrastructure.persistence.repository.MomentumRepository;
import com.yourlifeone.planner.shared.AllowedValues;
import com.yourlifeone.planner.shared.Timestamps;
import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Daily momentum, one state per pillar replaced whole. A save names the version it changed, so a tab with
 * an out-of-date copy is refused instead of overwriting newer progress.
 */
@Service
@RequiredArgsConstructor
public class MomentumService {
    private final MomentumRepository momentum;
    private final MomentumProperties properties;
    private final JsonMapper json;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Map<String, MomentumPillar> list(UUID userId) {
        return momentum.findByUserId(userId).stream().map(MomentumService::toDomain)
                .collect(Collectors.toMap(MomentumPillar::pillar, pillar -> pillar));
    }

    /** @param expectedVersion the version the app changed; null when it creates the pillar's state */
    @Transactional
    public MomentumPillar save(UUID userId, String pillar, Map<String, Object> state, Integer expectedVersion) {
        AllowedValues.require("pillar", pillar, properties.pillars());
        requireSize(state);
        var existing = momentum.findById(new MomentumId(userId, pillar));
        var current = existing.map(MomentumEntity::getVersion).orElse(null);
        if (!Objects.equals(current, expectedVersion)) {
            throw ApiException.conflict("momentum_changed",
                    "Daily momentum changed since it was shown; reload and try again.");
        }
        var entity = existing.orElseGet(() -> MomentumEntity.builder().userId(userId).pillar(pillar).build());
        entity.setState(state);
        entity.setUpdatedAt(Timestamps.now(clock));
        return toDomain(momentum.saveAndFlush(entity));
    }

    private void requireSize(Map<String, Object> state) {
        if (json.writeValueAsString(state).length() > properties.maxStateChars()) {
            throw ApiException.badRequest("momentum_too_large", "This daily momentum state is too large to save.");
        }
    }

    private static MomentumPillar toDomain(MomentumEntity entity) {
        return new MomentumPillar(entity.getPillar(), entity.getState(), entity.getVersion(), entity.getUpdatedAt());
    }
}
