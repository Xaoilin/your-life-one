package com.yourlifeone.life.health.infrastructure.persistence.entity;

import com.yourlifeone.life.shared.UserScopedId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "health", name = "fast_food_entry")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FastFoodEntryEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String venue;
    @Column(name = "entry_date")
    private LocalDate date;
    @Column(name = "food_order")
    private String order;
    private String rating;
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> symptoms;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
