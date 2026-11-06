package com.yourlifeone.profile.activity.domain;

/**
 * Sessions in the filtered current window that started, and how many of those went on to each step.
 *
 * @param started sessions with a {@code session} event
 * @param viewed started sessions with a surface view
 * @param opened started sessions with a successful surface open
 */
public record SessionProgress(long started, long viewed, long opened) {
}
