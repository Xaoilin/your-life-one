package com.yourlifeone.calendar.infrastructure.persistence.model;

import lombok.Builder;

/** Provider values for exact comparison inside PostgreSQL; timestamps use ISO instants. */
@Builder
public record EventComparisonValue(String id, String title, String description, String location,
        boolean allDay, String startsAt, String endsAt) {
}
