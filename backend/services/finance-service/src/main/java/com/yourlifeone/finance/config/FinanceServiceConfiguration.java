package com.yourlifeone.finance.config;

import com.yourlifeone.commonservice.persistence.SchemaMigrator;
import com.yourlifeone.commonservice.security.SupabaseResourceServerConfiguration;
import com.yourlifeone.finance.infrastructure.database.AgentApprovalRepository;
import com.yourlifeone.finance.infrastructure.database.FinanceDatabaseHealthCheck;
import com.yourlifeone.finance.infrastructure.persistence.FinanceSchemaMigrationInterceptor;
import com.yourlifeone.finance.infrastructure.persistence.FinanceSchemas;
import com.yourlifeone.finance.security.AgentAccessInterceptor;
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
 * One service, one PostgreSQL schema per domain ({@code finance} for the ledger and the banking review,
 * {@code equity} for positions) so each can move to its own service later, plus {@code finance_service}
 * for the shared command inbox. Each schema has its own Flyway history.
 */
@Configuration(proxyBeanMethods = false)
@Import(SupabaseResourceServerConfiguration.class)
@EnableConfigurationProperties(AgentAccessProperties.class)
public class FinanceServiceConfiguration implements WebMvcConfigurer {
    private static final List<String> SCHEMAS = List.of("finance_service", "finance", "equity");
    private static final String DATA_PATHS = "/api/finance/v1/**";

    private final FinanceSchemas schemas;
    private final AgentApprovalRepository approvals;
    private final AgentAccessProperties agentAccess;

    public FinanceServiceConfiguration(DataSource dataSource, JdbcClient jdbc, AgentAccessProperties agentAccess) {
        this.schemas = new FinanceSchemas(SCHEMAS.stream().map(schema -> new SchemaMigrator(dataSource, schema)).toList());
        this.approvals = new AgentApprovalRepository(jdbc);
        this.agentAccess = agentAccess;
    }

    @Bean
    FinanceSchemas financeSchemas() {
        return schemas;
    }

    /** Agents are checked first; then every data request makes sure the schemas are migrated. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AgentAccessInterceptor(agentAccess, approvals)).addPathPatterns(DATA_PATHS);
        registry.addInterceptor(new FinanceSchemaMigrationInterceptor(schemas)).addPathPatterns(DATA_PATHS)
                // Approval probes never need business schemas, including on a cold first request.
                .excludePathPatterns("/api/finance/v1/review/access", "/api/finance/v1/equity/access");
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    FinanceDatabaseHealthCheck financeDatabaseHealthCheck(JdbcTemplate jdbcTemplate) {
        return new FinanceDatabaseHealthCheck(jdbcTemplate);
    }
}
