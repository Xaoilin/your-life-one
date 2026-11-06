package com.yourlifeone.finance.equity.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.finance.equity.config.EquityProperties;
import com.yourlifeone.finance.equity.domain.EquityPosition;
import com.yourlifeone.finance.equity.domain.EquityPositionDraft;
import com.yourlifeone.finance.equity.domain.PositionPage;
import com.yourlifeone.finance.equity.infrastructure.persistence.entity.PositionEntity;
import com.yourlifeone.finance.equity.infrastructure.persistence.mapper.PositionPersistenceMapper;
import com.yourlifeone.finance.equity.infrastructure.persistence.repository.PositionRepository;
import com.yourlifeone.finance.shared.ExpectedVersions;
import com.yourlifeone.finance.shared.RecordIds;
import com.yourlifeone.finance.shared.Timestamps;
import com.yourlifeone.finance.shared.UserScopedId;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Equity positions, each replaced whole. A create names no version and is refused when the ID is taken;
 * an edit or delete names the {@code updatedAt} it read and is refused when the position changed since.
 */
@Service
@RequiredArgsConstructor
public class EquityService {
    private final PositionRepository positions;
    private final PositionPersistenceMapper persistence;
    private final EquityRules rules;
    private final EquityProperties properties;
    private final Clock clock;

    /** Positions whose company contains the query (ignoring case), in ID order. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PositionPage search(UUID userId, String query, Integer limit, Integer offset) {
        var size = rules.limit(limit);
        var start = rules.offset(offset);
        var needle = rules.query(query);
        var page = positions.search(userId, needle, size, start).stream().map(persistence::toDomain).toList();
        return new PositionPage(page, positions.countMatches(userId, needle));
    }

    @Transactional(readOnly = true)
    public EquityPosition position(UUID userId, String id) {
        return positions.findById(new UserScopedId(userId, RecordIds.require(id))).map(persistence::toDomain)
                .orElseThrow(EquityService::notFound);
    }

    /** @param expectedUpdatedAt the {@code updatedAt} being replaced; null to create the position */
    @Transactional
    public EquityPosition save(UUID userId, String id, EquityPositionDraft draft, String expectedUpdatedAt) {
        RecordIds.require(id);
        var position = rules.check(draft);
        var existing = positions.lock(userId, id);
        if (expectedUpdatedAt == null) {
            requireNew(userId, existing);
        } else {
            requireVersion(existing, expectedUpdatedAt);
        }
        var now = Timestamps.now(clock);
        var entity = existing.orElseGet(() -> PositionEntity.builder().userId(userId).id(id).createdAt(now).build());
        entity.setDocument(position);
        entity.setCompany(position.company());
        entity.setAsOf(LocalDate.parse(position.asOf()));
        entity.setUpdatedAt(now);
        return persistence.toDomain(positions.saveAndFlush(entity));
    }

    @Transactional
    public String delete(UUID userId, String id, String expectedUpdatedAt) {
        var existing = positions.lock(userId, RecordIds.require(id));
        var entity = requireVersion(existing, expectedUpdatedAt);
        positions.delete(entity);
        return id;
    }

    private void requireNew(UUID userId, Optional<PositionEntity> existing) {
        if (existing.isPresent()) {
            throw ApiException.conflict("position_exists", "This position already exists; update it with its updatedAt.");
        }
        if (positions.countByUserId(userId) >= properties.maxPositions()) {
            throw ApiException.conflict("position_limit_reached",
                    "At most " + properties.maxPositions() + " equity positions are kept.");
        }
    }

    private static PositionEntity requireVersion(Optional<PositionEntity> existing, String expectedUpdatedAt) {
        var entity = existing.orElseThrow(EquityService::notFound);
        if (!ExpectedVersions.matches(expectedUpdatedAt, entity.getUpdatedAt())) {
            throw ApiException.conflict("position_changed", "This position changed since it was read; reload and try again.");
        }
        return entity;
    }

    private static ApiException notFound() {
        return ApiException.notFound("position_not_found", "This equity position does not exist.");
    }
}
