package com.yourlifeone.knowledge.projects.api.response;

import java.util.List;

/** Projects matching a lookup, by name. */
public record ProjectMatchesResponse(List<ProjectMatchResponse> projects) {
}
