package com.yourlifeone.knowledge.projects.application.service;

import com.yourlifeone.knowledge.projects.domain.Project;
import com.yourlifeone.knowledge.projects.domain.ProjectPage;

/** The overview page every new project starts with: its name, summary and a skeleton to fill in. */
public final class OverviewPages {
    public static final String TITLE = "Overview";
    private static final String TEMPLATE = """
            # %s

            %s## Goals

            - Add milestones for this project.

            ## Notes

            - Keep architecture decisions and links here.

            ## Next Steps

            - Capture the next 1-3 actions on the board.""";
    private static final String PARAGRAPH_BREAK = "\n\n";

    private OverviewPages() {
    }

    public static ProjectPage forProject(String pageId, Project project) {
        var intro = project.summary().isEmpty() ? "" : project.summary() + PARAGRAPH_BREAK;
        return ProjectPage.builder()
                .id(pageId)
                .projectId(project.id())
                .title(TITLE)
                .content(TEMPLATE.formatted(project.name(), intro))
                .overview(true)
                .createdAt(project.createdAt())
                .updatedAt(project.updatedAt())
                .build();
    }
}
