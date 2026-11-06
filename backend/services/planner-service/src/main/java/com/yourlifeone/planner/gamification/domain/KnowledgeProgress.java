package com.yourlifeone.planner.gamification.domain;

/** The knowledge base and lifestyle tracker totals the Islamic badges count. */
public record KnowledgeProgress(
        int knowledgeEntries,
        int knowledgeTopics,
        int lifestyleHaramMastered,
        int lifestyleHalalConsistent,
        int lifestyleTotal) {

    public static KnowledgeProgress none() {
        return new KnowledgeProgress(0, 0, 0, 0, 0);
    }
}
