package com.yourlifeone.finance.ledger.infrastructure.persistence.entity;

import com.yourlifeone.finance.shared.UserScopedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "finance", name = "savings_goal")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsGoalEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String name;
    private long targetAmount;
    private long currentAmount;
    private String linkedAccountId;
    private String icon;
    private LocalDate deadline;
    private boolean completed;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
