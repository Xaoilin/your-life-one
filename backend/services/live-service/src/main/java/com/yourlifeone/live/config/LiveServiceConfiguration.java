package com.yourlifeone.live.config;

import com.yourlifeone.commonservice.security.SupabaseResourceServerConfiguration;
import java.time.Clock;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The gateway listens to every change event on this server's RabbitMQ through its own temporary queue (it
 * goes when the gateway stops, so nothing piles up while no tab is listening), and serves streams to
 * signed-in users only.
 */
@Configuration(proxyBeanMethods = false)
@Import(SupabaseResourceServerConfiguration.class)
@EnableConfigurationProperties(LiveProperties.class)
@EnableScheduling
public class LiveServiceConfiguration {
    /** Every routing key: the gateway forwards all of a user's events to that user's streams. */
    private static final String ALL_EVENTS = "#";

    @Bean
    AnonymousQueue liveEventQueue() {
        return new AnonymousQueue();
    }

    @Bean
    Binding liveEventBinding(AnonymousQueue liveEventQueue, TopicExchange changeEventExchange) {
        return BindingBuilder.bind(liveEventQueue).to(changeEventExchange).with(ALL_EVENTS);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
