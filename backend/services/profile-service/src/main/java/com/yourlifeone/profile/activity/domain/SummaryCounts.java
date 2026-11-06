package com.yourlifeone.profile.activity.domain;

import lombok.Builder;

/**
 * Raw counts over the filtered current window.
 *
 * @param events matching events
 * @param sessions distinct sessions among them
 * @param surfaces distinct named surfaces among them
 * @param errors error events plus events whose outcome is a failure
 * @param outcomes events that report an outcome
 */
@Builder
public record SummaryCounts(long events, long sessions, long surfaces, long errors, long outcomes) {
}
