package com.yourlifeone.knowledge.projects.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.knowledge.projects.domain.CatalogueSection;
import com.yourlifeone.knowledge.projects.domain.Project;
import com.yourlifeone.knowledge.projects.domain.ProjectCatalogue;
import com.yourlifeone.knowledge.projects.infrastructure.persistence.mapper.ProjectsPersistenceMapper;
import com.yourlifeone.knowledge.projects.infrastructure.persistence.repository.PageRepository;
import com.yourlifeone.knowledge.projects.infrastructure.persistence.repository.ProjectRepository;
import com.yourlifeone.knowledge.shared.RecordIds;
import com.yourlifeone.knowledge.shared.Timestamps;
import com.yourlifeone.knowledge.shared.UserScopedId;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The project catalogue. Each write changes one project, plus the positions of the projects in the
 * sections it touches, worked out from the stored catalogue rather than the caller's copy. Every write
 * answers with the whole catalogue so the app shows exactly what is stored.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectStore store;
    private final ProjectRepository projects;
    private final PageRepository pages;
    private final ProjectNormalizer normalizer;
    private final ProjectsPersistenceMapper persistence;
    private final Clock clock;

    /** Adds a project at the end of its section, with its overview page. */
    @Transactional
    public ProjectCatalogue create(UUID userId, Project requested, String overviewPageId) {
        var id = RecordIds.require(requested.id());
        RecordIds.require(overviewPageId);
        var before = store.projects(userId);
        if (before.stream().anyMatch(project -> project.id().equals(id))) {
            throw ApiException.conflict("project_exists", "A project with this ID already exists.");
        }
        var now = Timestamps.now(clock);
        var project = normalizer.normalize(requested.toBuilder().sortOrder(null).createdAt(now).updatedAt(now).build());
        if (projects.existsByUserIdAndCatalogKey(userId, project.catalogKey())) {
            throw ApiException.conflict("catalog_key_exists", "Another project already uses this catalogue key.");
        }
        store.saveChanged(userId, before, ProjectOrdering.append(before, project, now));
        pages.save(persistence.toEntity(userId, OverviewPages.forProject(overviewPageId, project)));
        return store.catalogue(userId);
    }

    /** Replaces what the project shows (name, summary, kind, links, guidance, look, tags); its place stays. */
    @Transactional
    public ProjectCatalogue updateReference(UUID userId, String projectId, Project reference) {
        var before = store.projects(userId);
        var current = store.require(before, projectId);
        var updated = normalizer.normalize(current.toBuilder()
                .name(reference.name())
                .summary(reference.summary())
                .kind(reference.kind())
                .links(reference.links())
                .setupSteps(reference.setupSteps())
                .runRecipes(reference.runRecipes())
                .preview(reference.preview())
                .verifiedAt(reference.verifiedAt())
                .tags(reference.tags())
                .updatedAt(Timestamps.now(clock))
                .build());
        store.saveChanged(userId, before, replace(before, updated));
        return store.catalogue(userId);
    }

    /** Sets the status of a project that is not archived; archiving has its own operation. */
    @Transactional
    public ProjectCatalogue setStatus(UUID userId, String projectId, String status) {
        var before = store.projects(userId);
        var current = store.require(before, projectId);
        if (Project.ARCHIVED.equals(status) || current.archived()) {
            throw ApiException.conflict("use_archive", "Archive or unarchive the project to change this status.");
        }
        var updated = normalizer.normalize(current.toBuilder().status(status).updatedAt(Timestamps.now(clock)).build());
        if (!updated.status().equals(status)) {
            throw ApiException.badRequest("invalid_status", "This project status does not exist.");
        }
        store.saveChanged(userId, before, replace(before, updated));
        return store.catalogue(userId);
    }

    /** Pins or unpins a project, moving it to the end of the section it joins. Archived projects cannot be pinned. */
    @Transactional
    public ProjectCatalogue setPinned(UUID userId, String projectId, boolean pinned) {
        var before = store.projects(userId);
        var current = store.require(before, projectId);
        if (pinned && current.archived()) {
            throw ApiException.conflict("project_archived", "Unarchive the project before pinning it.");
        }
        if (current.pinned() != pinned) {
            store.saveChanged(userId, before, ProjectOrdering.transition(before, projectId,
                    project -> project.toBuilder().pinned(pinned).build(), Timestamps.now(clock)));
        }
        return store.catalogue(userId);
    }

    /**
     * Archives a project (remembering its status and unpinning it) or brings it back with the status it had,
     * moving it to the end of the section it joins.
     */
    @Transactional
    public ProjectCatalogue setArchived(UUID userId, String projectId, boolean archived) {
        var before = store.projects(userId);
        var current = store.require(before, projectId);
        if (current.archived() != archived) {
            store.saveChanged(userId, before, ProjectOrdering.transition(before, projectId,
                    project -> archived ? archive(project) : unarchive(project), Timestamps.now(clock)));
        }
        return store.catalogue(userId);
    }

    /**
     * Puts a section in the given order. The IDs must be exactly the section's projects; otherwise the
     * caller's view is out of date and nothing changes.
     */
    @Transactional
    public ProjectCatalogue reorder(UUID userId, String sectionName, List<String> orderedIds) {
        var section = CatalogueSection.parse(sectionName);
        if (section == null) throw ApiException.badRequest("invalid_section", "This catalogue section does not exist.");
        var before = store.projects(userId);
        var current = ProjectOrdering.sectionIds(before, section);
        if (orderedIds.size() != current.size() || !new HashSet<>(orderedIds).equals(new HashSet<>(current))) {
            throw ApiException.conflict("project_order_changed",
                    "The catalogue changed since it was shown; reload and try again.");
        }
        store.saveChanged(userId, before,
                ProjectOrdering.applySectionOrder(before, section, orderedIds, Timestamps.now(clock)));
        return store.catalogue(userId);
    }

    /**
     * Deletes one project with its wiki pages, after the caller confirmed it; the section it left is
     * renumbered. Tasks that pointed at it keep existing in the app.
     */
    @Transactional
    public ProjectCatalogue delete(UUID userId, String projectId, boolean confirmed) {
        if (!confirmed) {
            throw ApiException.badRequest("confirmation_required", "Confirm deleting this project.");
        }
        var before = store.projects(userId);
        var deleted = store.require(before, projectId);
        projects.deleteById(new UserScopedId(userId, deleted.id()));
        var remaining = before.stream().filter(project -> !project.id().equals(projectId)).toList();
        var section = CatalogueSection.of(deleted);
        store.saveChanged(userId, remaining, ProjectOrdering.applySectionOrder(remaining, section,
                ProjectOrdering.sectionIds(remaining, section), Timestamps.now(clock)));
        return store.catalogue(userId);
    }

    private Project archive(Project project) {
        return project.toBuilder().statusBeforeArchive(project.status()).status(Project.ARCHIVED).pinned(false).build();
    }

    private Project unarchive(Project project) {
        var restored = project.statusBeforeArchive() == null ? normalizer.normalize(project.toBuilder().status(null).build())
                .status() : project.statusBeforeArchive();
        return project.toBuilder().status(restored).statusBeforeArchive(null).pinned(false).build();
    }

    private static List<Project> replace(List<Project> catalogue, Project updated) {
        return catalogue.stream().map(project -> project.id().equals(updated.id()) ? updated : project).toList();
    }
}
