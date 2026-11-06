package com.yourlifeone.commonservice.autoconfigure;

import com.yourlifeone.commonservice.changes.ChangeProperties;
import com.yourlifeone.commonservice.changes.ChangePublisher;
import java.time.Clock;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

/**
 * Change events for every service that talks to RabbitMQ: the {@code yourlife.changes} topic exchange
 * (declared on first connection) and a publisher. Saved writes publish through it automatically.
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
@EnableConfigurationProperties(ChangeProperties.class)
public class ChangeEventAutoConfiguration {
    @Bean
    TopicExchange changeEventExchange(ChangeProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    MessageConverter changeEventMessageConverter(JsonMapper json) {
        return new JacksonJsonMessageConverter(json);
    }

    @Bean
    @ConditionalOnMissingBean
    ChangePublisher changePublisher(RabbitTemplate rabbit, ChangeProperties properties) {
        return new ChangePublisher(rabbit, properties, Clock.systemUTC());
    }
}
