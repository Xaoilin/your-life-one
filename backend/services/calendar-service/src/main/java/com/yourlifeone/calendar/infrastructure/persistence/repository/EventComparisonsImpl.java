package com.yourlifeone.calendar.infrastructure.persistence.repository;

import com.yourlifeone.calendar.infrastructure.persistence.model.EventComparisonValue;
import com.yourlifeone.calendar.infrastructure.persistence.model.EventDifference;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

/** Exact, null-safe comparison without transferring unchanged stored event bodies to the service. */
class EventComparisonsImpl implements EventComparisons {
    static final String DIFFERENCES = """
            WITH incoming AS MATERIALIZED (
                SELECT * FROM jsonb_to_recordset(CAST(:fetched AS jsonb))
                    AS f(id text, title text, description text, location text,
                        "allDay" boolean, "startsAt" timestamptz, "endsAt" timestamptz)
            )
            SELECT e.id AS stored_id, f.id AS google_id
            FROM incoming f LEFT JOIN calendar.event e
                ON e.user_id = :userId AND e.source_id = :sourceId AND e.google_event_id = f.id
            WHERE e.id IS NULL OR
                ROW(e.title, e.description, e.location, e.all_day, e.starts_at, e.ends_at)
                IS DISTINCT FROM
                ROW(f.title, f.description, f.location, f."allDay", f."startsAt", f."endsAt")
            UNION ALL
            SELECT e.id AS stored_id, NULL AS google_id FROM calendar.event e
            WHERE e.user_id = :userId AND e.source_id = :sourceId
                AND e.starts_at < :to AND e.ends_at >= :from
                AND NOT EXISTS (SELECT 1 FROM incoming f WHERE f.id = e.google_event_id)
            """;
    private final JdbcClient jdbc;
    private final JsonMapper json;

    EventComparisonsImpl(DataSource dataSource, JsonMapper json) {
        jdbc = JdbcClient.create(dataSource);
        this.json = json;
    }

    @Override
    public List<EventDifference> differences(UUID userId, String sourceId,
            Collection<EventComparisonValue> fetched, Instant from, Instant to) {
        return jdbc.sql(DIFFERENCES).param("userId", userId).param("sourceId", sourceId)
                .param("fetched", json.writeValueAsString(fetched))
                .param("from", OffsetDateTime.ofInstant(from, ZoneOffset.UTC))
                .param("to", OffsetDateTime.ofInstant(to, ZoneOffset.UTC))
                .query((row, index) -> new EventDifference(row.getString("stored_id"), row.getString("google_id")))
                .list();
    }
}
