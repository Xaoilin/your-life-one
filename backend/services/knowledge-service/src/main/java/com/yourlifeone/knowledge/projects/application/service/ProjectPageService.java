package com.yourlifeone.knowledge.projects.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.knowledge.projects.config.ProjectsProperties;
import com.yourlifeone.knowledge.projects.domain.ProjectPage;
import com.yourlifeone.knowledge.projects.infrastructure.persistence.mapper.ProjectsPersistenceMapper;
import com.yourlifeone.knowledge.projects.infrastructure.persistence.repository.PageRepository;
import com.yourlifeone.knowledge.projects.infrastructure.persistence.repository.ProjectRepository;
import com.yourlifeone.knowledge.shared.RecordIds;
import com.yourlifeone.knowledge.shared.Texts;
import com.yourlifeone.knowledge.shared.Timestamps;
import com.yourlifeone.knowledge.shared.UserScopedId;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Project wiki pages. A save replaces one page; each project keeps exactly one overview page. */
@Service
@RequiredArgsConstructor
public class ProjectPageService {
    private final PageRepository pages;
    private final ProjectRepository projects;
    private final ProjectsPersistenceMapper persistence;
    private final ProjectsProperties properties;
    private final Clock clock;

    /** Creates or replaces a page. Making it the overview makes the project's previous overview a normal page. */
    @Transactional
    public ProjectPage save(UUID userId, ProjectPage requested) {
        var key = new UserScopedId(userId, RecordIds.require(requested.id()));
        if (!projects.existsById(new UserScopedId(userId, RecordIds.require(requested.projectId())))) {
            throw ApiException.notFound("project_not_found", "The page's project does not exist.");
        }
        var existing = pages.findById(key);
        if (existing.isPresent() && !existing.get().getProjectId().equals(requested.projectId())) {
            throw ApiException.conflict("page_project_changed", "A page cannot move to another project.");
        }
        if (existing.isPresent() && existing.get().isOverview() && !requested.overview()) {
            throw ApiException.conflict("overview_page_required", "Make another page the overview first.");
        }
        var now = Timestamps.now(clock);
        if (requested.overview()) demoteOtherOverviews(userId, requested, now);
        var page = requested.toBuilder()
                .title(Texts.orDefault(requested.title(), properties.defaultPageTitle()))
                .content(requested.content() == null ? "" : requested.content())
                .createdAt(existing.map(entity -> entity.getCreatedAt()).orElse(now))
                .updatedAt(now)
                .build();
        return persistence.toDomain(pages.save(persistence.toEntity(userId, page)));
    }

    /** Deletes one page. The overview page stays: every project has one. */
    @Transactional
    public void delete(UUID userId, String id) {
        var page = pages.findById(new UserScopedId(userId, RecordIds.require(id)))
                .orElseThrow(() -> ApiException.notFound("page_not_found", "This page does not exist."));
        if (page.isOverview()) {
            throw ApiException.conflict("overview_page_required", "A project's overview page cannot be deleted.");
        }
        pages.delete(page);
    }

    private void demoteOtherOverviews(UUID userId, ProjectPage requested, Instant now) {
        var others = pages.findByUserIdAndProjectIdAndOverviewTrue(userId, requested.projectId()).stream()
                .filter(page -> !page.getId().equals(requested.id())).toList();
        others.forEach(page -> {
            page.setOverview(false);
            page.setUpdatedAt(now);
        });
        pages.saveAllAndFlush(others);
    }
}
