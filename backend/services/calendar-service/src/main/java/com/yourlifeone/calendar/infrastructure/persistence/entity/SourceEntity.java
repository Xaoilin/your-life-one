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
@Table(schema = "calendar", name = "source")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SourceEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String accountId;
    private String name;
    private String color;
    private boolean visible;
    private String googleCalendarId;
    private String accessRole;
    private Instant createdAt;
    private Instant updatedAt;
}
