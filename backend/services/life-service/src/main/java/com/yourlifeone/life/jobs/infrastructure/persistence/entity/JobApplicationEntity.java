package com.yourlifeone.life.jobs.infrastructure.persistence.entity;

import com.yourlifeone.life.shared.UserScopedId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "jobs", name = "application")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private int position;
    private String company;
    private String role;
    private String url;
    private String workType;
    private String remoteRegion;
    private String remoteStatus;
    private String remoteEvidence;
    private String remoteCaveat;
    private String compensation;
    private String status;
    private LocalDate applicationDate;
    private String nextAction;
    private LocalDate nextActionDate;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    /** Rows of {@code jobs.history_entry}, oldest first by their position. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(schema = "jobs", name = "history_entry", joinColumns = {
            @JoinColumn(name = "user_id", referencedColumnName = "userId"),
            @JoinColumn(name = "application_id", referencedColumnName = "id")})
    @OrderColumn(name = "position")
    @Builder.Default
    private List<HistoryEntryEmbeddable> history = new ArrayList<>();
}
