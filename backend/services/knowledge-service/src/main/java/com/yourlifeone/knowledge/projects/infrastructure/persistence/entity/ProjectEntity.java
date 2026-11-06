package com.yourlifeone.knowledge.projects.infrastructure.persistence.entity;

import com.yourlifeone.knowledge.projects.domain.ProjectLink;
import com.yourlifeone.knowledge.projects.domain.ProjectPreview;
import com.yourlifeone.knowledge.projects.domain.ProjectRunRecipe;
import com.yourlifeone.knowledge.projects.domain.ProjectSetupStep;
import com.yourlifeone.knowledge.shared.UserScopedId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(schema = "projects", name = "project")
@IdClass(UserScopedId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEntity {
    @Id
    private UUID userId;
    @Id
    private String id;
    private String catalogKey;
    private String name;
    private String kind;
    private String summary;
    private String status;
    private String statusBeforeArchive;
    @Column(name = "is_pinned")
    private boolean pinned;
    private Integer sortOrder;
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> tags;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<ProjectLink> links;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<ProjectSetupStep> setupSteps;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<ProjectRunRecipe> runRecipes;
    @JdbcTypeCode(SqlTypes.JSON)
    private ProjectPreview preview;
    private Instant verifiedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
