package com.yourlifeone.life.jobs.api.command;

import com.yourlifeone.life.jobs.api.request.ApplicationPatchRequest;

/** Changes one application; the queued payload carries the ID from the request path. */
public record UpdateApplicationCommand(String applicationId, ApplicationPatchRequest patch) {
}
