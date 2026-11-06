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
@Table(schema = "trips", name = "booking")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String tripId;
    private String legId;
    private String kind;
    private String title;
    private String mode;
    private String fromLabel;
    private String toLabel;
    private String departAt;
    private String arriveAt;
    private String propertyName;
    private String address;
    private String city;
    private String country;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Long budgetAmount;
    private String budgetStatus;
    private LocalDate budgetDate;
    private String provider;
    private String confirmationCode;
    private String link;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
