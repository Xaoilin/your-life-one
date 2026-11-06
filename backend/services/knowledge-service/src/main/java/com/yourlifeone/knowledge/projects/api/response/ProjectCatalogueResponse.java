package com.yourlifeone.knowledge.projects.api.response;

import java.util.List;

/** Every project in catalogue order (pinned, then the rest, then archived) and every wiki page. */
public record ProjectCatalogueResponse(List<ProjectResponse> projects, List<PageResponse> pages) {
}
