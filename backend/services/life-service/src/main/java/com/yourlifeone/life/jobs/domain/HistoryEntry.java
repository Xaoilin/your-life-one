package com.yourlifeone.life.jobs.domain;

import java.time.LocalDate;
import lombok.Builder;

/**
 * One piece of sourced evidence about an application.
 *
 * @param date the confirmed local date; null when the source gave none
 * @param evidenceUrl link to the email or job page; null when there is none
 */
@Builder(toBuilder = true)
public record HistoryEntry(String id, String kind, LocalDate date, String summary, String details, String evidenceUrl) {
}
