package com.yourlifeone.calendar.infrastructure.persistence.entity;

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

/** A Google refresh token, encrypted by {@code GoogleCredentialCipher}. Deliberately has no toString. */
@Entity
@Table(schema = "calendar", name = "google_credential")
@IdClass(GoogleCredentialId.class)
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCredentialEntity {
    @Id
    private UUID userId;
    @Id
    private String email;
    private byte[] refreshTokenCiphertext;
    private byte[] refreshTokenNonce;
    private short keyVersion;
    private String scope;
    private Instant lastRefreshAt;
    private String lastFailureReason;
    private Instant lastFailureAt;
    private Instant revokedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
