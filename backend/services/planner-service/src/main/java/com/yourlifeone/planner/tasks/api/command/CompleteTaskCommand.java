package com.yourlifeone.planner.tasks.api.command;

import com.yourlifeone.planner.gamification.domain.KnowledgeProgress;
import com.yourlifeone.planner.tasks.api.request.CompleteTaskRequest;

/**
 * Completes one task. The queued payload carries the ID from the request path and the knowledge counts the
 * API fetched for the badges (null when the knowledge service did not answer: those badges wait).
 */
public record CompleteTaskCommand(String id, CompleteTaskRequest completion, KnowledgeProgress knowledge) {
}
