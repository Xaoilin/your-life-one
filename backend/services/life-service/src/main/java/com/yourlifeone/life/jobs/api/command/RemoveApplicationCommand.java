package com.yourlifeone.life.jobs.api.command;

import java.time.Instant;

/**
 * Removes one application and its history.
 *
 * @param expectedUpdatedAt when set, refused unless the application is still this version
 */
public record RemoveApplicationCommand(String applicationId, Instant expectedUpdatedAt) {
}
