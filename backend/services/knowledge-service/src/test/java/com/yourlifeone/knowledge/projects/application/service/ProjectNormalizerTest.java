package com.yourlifeone.knowledge.projects.application.service;

import com.yourlifeone.knowledge.projects.config.ProjectsProperties;
import com.yourlifeone.knowledge.projects.domain.Project;
import com.yourlifeone.knowledge.projects.domain.ProjectPreview;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectNormalizerTest {
    private static final ProjectPreview DEFAULT_PREVIEW = new ProjectPreview("folder", "#7c6cff", "#171827", null);
    private static final ProjectsProperties PROPERTIES = new ProjectsProperties(
            List.of("planning", "active", "blocked", "completed", "archived"), List.of("web_app", "other"),
            List.of("repository", "other"), "other", "other", "active", "New Project", "Untitled Page",
            DEFAULT_PREVIEW, 10, 50, 50, 20, 25);
    private final ProjectNormalizer normalizer = new ProjectNormalizer(PROPERTIES);

    @Test
    void fallsBackToDefaultsAndNeverPinsAnArchivedProject() {
        var project = normalizer.normalize(Project.builder().id("p").name(" ").kind("rocket").status("archived")
                .statusBeforeArchive("archived").pinned(true).tags(List.of(" a ", "a", "")).build());

        assertEquals("custom:p", project.catalogKey());
        assertEquals("New Project", project.name());
        assertEquals("other", project.kind());
        assertNull(project.statusBeforeArchive());
        assertFalse(project.pinned());
        assertEquals(List.of("a"), project.tags());
        assertEquals(DEFAULT_PREVIEW, project.preview());
    }

    @Test
    void recognisesAbsolutePathsOnEverySystem() {
        assertTrue(ProjectNormalizer.isAbsolutePath("/usr/bin/npm"));
        assertTrue(ProjectNormalizer.isAbsolutePath("C:\\tools\\npm.exe"));
        assertTrue(ProjectNormalizer.isAbsolutePath("\\\\server\\share"));
        assertFalse(ProjectNormalizer.isAbsolutePath("npm"));
    }
}
