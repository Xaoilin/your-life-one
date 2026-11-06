package com.yourlifeone.knowledge.projects.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

/**
 * Setup guidance shown on a project. Display-only: never passed to a shell.
 *
 * @param displayCode code shown to copy; null when there is none
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectSetupStep(String id, String title, String description, String displayCode) {
}
