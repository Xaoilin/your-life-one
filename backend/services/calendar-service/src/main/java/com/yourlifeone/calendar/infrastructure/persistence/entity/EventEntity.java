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

@Entity
@Table(schema = "calendar", name = "event")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EventEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String sourceId;
    private String title;
    private String description;
    private String location;
    private boolean allDay;
    private Instant startsAt;
    private Instant endsAt;
    private String googleEventId;
    private Instant createdAt;
    private Instant updatedAt;
}
