package com.yourlifeone.life.trips.infrastructure.persistence.entity;

import com.yourlifeone.life.shared.UserScopedId;
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
@Table(schema = "trips", name = "leg")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripLegEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String tripId;
    private String country;
    private String city;
    private LocalDate startDate;
    private LocalDate endDate;
    private int sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
}
