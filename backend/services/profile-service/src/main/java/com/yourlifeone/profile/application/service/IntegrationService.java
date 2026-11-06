package com.yourlifeone.profile.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.profile.config.IntegrationProperties;
import com.yourlifeone.profile.domain.Integration;
import com.yourlifeone.profile.domain.IntegrationStatus;
import com.yourlifeone.profile.infrastructure.persistence.entity.IntegrationEntity;
import com.yourlifeone.profile.infrastructure.persistence.repository.IntegrationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A user's integration connection records, one per provider. */
@Service
@RequiredArgsConstructor
public class IntegrationService {
    private final IntegrationRepository integrations;
    private final IntegrationProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Integration> list(UUID userId) {
        return integrations.findByUserIdOrderByProvider(userId).stream().map(IntegrationService::toDomain).toList();
    }

    /** Creates or replaces the user's record for the provider. */
    @Transactional
    public Integration save(UUID userId, String provider, String status, Instant configuredAt, String lastError) {
        if (!properties.providers().contains(provider)) {
            throw ApiException.notFound("unknown_integration", "No integration is offered for this provider.");
        }
        if (!IntegrationStatus.ALL.contains(status)) {
            throw ApiException.badRequest("invalid_status", "status must be one of " + IntegrationStatus.ALL + ".");
        }
        var entity = IntegrationEntity.builder()
                .userId(userId)
                .provider(provider)
                .status(status)
                .configuredAt(configuredAt)
                .lastError(lastError == null || lastError.isBlank() ? null : lastError.trim())
                .updatedAt(clock.instant())
                .build();
        return toDomain(integrations.save(entity));
    }

    private static Integration toDomain(IntegrationEntity entity) {
        return Integration.builder()
                .provider(entity.getProvider())
                .status(entity.getStatus())
                .configuredAt(entity.getConfiguredAt())
                .lastError(entity.getLastError())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
