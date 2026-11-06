package com.yourlifeone.knowledge.config;

import com.yourlifeone.commonservice.persistence.SchemaMigrator;
import com.yourlifeone.commonservice.security.SupabaseResourceServerConfiguration;
import com.yourlifeone.knowledge.infrastructure.database.AgentApprovalRepository;
import com.yourlifeone.knowledge.infrastructure.database.KnowledgeDatabaseHealthCheck;
import com.yourlifeone.knowledge.infrastructure.persistence.KnowledgeSchemaMigrationInterceptor;
import com.yourlifeone.knowledge.infrastructure.persistence.KnowledgeSchemas;
import com.yourlifeone.knowledge.security.AgentAccessInterceptor;
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
 * One service, one PostgreSQL schema per domain ({@code knowledge}, {@code lifestyle}, {@code projects})
 * so each can move to its own service later, plus {@code knowledge_service} for the shared command
 * inbox. Each schema has its own Flyway history.
 */
@Configuration(proxyBeanMethods = false)
@Import(SupabaseResourceServerConfiguration.class)
@EnableConfigurationProperties(AgentAccessProperties.class)
public class KnowledgeServiceConfiguration implements WebMvcConfigurer {
    private static final List<String> SCHEMAS = List.of("knowledge_service", "knowledge", "lifestyle", "projects");
    private static final String DATA_PATHS = "/api/knowledge/v1/**";

    private final KnowledgeSchemas schemas;
    private final AgentApprovalRepository approvals;
    private final AgentAccessProperties agentAccess;

    public KnowledgeServiceConfiguration(DataSource dataSource, JdbcClient jdbc, AgentAccessProperties agentAccess) {
        this.schemas = new KnowledgeSchemas(SCHEMAS.stream().map(schema -> new SchemaMigrator(dataSource, schema)).toList());
        this.approvals = new AgentApprovalRepository(jdbc);
        this.agentAccess = agentAccess;
    }

    @Bean
    KnowledgeSchemas knowledgeSchemas() {
        return schemas;
    }

    /** Agents are checked first; then every data request makes sure the schemas are migrated. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AgentAccessInterceptor(agentAccess, approvals)).addPathPatterns(DATA_PATHS);
        registry.addInterceptor(new KnowledgeSchemaMigrationInterceptor(schemas)).addPathPatterns(DATA_PATHS);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    KnowledgeDatabaseHealthCheck knowledgeDatabaseHealthCheck(JdbcTemplate jdbcTemplate) {
        return new KnowledgeDatabaseHealthCheck(jdbcTemplate);
    }
}
