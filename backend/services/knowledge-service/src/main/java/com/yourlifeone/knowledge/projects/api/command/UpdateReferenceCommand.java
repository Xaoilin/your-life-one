package com.yourlifeone.knowledge.projects.api.command;

import com.yourlifeone.knowledge.projects.api.request.ProjectReferenceRequest;

/** Replaces what one project shows. The queued payload carries the ID from the request path. */
public record UpdateReferenceCommand(String id, ProjectReferenceRequest reference) {
}
