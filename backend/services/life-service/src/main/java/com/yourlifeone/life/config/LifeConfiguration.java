package com.yourlifeone.life.config;

import com.yourlifeone.commonservice.persistence.SchemaMigrator;
import com.yourlifeone.commonservice.security.SupabaseResourceServerConfiguration;
import com.yourlifeone.life.infrastructure.database.AgentApprovalRepository;
import com.yourlifeone.life.infrastructure.database.LifeDatabaseHealthCheck;
import com.yourlifeone.life.infrastructure.persistence.LifeSchemaMigrationInterceptor;
import com.yourlifeone.life.infrastructure.persistence.LifeSchemas;
import com.yourlifeone.life.security.AgentAccessInterceptor;
import java.time.Clock;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * One service, one PostgreSQL schema per domain ({@code inventory}, {@code trips}, {@code health},
 * {@code jobs}) so each can move to its own service later, plus {@code life} for the shared command
 * inbox. Each schema has its own Flyway history.
 */
@Configuration(proxyBeanMethods = false)
@Import(SupabaseResourceServerConfiguration.class)
@EnableConfigurationProperties(AgentAccessProperties.class)
public class LifeConfiguration implements WebMvcConfigurer {
    private static final List<String> SCHEMAS = List.of("life", "health", "jobs", "inventory", "trips");

    private final LifeSchemas schemas;
    private final AgentApprovalRepository approvals;
    private final AgentAccessProperties agentAccess;

    public LifeConfiguration(DataSource dataSource, JdbcClient jdbc, AgentAccessProperties agentAccess) {
        this.schemas = new LifeSchemas(SCHEMAS.stream().map(schema -> new SchemaMigrator(dataSource, schema)).toList());
        this.approvals = new AgentApprovalRepository(jdbc);
        this.agentAccess = agentAccess;
    }

    @Bean
    LifeSchemas lifeSchemas() {
        return schemas;
    }

    /** Agents are checked first; then every data request makes sure the schemas are migrated. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AgentAccessInterceptor(agentAccess, approvals)).addPathPatterns("/api/life/v1/**");
        registry.addInterceptor(new LifeSchemaMigrationInterceptor(schemas)).addPathPatterns("/api/life/v1/**")
                // Approval probes never need business schemas, including on a cold first request.
                .excludePathPatterns("/api/life/v1/inventory/access", "/api/life/v1/jobs/access");
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    LifeDatabaseHealthCheck lifeDatabaseHealthCheck(JdbcTemplate jdbcTemplate) {
        return new LifeDatabaseHealthCheck(jdbcTemplate);
    }
}
