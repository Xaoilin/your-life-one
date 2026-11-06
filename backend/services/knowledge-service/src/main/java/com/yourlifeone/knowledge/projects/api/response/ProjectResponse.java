package com.yourlifeone.knowledge.projects.api.response;

import com.yourlifeone.knowledge.projects.domain.ProjectLink;
import com.yourlifeone.knowledge.projects.domain.ProjectPreview;
import com.yourlifeone.knowledge.projects.domain.ProjectRunRecipe;
import com.yourlifeone.knowledge.projects.domain.ProjectSetupStep;
import java.time.Instant;
import java.util.List;

public record ProjectResponse(
        String id,
        String catalogKey,
        String name,
        String kind,
        List<ProjectLink> links,
        List<ProjectSetupStep> setupSteps,
        List<ProjectRunRecipe> runRecipes,
        ProjectPreview preview,
        Instant verifiedAt,
        String summary,
        String status,
        String statusBeforeArchive,
        List<String> tags,
        boolean isPinned,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt) {
}
