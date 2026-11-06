package com.yourlifeone.profile.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

/** Primary key of {@link IntegrationEntity}: one record per user and provider. */
public record IntegrationKey(UUID userId, String provider) implements Serializable {
}
