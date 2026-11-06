package com.yourlifeone.knowledge.projects.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

/**
 * How a project's card looks.
 *
 * @param accentColor hex colour such as {@code #7c6cff}
 * @param coverImageUrl web address of a cover image; null when there is none
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectPreview(String icon, String accentColor, String backgroundColor, String coverImageUrl) {
}
