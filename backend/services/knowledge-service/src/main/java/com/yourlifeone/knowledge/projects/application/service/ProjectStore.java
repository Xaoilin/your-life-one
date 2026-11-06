package com.yourlifeone.knowledge.projects.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.knowledge.projects.domain.Project;
import com.yourlifeone.knowledge.projects.domain.ProjectCatalogue;
import com.yourlifeone.knowledge.projects.infrastructure.persistence.mapper.ProjectsPersistenceMapper;
import com.yourlifeone.knowledge.projects.infrastructure.persistence.repository.PageRepository;
import com.yourlifeone.knowledge.projects.infrastructure.persistence.repository.ProjectRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Loads a user's catalogue and saves the projects an operation changed. */
@Component
@RequiredArgsConstructor
public class ProjectStore {
    private final ProjectRepository projects;
    private final PageRepository pages;
    private final ProjectsPersistenceMapper persistence;

    public List<Project> projects(UUID userId) {
        return ProjectOrdering.inCatalogueOrder(projects.findByUserId(userId).stream().map(persistence::toDomain).toList());
    }

    public ProjectCatalogue catalogue(UUID userId) {
        return new ProjectCatalogue(projects(userId), pages.findByUserIdOrderByOverviewDescUpdatedAtDescIdAsc(userId)
                .stream().map(persistence::toDomain).toList());
    }

    public Project require(List<Project> catalogue, String projectId) {
        return catalogue.stream().filter(project -> project.id().equals(projectId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("project_not_found", "This project does not exist."));
    }

    /** Saves each project that differs from what was loaded. */
    public void saveChanged(UUID userId, List<Project> before, List<Project> after) {
        Map<String, Project> loaded = before.stream().collect(Collectors.toMap(Project::id, Function.identity()));
        var changed = after.stream().filter(project -> !project.equals(loaded.get(project.id())))
                .map(project -> persistence.toEntity(userId, project)).toList();
        projects.saveAll(changed);
    }
}
