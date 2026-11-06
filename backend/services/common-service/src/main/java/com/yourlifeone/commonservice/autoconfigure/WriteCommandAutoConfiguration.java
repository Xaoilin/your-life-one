package com.yourlifeone.commonservice.autoconfigure;

import com.yourlifeone.commonservice.changes.ChangePublisher;
import com.yourlifeone.commonservice.command.WriteCommandHandler;
import com.yourlifeone.commonservice.command.WriteCommandProperties;
import com.yourlifeone.commonservice.command.messaging.WriteCommandExecutor;
import com.yourlifeone.commonservice.command.messaging.WriteCommandGateway;
import com.yourlifeone.commonservice.command.messaging.WriteCommandListener;
import com.yourlifeone.commonservice.command.persistence.ProcessedCommandRepository;
import com.yourlifeone.commonservice.command.retention.ProcessedCommandCleanup;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.json.JsonMapper;

/**
 * Routes a service's writes through RabbitMQ when it sets {@code app.write-commands.queue}:
 * the API publishes each write and waits; one consumer per queue slot performs it exactly once.
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(prefix = "app.write-commands", name = "queue")
@EnableConfigurationProperties(WriteCommandProperties.class)
@EnableScheduling
public class WriteCommandAutoConfiguration {
    @Bean
    Queue writeCommandQueue(WriteCommandProperties properties) {
        return QueueBuilder.durable(properties.queue()).build();
    }

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    MessageConverter writeCommandMessageConverter(JsonMapper json) {
        return new JacksonJsonMessageConverter(json);
    }

    @Bean
    RabbitTemplateCustomizer writeCommandReplyTimeout(WriteCommandProperties properties) {
        return template -> template.setReplyTimeout(properties.replyTimeout().toMillis());
    }

    @Bean
    ProcessedCommandRepository processedCommandRepository(JdbcClient jdbc, WriteCommandProperties properties) {
        return new ProcessedCommandRepository(jdbc, properties);
    }

    @Bean
    WriteCommandExecutor writeCommandExecutor(List<WriteCommandHandler<?>> handlers,
            ProcessedCommandRepository processed, JsonMapper json) {
        return new WriteCommandExecutor(handlers, processed, json);
    }

    @Bean
    WriteCommandListener writeCommandListener(WriteCommandExecutor executor, ObjectProvider<MeterRegistry> meters,
            ObjectProvider<ChangePublisher> changes) {
        return new WriteCommandListener(executor, meters.getIfAvailable(SimpleMeterRegistry::new),
                changes.getIfAvailable());
    }

    @Bean
    ProcessedCommandCleanup processedCommandCleanup(ProcessedCommandRepository processed,
            WriteCommandProperties properties) {
        return new ProcessedCommandCleanup(processed, properties);
    }

    @Bean
    WriteCommandGateway writeCommandGateway(RabbitTemplate rabbit, ProcessedCommandRepository processed,
            WriteCommandProperties properties, JsonMapper json) {
        return new WriteCommandGateway(rabbit, processed, properties, json);
    }
}
