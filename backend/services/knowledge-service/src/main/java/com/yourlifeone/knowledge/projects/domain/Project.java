package com.yourlifeone.knowledge.projects.domain;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * A project in the catalogue.
 *
 * @param catalogKey stable identity that Inventory links to; {@code custom:<id>} for hand-made projects
 * @param statusBeforeArchive the status restored when an archived project comes back; null otherwise
 * @param pinned shown in the pinned section; archived projects are never pinned
 * @param sortOrder position within the project's catalogue section
 */
@Builder(toBuilder = true)
public record Project(
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
        boolean pinned,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {
    /** The status of an archived project; archive and unarchive move it between catalogue sections. */
    public static final String ARCHIVED = "archived";

    public boolean archived() {
        return ARCHIVED.equals(status);
    }
}
