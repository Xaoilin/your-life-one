package com.yourlifeone.knowledge.projects.api.command;

import com.yourlifeone.knowledge.projects.api.request.ProjectStatusRequest;

/** Sets one project's status. The queued payload carries the ID from the request path. */
public record SetStatusCommand(String id, ProjectStatusRequest status) {
}
