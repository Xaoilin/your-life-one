package com.yourlifeone.knowledge.projects.config;

import com.yourlifeone.knowledge.projects.domain.ProjectPreview;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Project catalogue rules ({@code app.knowledge.projects.*}).
 *
 * @param statuses allowed statuses, including {@code archived}
 * @param kinds allowed project kinds, e.g. {@code web_app}
 * @param linkKinds allowed link kinds, e.g. {@code repository}
 * @param defaultKind kind of a project that names none (or an unknown one)
 * @param defaultLinkKind kind of a link that names none (or an unknown one)
 * @param defaultStatus status of a project that names none (or an unknown one)
 * @param defaultName name of a project saved without one
 * @param defaultPageTitle title of a wiki page saved without one
 * @param defaultPreview how a project's card looks until it is styled
 * @param maxResolveResults the most projects a name lookup returns
 */
@ConfigurationProperties("app.knowledge.projects")
public record ProjectsProperties(
        List<String> statuses,
        List<String> kinds,
        List<String> linkKinds,
        String defaultKind,
        String defaultLinkKind,
        String defaultStatus,
        String defaultName,
        String defaultPageTitle,
        ProjectPreview defaultPreview,
        int maxResolveResults,
        int maxLinks,
        int maxSetupSteps,
        int maxRunRecipes,
        int maxTags) {
    public ProjectsProperties {
        statuses = copy(statuses);
        kinds = copy(kinds);
        linkKinds = copy(linkKinds);
        if (!kinds.contains(defaultKind) || !linkKinds.contains(defaultLinkKind) || !statuses.contains(defaultStatus) || defaultPreview == null) {
            throw new IllegalArgumentException("app.knowledge.projects defaults must be allowed values");
        }
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
