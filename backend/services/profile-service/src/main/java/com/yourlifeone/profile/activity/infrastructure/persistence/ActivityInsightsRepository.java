package com.yourlifeone.profile.activity.infrastructure.persistence;

import com.yourlifeone.profile.activity.domain.ActivityFilters;
import com.yourlifeone.profile.activity.domain.ErrorSummary;
import com.yourlifeone.profile.activity.domain.PathCount;
import com.yourlifeone.profile.activity.domain.SessionProgress;
import com.yourlifeone.profile.activity.domain.SummaryCounts;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Counts a user's product-usage events for the Activity page, aggregated in SQL. Every read applies the
 * same filters; a null filter matches every event. Windows include their start and exclude their end.
 */
@Repository
@RequiredArgsConstructor
public class ActivityInsightsRepository {
    private static final String MATCHING = """
            user_id = :userId
              AND (CAST(:kind AS text) IS NULL OR event_kind = CAST(:kind AS text))
              AND (CAST(:surface AS text) IS NULL OR surface = CAST(:surface AS text))
              AND (CAST(:outcome AS text) IS NULL OR outcome = CAST(:outcome AS text))
              AND (CAST(:feature AS text) IS NULL OR feature = CAST(:feature AS text))
            """;
    private static final String ERROR_EVENT = "(event_kind = 'error' OR outcome = 'failure')";

    private final JdbcClient jdbc;

    public long countAll(UUID userId) {
        return jdbc.sql("SELECT count(*) FROM activity.product_usage_event WHERE user_id = :userId")
                .param("userId", userId).query(Long.class).single();
    }

    public List<String> features(UUID userId) {
        return jdbc.sql("SELECT DISTINCT feature FROM activity.product_usage_event WHERE user_id = :userId")
                .param("userId", userId).query(String.class).list();
    }

    public SummaryCounts summary(UUID userId, ActivityFilters filters, Instant from, Instant to) {
        return filtered("""
                SELECT count(*) AS events, count(DISTINCT session_id) AS sessions, count(DISTINCT surface) AS surfaces,
                       count(*) FILTER (WHERE %s) AS errors, count(*) FILTER (WHERE outcome IS NOT NULL) AS outcomes
                FROM activity.product_usage_event
                WHERE %s AND occurred_at >= :from AND occurred_at < :to""".formatted(ERROR_EVENT, MATCHING),
                userId, filters, from, to)
                .query((rs, row) -> SummaryCounts.builder()
                        .events(rs.getLong("events"))
                        .sessions(rs.getLong("sessions"))
                        .surfaces(rs.getLong("surfaces"))
                        .errors(rs.getLong("errors"))
                        .outcomes(rs.getLong("outcomes"))
                        .build())
                .single();
    }

    /** Each path's count in {@code [split, to)} (current) and {@code [from, split)} (previous). */
    public List<PathCount> paths(UUID userId, ActivityFilters filters, Instant from, Instant split, Instant to) {
        return filtered("""
                SELECT feature, action,
                       count(*) FILTER (WHERE occurred_at >= :split) AS current_count,
                       count(*) FILTER (WHERE occurred_at < :split) AS previous_count
                FROM activity.product_usage_event
                WHERE %s AND occurred_at >= :from AND occurred_at < :to
                GROUP BY feature, action""".formatted(MATCHING), userId, filters, from, to)
                .param("split", split.atOffset(ZoneOffset.UTC))
                .query((rs, row) -> PathCount.builder()
                        .feature(rs.getString("feature"))
                        .action(rs.getString("action"))
                        .currentCount(rs.getLong("current_count"))
                        .previousCount(rs.getLong("previous_count"))
                        .build())
                .list();
    }

    public SessionProgress sessionProgress(UUID userId, ActivityFilters filters, Instant from, Instant to) {
        return filtered("""
                SELECT count(*) FILTER (WHERE started) AS started,
                       count(*) FILTER (WHERE started AND viewed) AS viewed,
                       count(*) FILTER (WHERE started AND opened) AS opened
                FROM (SELECT bool_or(event_kind = 'session') AS started,
                             bool_or(event_kind = 'navigation' AND action = 'viewed') AS viewed,
                             bool_or(event_kind = 'outcome' AND action = 'surface_opened' AND outcome = 'success')
                                 AS opened
                      FROM activity.product_usage_event
                      WHERE %s AND occurred_at >= :from AND occurred_at < :to
                      GROUP BY session_id) AS per_session""".formatted(MATCHING), userId, filters, from, to)
                .query((rs, row) -> new SessionProgress(rs.getLong("started"), rs.getLong("viewed"),
                        rs.getLong("opened")))
                .single();
    }

    /** Error events grouped by code (the action when there is none) and surface ({@code noSurface} when none). */
    public List<ErrorSummary> errors(UUID userId, ActivityFilters filters, Instant from, Instant to, String noSurface) {
        return filtered("""
                SELECT coalesce(error_code, action) AS code, coalesce(surface, :noSurface) AS surface, count(*) AS total
                FROM activity.product_usage_event
                WHERE %s AND %s AND occurred_at >= :from AND occurred_at < :to
                GROUP BY 1, 2""".formatted(MATCHING, ERROR_EVENT), userId, filters, from, to)
                .param("noSurface", noSurface)
                .query((rs, row) -> new ErrorSummary(rs.getString("code"), rs.getLong("total"), rs.getString("surface")))
                .list();
    }

    private JdbcClient.StatementSpec filtered(String sql, UUID userId, ActivityFilters filters, Instant from,
            Instant to) {
        return jdbc.sql(sql)
                .param("userId", userId)
                .param("kind", filters.kind())
                .param("surface", filters.surface())
                .param("outcome", filters.outcome())
                .param("feature", filters.feature())
                .param("from", from.atOffset(ZoneOffset.UTC))
                .param("to", to.atOffset(ZoneOffset.UTC));
    }
}
