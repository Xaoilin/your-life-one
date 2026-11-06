package com.yourlifeone.profile.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "profile", name = "integration")
@IdClass(IntegrationKey.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationEntity {
    @Id
    private UUID userId;
    @Id
    private String provider;
    private String status;
    private Instant configuredAt;
    private String lastError;
    private Instant updatedAt;
}
