package com.yourlifeone.calendar.infrastructure.persistence.entity;

import com.yourlifeone.calendar.domain.AccountProvider;
import com.yourlifeone.calendar.domain.AuthStatus;
import jakarta.persistence.Column;
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
@Table(schema = "calendar", name = "account")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private AccountProvider provider;
    private String name;
    private String email;
    @Column(name = "is_primary")
    private boolean primary;
    private Integer paletteIndex;
    private AuthStatus authStatus;
    private String authError;
    private Instant lastSyncedAt;
    private String syncError;
    private Instant createdAt;
    private Instant updatedAt;
}
