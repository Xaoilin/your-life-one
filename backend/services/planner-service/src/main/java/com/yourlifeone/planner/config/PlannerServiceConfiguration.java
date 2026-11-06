package com.yourlifeone.planner.config;

import com.yourlifeone.commonservice.persistence.SchemaMigrator;
import com.yourlifeone.commonservice.security.SupabaseResourceServerConfiguration;
import com.yourlifeone.planner.infrastructure.database.PlannerDatabaseHealthCheck;
import com.yourlifeone.planner.infrastructure.persistence.PlannerSchemaMigrationInterceptor;
import com.yourlifeone.planner.infrastructure.persistence.PlannerSchemas;
import java.time.Clock;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * One service, one PostgreSQL schema per domain ({@code tasks}, {@code gamification}, {@code clock}) so
 * each can move to its own service later, plus {@code planner_service} for the shared command inbox.
 * Each schema has its own Flyway history.
 */
@Configuration(proxyBeanMethods = false)
@Import(SupabaseResourceServerConfiguration.class)
public class PlannerServiceConfiguration implements WebMvcConfigurer {
    private static final List<String> SCHEMAS = List.of("planner_service", "tasks", "gamification", "clock");
    private static final String DATA_PATHS = "/api/planner/v1/**";

    private final PlannerSchemas schemas;

    public PlannerServiceConfiguration(DataSource dataSource) {
        this.schemas = new PlannerSchemas(SCHEMAS.stream().map(schema -> new SchemaMigrator(dataSource, schema)).toList());
    }

    @Bean
    PlannerSchemas plannerSchemas() {
        return schemas;
    }

    /** Every data request makes sure the schemas are migrated. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PlannerSchemaMigrationInterceptor(schemas)).addPathPatterns(DATA_PATHS);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PlannerDatabaseHealthCheck plannerDatabaseHealthCheck(JdbcTemplate jdbcTemplate) {
        return new PlannerDatabaseHealthCheck(jdbcTemplate);
    }
}
