package com.yourlifeone.finance.ledger.infrastructure.persistence.entity;

import com.yourlifeone.finance.shared.UserScopedId;
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
@Table(schema = "finance", name = "account")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String name;
    private String type;
    private long balance;
    private String currency;
    private String color;
    private String icon;
    private boolean includeInNetWorth;
    private int sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
}
