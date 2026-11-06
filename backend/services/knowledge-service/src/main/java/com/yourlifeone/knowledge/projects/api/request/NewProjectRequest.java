package com.yourlifeone.knowledge.projects.api.request;

import com.yourlifeone.knowledge.projects.domain.ProjectLink;
import com.yourlifeone.knowledge.projects.domain.ProjectPreview;
import com.yourlifeone.knowledge.projects.domain.ProjectRunRecipe;
import com.yourlifeone.knowledge.projects.domain.ProjectSetupStep;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * A new project. The app chooses both IDs, so a retried create never makes a second project. Unknown
 * kinds and statuses fall back to defaults and non-web links are dropped (see the service's rules).
 *
 * @param overviewPageId the ID of the overview page created with the project
 * @param catalogKey stable catalogue identity; absent for a hand-made project ({@code custom:<id>})
 */
public record NewProjectRequest(
        @NotBlank @Size(max = 256) String id,
        @NotBlank @Size(max = 256) String overviewPageId,
        @Size(max = 300) String catalogKey,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 4000) String summary,
        @Size(max = 32) String kind,
        @Size(max = 50) List<@NotNull ProjectLink> links,
        @Size(max = 50) List<@NotNull ProjectSetupStep> setupSteps,
        @Size(max = 20) List<@NotNull ProjectRunRecipe> runRecipes,
        ProjectPreview preview,
        Instant verifiedAt,
        @Size(max = 32) String status,
        @Size(max = 25) List<@NotNull @Size(max = 60) String> tags,
        boolean isPinned) {
}
