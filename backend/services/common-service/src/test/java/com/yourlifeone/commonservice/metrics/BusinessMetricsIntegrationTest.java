package com.yourlifeone.commonservice.metrics;

import com.yourlifeone.commonservice.autoconfigure.BusinessMetricsAutoConfiguration;
import com.yourlifeone.commonservice.changes.ChangeProperties;
import com.yourlifeone.commonservice.changes.ChangePublisher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.Connection;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Exercises real Spring event wiring and transaction commit/rollback, without a remote broker. */
class BusinessMetricsIntegrationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BusinessMetricsAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues("app.business-metrics.initial-delay=PT1H");

    @Test
    void committedChangesInvalidateOnlyAfterTheTransactionCommits() {
        context.run(started -> {
            var publisher = started.getBean(BusinessGaugePublisher.class);
            var changes = started.getBean(ChangePublisher.class);
            var meters = started.getBean(SimpleMeterRegistry.class);
            publisher.refresh();

            new TransactionTemplate(started.getBean(DataSourceTransactionManager.class))
                    .executeWithoutResult(status -> {
                        changes.publish(UUID.randomUUID(), "test.saved");
                        publisher.refresh();
                        assertThat(meters.get("yourlife.business.metrics.reads").counter().count()).isEqualTo(1);
                    });
            publisher.refresh();

            assertThat(meters.get("yourlife.business.metrics.reads").counter().count()).isEqualTo(2);
        });
    }

    @Test
    void rollbackDoesNotCauseAnotherDatabaseRead() {
        context.run(started -> {
            var publisher = started.getBean(BusinessGaugePublisher.class);
            publisher.refresh();

            new TransactionTemplate(started.getBean(DataSourceTransactionManager.class))
                    .executeWithoutResult(status -> {
                        started.getBean(ChangePublisher.class).publish(UUID.randomUUID(), "test.saved");
                        status.setRollbackOnly();
                    });
            publisher.refresh();

            assertThat(started.getBean(SimpleMeterRegistry.class)
                    .get("yourlife.business.metrics.reads").counter().count()).isEqualTo(1);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TestConfiguration {
        @Bean
        SimpleMeterRegistry meters() {
            return new SimpleMeterRegistry();
        }

        @Bean
        BusinessGauge gauge() {
            return new BusinessGauge("yourlife.test.integration", "Integration", () -> List.of(BusinessGauge.Row.of(1)));
        }

        @Bean
        ChangePublisher changes() {
            return new ChangePublisher(mock(RabbitTemplate.class), new ChangeProperties(null), Clock.systemUTC());
        }

        @Bean
        DataSourceTransactionManager transactions() throws Exception {
            var connection = mock(Connection.class);
            when(connection.getAutoCommit()).thenReturn(true);
            var source = mock(DataSource.class);
            when(source.getConnection()).thenReturn(connection);
            return new DataSourceTransactionManager(source);
        }
    }
}
