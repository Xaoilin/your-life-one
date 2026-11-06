package com.yourlifeone.knowledge.projects.api.command;

/**
 * Deletes one project with its pages. The queued payload carries the ID from the path.
 *
 * @param confirmed the caller confirmed the delete ({@code confirm=true})
 */
public record DeleteProjectCommand(String id, boolean confirmed) {
}
