package com.yourlifeone.knowledge.projects.domain;

import java.util.Locale;

/** The catalogue's sections, in display order. */
public enum CatalogueSection {
    PINNED, PROJECTS, ARCHIVED;

    public static CatalogueSection of(Project project) {
        if (Project.ARCHIVED.equals(project.status())) return ARCHIVED;
        return project.pinned() ? PINNED : PROJECTS;
    }

    /** The section named in a request, e.g. {@code pinned}; null when there is no such section. */
    public static CatalogueSection parse(String name) {
        for (var section : values()) {
            if (section.name().toLowerCase(Locale.ROOT).equals(name)) return section;
        }
        return null;
    }
}
