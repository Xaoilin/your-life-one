package com.yourlifeone.knowledge.projects.application.service;

import com.yourlifeone.knowledge.projects.config.ProjectsProperties;
import com.yourlifeone.knowledge.projects.domain.ProjectCatalogue;
import com.yourlifeone.knowledge.projects.domain.ProjectMatch;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads the catalogue, and finds projects by name for Inventory. */
@Service
@RequiredArgsConstructor
public class ProjectQueries {
    private final ProjectStore store;
    private final ProjectsProperties properties;

    @Transactional(readOnly = true)
    public ProjectCatalogue catalogue(UUID userId) {
        return store.catalogue(userId);
    }

    @Transactional(readOnly = true)
    public List<ProjectMatch> resolve(UUID userId, String query) {
        return ProjectResolver.resolve(store.projects(userId), query, properties.maxResolveResults());
    }
}
