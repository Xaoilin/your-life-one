package com.yourlifeone.knowledge.projects.application.service;

import com.yourlifeone.knowledge.projects.domain.CatalogueSection;
import com.yourlifeone.knowledge.projects.domain.Project;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectOrderingTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final Instant BEFORE = Instant.parse("2026-09-01T12:00:00Z");

    private static Project project(String id, String name, boolean pinned, String status, Integer sortOrder) {
        return Project.builder().id(id).name(name).pinned(pinned).status(status).sortOrder(sortOrder)
                .updatedAt(BEFORE).build();
    }

    private static List<String> ids(List<Project> projects) {
        return projects.stream().map(project -> project.id() + ":" + project.sortOrder()).toList();
    }

    @Test
    void ordersPinnedThenProjectsThenArchivedWithUnnumberedProjectsByName() {
        var catalogue = List.of(
                project("z", "Zeta", false, "active", null),
                project("b", "beta", false, "active", 1),
                project("x", "Old", false, "archived", 0),
                project("a", "Alpha", false, "active", null),
                project("p", "Pinned", true, "active", 0),
                project("c", "Gamma", false, "active", 0));

        assertEquals(List.of("p:0", "c:0", "b:1", "a:null", "z:null", "x:0"), ids(ProjectOrdering.inCatalogueOrder(catalogue)));
    }

    @Test
    void appendsANewProjectToTheEndOfItsSection() {
        var catalogue = List.of(project("a", "A", false, "active", 0), project("b", "B", false, "active", 1));

        var next = ProjectOrdering.append(catalogue, project("n", "New", false, "active", null), NOW);

        assertEquals(List.of("a:0", "b:1", "n:2"), ids(next));
    }

    @Test
    void movingBetweenSectionsRenumbersTheOldSectionAndStampsOnlyWhatChanged() {
        var catalogue = List.of(project("a", "A", false, "active", 0), project("b", "B", false, "active", 1),
                project("c", "C", false, "active", 2), project("p", "P", true, "active", 0));

        var next = ProjectOrdering.transition(catalogue, "a", project -> project.toBuilder().pinned(true).build(), NOW);

        assertEquals(List.of("p:0", "a:1", "b:0", "c:1"), ids(next));
        assertEquals(NOW, next.get(2).updatedAt());
        assertEquals(BEFORE, next.getFirst().updatedAt());
        assertEquals(List.of("b", "c"), ProjectOrdering.sectionIds(next, CatalogueSection.PROJECTS));
    }
}
