package com.yourlifeone.life.jobs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row of {@code jobs.history_entry}. */
@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryEntryEmbeddable {
    private String id;
    private String kind;
    @Column(name = "entry_date")
    private LocalDate date;
    private String summary;
    private String details;
    private String evidenceUrl;
}
