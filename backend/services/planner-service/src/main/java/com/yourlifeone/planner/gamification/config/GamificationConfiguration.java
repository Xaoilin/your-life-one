package com.yourlifeone.planner.gamification.config;

import com.yourlifeone.commonservice.metrics.BusinessGauge;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Progress settings and the numbers for the Grafana business dashboard. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MomentumProperties.class)
public class GamificationConfiguration {
    /** Where the user stands: XP, level, streaks and badges (the highest across users; there is one). */
    @Bean
    BusinessGauge progressGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.gamification.progress", "XP, level, streaks and badges",
                () -> jdbc.sql("""
                        SELECT coalesce(max(total_xp), 0) AS xp, coalesce(max(level), 0) AS level,
                               coalesce(max(current_streak), 0) AS streak, coalesce(max(longest_streak), 0) AS longest,
                               coalesce(max(cardinality(badges)), 0) AS badges
                        FROM gamification.profile""")
                        .query((rs, row) -> List.of(
                                BusinessGauge.Row.of(rs.getLong("xp"), "measure", "xp"),
                                BusinessGauge.Row.of(rs.getLong("level"), "measure", "level"),
                                BusinessGauge.Row.of(rs.getLong("streak"), "measure", "current_streak"),
                                BusinessGauge.Row.of(rs.getLong("longest"), "measure", "longest_streak"),
                                BusinessGauge.Row.of(rs.getLong("badges"), "measure", "badges")))
                        .single());
    }

    /** Prayers rewarded with XP over the last month, by prayer. */
    @Bean
    BusinessGauge prayerRewardsGauge(JdbcClient jdbc) {
        return new BusinessGauge("yourlife.gamification.prayer.rewards.last30days", "Prayers rewarded in the last 30 days",
                () -> jdbc.sql("""
                        SELECT prayer_name, count(*) AS total
                        FROM gamification.prayer_reward
                        WHERE prayer_date > current_date - 30
                        GROUP BY prayer_name""")
                        .query((rs, row) -> BusinessGauge.Row.of(rs.getLong("total"), "prayer", rs.getString("prayer_name")))
                        .list());
    }
}
