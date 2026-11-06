package com.yourlifeone.knowledge.projects.application.service;

import com.yourlifeone.commonservice.web.ApiException;
import com.yourlifeone.knowledge.projects.domain.Project;
import com.yourlifeone.knowledge.projects.domain.ProjectMatch;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectResolverTest {
    private static final int LIMIT = 10;
    private static final List<Project> CATALOGUE = List.of(
            Project.builder().id("1").catalogKey("catalog:magnus").name("Magnus Saddle").tags(List.of("desk")).build(),
            Project.builder().id("2").catalogKey("custom:2").name("Agent Board").tags(List.of("White-Boarding")).build());

    @Test
    void matchesNameContainsKeyEqualsOrCompactTagEquals() {
        assertEquals(List.of(new ProjectMatch("1", "catalog:magnus", "Magnus Saddle")),
                ProjectResolver.resolve(CATALOGUE, "saddle", LIMIT));
        assertEquals("2", ProjectResolver.resolve(CATALOGUE, "CUSTOM:2", LIMIT).getFirst().id());
        assertEquals("2", ProjectResolver.resolve(CATALOGUE, "white boarding", LIMIT).getFirst().id());
        assertEquals(List.of(), ProjectResolver.resolve(CATALOGUE, "catalog", LIMIT));
    }

    @Test
    void refusesQueriesWithoutALetterOrDigitOrWithSurroundingSpace() {
        assertThrows(ApiException.class, () -> ProjectResolver.resolve(CATALOGUE, "--", LIMIT));
        assertThrows(ApiException.class, () -> ProjectResolver.resolve(CATALOGUE, " magnus", LIMIT));
    }
}
