package com.yourlifeone.profile.activity.domain;

/**
 * How often one coded error occurred on one surface.
 *
 * @param code the error code, or the action when the event has none
 * @param surface the surface, or {@code application} for application-wide events
 */
public record ErrorSummary(String code, long count, String surface) {
}
