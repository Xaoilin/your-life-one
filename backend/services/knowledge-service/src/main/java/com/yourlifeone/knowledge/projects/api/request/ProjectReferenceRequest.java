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

/** What a project shows, replaced as a whole; status, pinning and position have their own operations. */
public record ProjectReferenceRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 4000) String summary,
        @Size(max = 32) String kind,
        @Size(max = 50) List<@NotNull ProjectLink> links,
        @Size(max = 50) List<@NotNull ProjectSetupStep> setupSteps,
        @Size(max = 20) List<@NotNull ProjectRunRecipe> runRecipes,
        ProjectPreview preview,
        Instant verifiedAt,
        @Size(max = 25) List<@NotNull @Size(max = 60) String> tags) {
}
