package com.yourlifeone.knowledge.projects.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.knowledge.projects.domain.Project;
import com.yourlifeone.knowledge.projects.domain.ProjectMatch;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Finds projects for a name an agent or user typed, so Inventory can link records to the project's
 * catalogue key: the name contains the query, the catalogue key equals it, or a tag equals it ignoring
 * case, spaces and punctuation.
 */
public final class ProjectResolver {
    private static final int MAX_QUERY_LENGTH = 160;
    private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private ProjectResolver() {
    }

    /** The first {@code limit} matches, by name. */
    public static List<ProjectMatch> resolve(List<Project> projects, String query, int limit) {
        if (query == null || !query.equals(query.trim()) || query.isEmpty() || query.length() > MAX_QUERY_LENGTH) {
            throw invalidQuery();
        }
        var lower = query.toLowerCase(Locale.ROOT);
        var compact = compact(query);
        if (compact.isEmpty()) throw invalidQuery();
        return projects.stream()
                .filter(project -> project.name().toLowerCase(Locale.ROOT).contains(lower)
                        || project.catalogKey().toLowerCase(Locale.ROOT).equals(lower)
                        || project.tags().stream().anyMatch(tag -> compact(tag).equals(compact)))
                .sorted(Comparator.comparing((Project project) -> project.name().toLowerCase(Locale.ROOT)))
                .limit(limit)
                .map(project -> new ProjectMatch(project.id(), project.catalogKey(), project.name()))
                .toList();
    }

    private static String compact(String value) {
        return NOT_ALPHANUMERIC.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private static ApiException invalidQuery() {
        return ApiException.badRequest("invalid_query", "The project query must be 1-160 characters with a letter or digit.");
    }
}
