package com.yourlifeone.knowledge.projects.domain;

import java.time.Instant;
import lombok.Builder;

/**
 * A wiki page of a project.
 *
 * @param overview the project's overview page; each project has one
 */
@Builder(toBuilder = true)
public record ProjectPage(
        String id,
        String projectId,
        String title,
        String content,
        boolean overview,
        Instant createdAt,
        Instant updatedAt) {
}
