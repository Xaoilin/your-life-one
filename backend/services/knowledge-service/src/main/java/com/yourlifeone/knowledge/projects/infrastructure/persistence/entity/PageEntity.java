package com.yourlifeone.knowledge.projects.infrastructure.persistence.entity;

import com.yourlifeone.knowledge.shared.UserScopedId;
import jakarta.persistence.Column;
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
@Table(schema = "projects", name = "page")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String projectId;
    private String title;
    private String content;
    @Column(name = "is_overview")
    private boolean overview;
    private Instant createdAt;
    private Instant updatedAt;
}
