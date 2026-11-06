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
@Table(schema = "trips", name = "itinerary_item")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryItemEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String tripId;
    private String legId;
    @Column(name = "item_date")
    private LocalDate date;
    private String title;
    private String startTime;
    private String endTime;
    private String location;
    private String notes;
    private int sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
}
