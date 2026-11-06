package com.yourlifeone.profile.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "profile", name = "app_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPreferencesEntity {
    @Id
    private UUID userId;
    private String theme;
    private int dataRetentionDays;
    private boolean telemetry;
    private String defaultCalendarTab;
    private Instant updatedAt;

    /** Rows of {@code profile.goal_tag}, kept in order by their position. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "profile", name = "goal_tag", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "position")
    @Column(name = "name")
    @Builder.Default
    private List<String> goalTags = new ArrayList<>();
}
