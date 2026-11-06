package com.yourlifeone.calendar.config;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Calendar numbers for the Grafana business dashboard, per visible calendar. */
@Configuration(proxyBeanMethods = false)
public class CalendarMetricsConfiguration {
    /** What is coming up: events starting in the next 30 days. */
    @Bean
    BusinessGauge upcomingEventsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.calendar.events.next30days", "Events starting in the next 30 days",
                () -> jdbc.sql("""
                        SELECT s.name AS calendar, count(*) AS total
                        FROM calendar.event e
                        JOIN calendar.source s ON s.user_id = e.user_id AND s.id = e.source_id
                        WHERE s.visible AND e.starts_at >= now() AND e.starts_at < now() + interval '30 days'
                        GROUP BY s.name""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"), "calendar", rs.getString("calendar")))
                        .list());
    }

    /** Where the time goes: hours of timed events in the next 7 days (all-day events excluded). */
    @Bean
    BusinessGauge busyHoursGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.calendar.busy.hours.next7days", "Hours of timed events in the next 7 days",
                () -> jdbc.sql("""
                        SELECT s.name AS calendar,
                               sum(extract(epoch FROM least(e.ends_at, now() + interval '7 days')
                                   - greatest(e.starts_at, now()))) / 3600 AS hours
                        FROM calendar.event e
                        JOIN calendar.source s ON s.user_id = e.user_id AND s.id = e.source_id
                        WHERE s.visible AND NOT e.all_day
                          AND e.ends_at > now() AND e.starts_at < now() + interval '7 days'
                        GROUP BY s.name""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getDouble("hours"), "calendar", rs.getString("calendar")))
                        .list());
    }

    /**
     * Whether stored Google credentials still work, by status: {@code refreshable},
     * {@code needs_reconnect} (Google refused the credential) or {@code revoked}.
     */
    @Bean
    BusinessGauge googleCredentialsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.calendar.google.credentials", "Stored Google credentials by status",
                () -> jdbc.sql("""
                        SELECT CASE WHEN revoked_at IS NOT NULL THEN 'revoked'
                                    WHEN last_failure_at IS NOT NULL THEN 'needs_reconnect'
                                    ELSE 'refreshable' END AS status,
                               count(*) AS total
                        FROM calendar.google_credential
                        GROUP BY 1""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"), "status", rs.getString("status")))
                        .list());
    }
}
