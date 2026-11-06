package com.yourlifeone.knowledge.projects.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * How a project is run, shown as portable guidance. Display-only: never executed. The working directory
 * is relative and the environment never holds secrets.
 *
 * @param mode {@code service} for long-running processes, else {@code one_shot}
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectRunRecipe(
        String id,
        String label,
        String displayCommand,
        String executable,
        List<String> args,
        String workingDirectory,
        Map<String, String> environment,
        String localUrl,
        List<String> prerequisites,
        String mode) {
}
