package com.yourlifeone.knowledge.projects.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

/**
 * A web link shown on a project, such as its repository or live deployment.
 *
 * @param kind one of the configured link kinds, e.g. {@code repository}
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectLink(String id, String kind, String label, String url) {
}
