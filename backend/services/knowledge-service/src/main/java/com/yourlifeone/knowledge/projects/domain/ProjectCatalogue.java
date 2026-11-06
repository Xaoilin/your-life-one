package com.yourlifeone.knowledge.projects.domain;

import java.util.List;

/** Every project in catalogue order (pinned, then the rest, then archived) and every wiki page. */
public record ProjectCatalogue(List<Project> projects, List<ProjectPage> pages) {
}
