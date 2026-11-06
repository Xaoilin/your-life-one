package com.yourlifeone.knowledge.projects.application.service;

import com.yourlifeone.knowledge.projects.config.ProjectsProperties;
import com.yourlifeone.knowledge.projects.domain.Project;
import com.yourlifeone.knowledge.projects.domain.ProjectLink;
import com.yourlifeone.knowledge.projects.domain.ProjectPreview;
import com.yourlifeone.knowledge.projects.domain.ProjectRunRecipe;
import com.yourlifeone.knowledge.projects.domain.ProjectSetupStep;
import com.yourlifeone.knowledge.shared.Texts;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Tidies a project the way the catalogue shows it: unknown kinds, statuses and link kinds fall back to
 * defaults, only web links are kept, setup steps and run recipes stay display-only (no absolute paths,
 * no secret-looking environment variables), and every listed part gets a stable ID.
 */
@Component
@RequiredArgsConstructor
public class ProjectNormalizer {
    private static final String CUSTOM_CATALOG_PREFIX = "custom:";
    private static final String LINK_ID = ":link:";
    private static final String SETUP_ID = ":setup:";
    private static final String RUN_ID = ":run:";
    private static final String LINK_LABEL = "Link ";
    private static final String STEP_TITLE = "Step ";
    private static final String SERVICE_MODE = "service";
    private static final String ONE_SHOT_MODE = "one_shot";
    private static final String PARENT_DIRECTORY = "..";
    private static final int MAX_ICON_LENGTH = 40;
    private static final Pattern COLOR = Pattern.compile("^#[0-9a-f]{6}(?:[0-9a-f]{2})?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENVIRONMENT_KEY = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
    private static final Pattern SENSITIVE_ENVIRONMENT_KEY = Pattern.compile(
            "(api.?key|access.?token|refresh.?token|secret|password|credential|private.?key)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile("^[A-Za-z]:[\\\\/].*");

    private final ProjectsProperties properties;

    /** The project with every field tidied; status and pin state stay consistent (archived is never pinned). */
    public Project normalize(Project project) {
        var catalogKey = catalogKey(project.catalogKey(), project.id());
        var status = allowedOr(project.status(), properties.statuses(), properties.defaultStatus());
        var archived = Project.ARCHIVED.equals(status);
        var beforeArchive = archived && project.statusBeforeArchive() != null
                && !Project.ARCHIVED.equals(project.statusBeforeArchive())
                && properties.statuses().contains(project.statusBeforeArchive()) ? project.statusBeforeArchive() : null;
        return project.toBuilder()
                .catalogKey(catalogKey)
                .name(Texts.orDefault(project.name(), properties.defaultName()))
                .kind(allowedOr(project.kind(), properties.kinds(), properties.defaultKind()))
                .links(links(project.links(), catalogKey))
                .setupSteps(setupSteps(project.setupSteps(), catalogKey))
                .runRecipes(runRecipes(project.runRecipes(), catalogKey))
                .preview(preview(project.preview()))
                .summary(Texts.trimmed(project.summary()))
                .status(status)
                .statusBeforeArchive(beforeArchive)
                .tags(Texts.distinctTrimmed(project.tags()))
                .pinned(!archived && project.pinned())
                .build();
    }

    static boolean isAbsolutePath(String value) {
        var candidate = Texts.trimmed(value);
        return candidate.startsWith("/") || candidate.startsWith("\\\\") || WINDOWS_ABSOLUTE_PATH.matcher(candidate).matches();
    }

    private static String catalogKey(String requested, String projectId) {
        return Texts.orDefault(requested, CUSTOM_CATALOG_PREFIX + projectId);
    }

    private static String allowedOr(String value, List<String> allowed, String fallback) {
        return value != null && allowed.contains(value) ? value : fallback;
    }

    private List<ProjectLink> links(List<ProjectLink> links, String catalogKey) {
        var kept = new ArrayList<ProjectLink>();
        for (int index = 0; index < size(links); index++) {
            var link = links.get(index);
            var url = link == null ? null : WebUrls.normalize(link.url());
            if (url == null) continue;
            var number = index + 1;
            kept.add(new ProjectLink(
                    Texts.orDefault(link.id(), catalogKey + LINK_ID + number),
                    allowedOr(link.kind(), properties.linkKinds(), properties.defaultLinkKind()),
                    Texts.orDefault(link.label(), LINK_LABEL + number),
                    url));
        }
        return kept;
    }

    private static List<ProjectSetupStep> setupSteps(List<ProjectSetupStep> steps, String catalogKey) {
        var kept = new ArrayList<ProjectSetupStep>();
        for (int index = 0; index < size(steps); index++) {
            var step = steps.get(index);
            if (step == null) continue;
            var title = Texts.trimmed(step.title());
            var description = Texts.trimmed(step.description());
            var displayCode = Texts.blankToNull(step.displayCode());
            if (title.isEmpty() && description.isEmpty() && displayCode == null) continue;
            var number = index + 1;
            kept.add(new ProjectSetupStep(
                    Texts.orDefault(step.id(), catalogKey + SETUP_ID + number),
                    title.isEmpty() ? STEP_TITLE + number : title,
                    description,
                    displayCode));
        }
        return kept;
    }

    private static List<ProjectRunRecipe> runRecipes(List<ProjectRunRecipe> recipes, String catalogKey) {
        var kept = new ArrayList<ProjectRunRecipe>();
        for (int index = 0; index < size(recipes); index++) {
            var recipe = recipes.get(index);
            var executable = recipe == null ? "" : Texts.trimmed(recipe.executable());
            if (executable.isEmpty() || isAbsolutePath(executable)) continue;
            var args = recipe.args() == null ? List.<String>of() : recipe.args().stream().filter(arg -> arg != null).toList();
            var prerequisites = Texts.distinctTrimmed(recipe.prerequisites());
            var number = index + 1;
            kept.add(ProjectRunRecipe.builder()
                    .id(Texts.orDefault(recipe.id(), catalogKey + RUN_ID + number))
                    .label(Texts.orDefault(recipe.label(), executable))
                    .displayCommand(Texts.orDefault(recipe.displayCommand(), joined(executable, args)))
                    .executable(executable)
                    .args(args)
                    .workingDirectory(relativeDirectory(recipe.workingDirectory()))
                    .environment(environment(recipe.environment()))
                    .localUrl(WebUrls.normalize(recipe.localUrl()))
                    .prerequisites(prerequisites.isEmpty() ? null : prerequisites)
                    .mode(SERVICE_MODE.equals(recipe.mode()) ? SERVICE_MODE : ONE_SHOT_MODE)
                    .build());
        }
        return kept;
    }

    private static String joined(String executable, List<String> args) {
        var parts = new ArrayList<String>();
        parts.add(executable);
        parts.addAll(args);
        return String.join(" ", parts);
    }

    private static String relativeDirectory(String value) {
        var candidate = Texts.blankToNull(value);
        if (candidate == null || isAbsolutePath(candidate)) return null;
        if (candidate.equals(PARENT_DIRECTORY) || candidate.startsWith(PARENT_DIRECTORY + "/")
                || candidate.startsWith(PARENT_DIRECTORY + "\\")) {
            return null;
        }
        return candidate;
    }

    private static Map<String, String> environment(Map<String, String> environment) {
        if (environment == null) return null;
        var kept = new LinkedHashMap<String, String>();
        environment.forEach((key, value) -> {
            if (key != null && value != null && ENVIRONMENT_KEY.matcher(key).matches()
                    && !SENSITIVE_ENVIRONMENT_KEY.matcher(key).find()) {
                kept.put(key, value);
            }
        });
        return kept.isEmpty() ? null : kept;
    }

    private ProjectPreview preview(ProjectPreview preview) {
        var defaults = properties.defaultPreview();
        if (preview == null) return defaults;
        var icon = Texts.trimmed(preview.icon());
        return new ProjectPreview(
                icon.isEmpty() ? defaults.icon() : icon.substring(0, Math.min(icon.length(), MAX_ICON_LENGTH)),
                color(preview.accentColor(), defaults.accentColor()),
                color(preview.backgroundColor(), defaults.backgroundColor()),
                WebUrls.normalize(preview.coverImageUrl()));
    }

    private static String color(String value, String fallback) {
        var candidate = Texts.trimmed(value);
        return COLOR.matcher(candidate).matches() ? candidate.toLowerCase(Locale.ROOT) : fallback;
    }

    private static int size(List<?> values) {
        return values == null ? 0 : values.size();
    }
}
