package com.yourlifeone.access.identity.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Imported identity link. Sign-in never creates an account or links identities by email. */
@Entity
@Table(schema = "identity", name = "account_identity")
@Getter
@NoArgsConstructor
public class AccountIdentity {
    @Id
    private UUID id;
    private UUID userId;
    private String provider;
    private String subject;
    private String email;
    private boolean enabled;
}
