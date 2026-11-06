package com.yourlifeone.knowledge.projects.application.service;

import com.yourlifeone.knowledge.projects.domain.CatalogueSection;
import com.yourlifeone.knowledge.projects.domain.Project;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Catalogue order. Projects sit in three sections (pinned, projects, archived), each numbered from 0 in
 * the order the user arranged; projects without a position follow, by name. Moving a project between
 * sections renumbers the section it left and appends it to the one it joins. Every operation returns the
 * whole catalogue in order; a project whose position or state changed is stamped {@code now}.
 */
public final class ProjectOrdering {
    private static final Comparator<Project> BY_NAME = Comparator
            .comparing((Project project) -> project.name().trim().toLowerCase(Locale.ROOT))
            .thenComparing(Project::id);
    public static final Comparator<Project> SECTION_ORDER = Comparator
            .comparing((Project project) -> project.sortOrder() == null)
            .thenComparing(project -> project.sortOrder() == null ? 0 : project.sortOrder())
            .thenComparing(BY_NAME);
    private static final Comparator<Project> CATALOGUE_ORDER = Comparator
            .comparing(CatalogueSection::of)
            .thenComparing(SECTION_ORDER);

    private ProjectOrdering() {
    }

    public static List<Project> inCatalogueOrder(List<Project> projects) {
        return projects.stream().sorted(CATALOGUE_ORDER).toList();
    }

    public static List<String> sectionIds(List<Project> projects, CatalogueSection section) {
        return projects.stream().filter(project -> CatalogueSection.of(project) == section).sorted(SECTION_ORDER)
                .map(Project::id).toList();
    }

    /** Adds a new project at the end of its section. */
    public static List<Project> append(List<Project> projects, Project added, Instant now) {
        var next = new ArrayList<>(projects);
        next.add(added);
        var section = CatalogueSection.of(added);
        var ids = new ArrayList<>(sectionIds(projects, section));
        ids.add(added.id());
        return applySectionOrder(next, section, ids, now);
    }

    /**
     * Applies a change to one project that may move it to another section (pin, unpin, archive,
     * unarchive): the section it left is renumbered and it goes to the end of the one it joins.
     */
    public static List<Project> transition(List<Project> projects, String projectId, UnaryOperator<Project> change,
            Instant now) {
        var before = projects.stream().filter(project -> project.id().equals(projectId)).findFirst().orElseThrow();
        var changed = change.apply(before).toBuilder().updatedAt(now).build();
        var next = projects.stream().map(project -> project.id().equals(projectId) ? changed : project).toList();
        var from = CatalogueSection.of(before);
        var to = CatalogueSection.of(changed);
        if (from != to) next = applySectionOrder(next, from, sectionIds(next, from), now);
        var destination = new ArrayList<>(sectionIds(next, to).stream().filter(id -> !id.equals(projectId)).toList());
        destination.add(projectId);
        return applySectionOrder(next, to, destination, now);
    }

    /** Numbers a section's projects by their place in {@code orderedIds}, which must list the whole section. */
    public static List<Project> applySectionOrder(List<Project> projects, CatalogueSection section,
            List<String> orderedIds, Instant now) {
        var positions = new HashMap<String, Integer>();
        for (int index = 0; index < orderedIds.size(); index++) positions.put(orderedIds.get(index), index);
        var next = projects.stream().map(project -> {
            var position = positions.get(project.id());
            if (CatalogueSection.of(project) != section || position == null || position.equals(project.sortOrder())) {
                return project;
            }
            return project.toBuilder().sortOrder(position).updatedAt(now).build();
        }).toList();
        return inCatalogueOrder(next);
    }
}
