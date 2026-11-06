package com.yourlifeone.planner.infrastructure.upstream.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

/** The knowledge service's {@code GET /api/knowledge/v1/progress}. */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record KnowledgeProgressDto(int entries, int topics, int haramMastered, int halalConsistent, int lifestyleItems) {
}
