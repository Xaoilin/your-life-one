package com.yourlifeone.commonservice.changes;

import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import tools.jackson.databind.JsonNode;

/**
 * Publishes change events after a change is committed. Best effort: open tabs also reload when they
 * regain focus or reconnect, so a lost event only delays convergence and never fails the change itself.
 */
public class ChangePublisher implements ApplicationEventPublisherAware {
    private static final Logger logger = LoggerFactory.getLogger(ChangePublisher.class);

    private final RabbitTemplate rabbit;
    private final ChangeProperties properties;
    private final Clock clock;
    private ApplicationEventPublisher localEvents;

    public ChangePublisher(RabbitTemplate rabbit, ChangeProperties properties, Clock clock) {
        this.rabbit = rabbit;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.localEvents = publisher;
    }

    public void publish(UUID userId, String type) {
        publish(userId, type, null);
    }

    public void publish(UUID userId, String type, JsonNode data) {
        publish(ChangeEvent.of(userId, type, clock.instant(), data));
    }

    public void publish(ChangeEvent event) {
        publishLocally(event);
        try {
            rabbit.convertAndSend(properties.exchange(), event.type(), event);
        } catch (AmqpException exception) {
            logger.warn("Change event not published: type={} reason={}", event.type(), exception.getMessage());
        }
    }

    /** Local snapshot invalidation must still run when the remote broker is unavailable. */
    private void publishLocally(ChangeEvent event) {
        if (localEvents == null) return;
        try {
            localEvents.publishEvent(event);
        } catch (RuntimeException exception) {
            logger.warn("Local change event not published: type={} exceptionType={}", event.type(),
                    exception.getClass().getSimpleName());
        }
    }
}
