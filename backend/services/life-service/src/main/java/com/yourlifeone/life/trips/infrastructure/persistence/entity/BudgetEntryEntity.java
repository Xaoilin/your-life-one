package com.yourlifeone.life.trips.infrastructure.persistence.entity;

import com.yourlifeone.life.shared.UserScopedId;
import jakarta.persistence.Column;
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
@Table(schema = "trips", name = "budget_entry")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetEntryEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String tripId;
    private String title;
    private String category;
    private long amount;
    private String status;
    @Column(name = "entry_date")
    private LocalDate date;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
