package com.yourlifeone.profile.config;

import com.yourlifeone.commonservice.persistence.SchemaMigrator;
import com.yourlifeone.commonservice.security.SupabaseResourceServerConfiguration;
import com.yourlifeone.profile.infrastructure.database.ProfileDatabaseHealthCheck;
import com.yourlifeone.profile.infrastructure.persistence.ProfileSchemaMigrationInterceptor;
import com.yourlifeone.profile.infrastructure.persistence.ProfileSchemas;
import java.time.Clock;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The profile service owns two PostgreSQL schemas, each with its own Flyway history: {@code profile}
 * (settings, integrations and the command inbox) and {@code activity} (product-usage events).
 */
@Configuration(proxyBeanMethods = false)
@Import(SupabaseResourceServerConfiguration.class)
@EnableConfigurationProperties({
        ProfileProperties.class, PreferencesProperties.class, ViewPreferencesProperties.class,
        IntegrationProperties.class, TelemetryProperties.class})
public class ProfileConfiguration implements WebMvcConfigurer {
    private static final List<String> SCHEMAS = List.of("profile", "activity");

    private final ProfileSchemas schemas;

    public ProfileConfiguration(DataSource dataSource) {
        this.schemas = new ProfileSchemas(SCHEMAS.stream().map(schema -> new SchemaMigrator(dataSource, schema)).toList());
    }

    @Bean
    ProfileSchemas profileSchemas() {
        return schemas;
    }

    /**
     * Every data request first makes sure the schemas are migrated. Operational events use no
     * database, so they keep flowing (and can report) while it is unavailable.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ProfileSchemaMigrationInterceptor(schemas))
                .addPathPatterns("/api/profile/v1/**")
                .excludePathPatterns("/api/profile/v1/operational-events");
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ProfileDatabaseHealthCheck profileDatabaseHealthCheck(JdbcTemplate jdbcTemplate) {
        return new ProfileDatabaseHealthCheck(jdbcTemplate);
    }
}
