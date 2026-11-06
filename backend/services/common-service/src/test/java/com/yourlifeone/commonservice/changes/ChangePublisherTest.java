package com.yourlifeone.commonservice.changes;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChangePublisherTest {
    private static final Instant NOW = Instant.parse("2026-09-27T08:00:00Z");

    @Mock
    private RabbitTemplate rabbit;

    private ChangePublisher publisher() {
        return new ChangePublisher(rabbit, new ChangeProperties(null), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void routesByTypeOnTheChangesExchangeWithTheDomainOfTheType() {
        var user = UUID.randomUUID();

        publisher().publish(user, "lifestyle.save-item");

        var event = ArgumentCaptor.forClass(Object.class);
        verify(rabbit).convertAndSend(eq("yourlife.changes"), eq("lifestyle.save-item"), event.capture());
        assertEquals(new ChangeEvent(user, "lifestyle.save-item", "lifestyle", NOW, null), event.getValue());
    }

    @Test
    void anUnreachableBrokerNeverFailsTheChange() {
        doThrow(new AmqpConnectException(new RuntimeException("down")))
                .when(rabbit).convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertDoesNotThrow(() -> publisher().publish(UUID.randomUUID(), "tasks.save-task"));
    }

    @Test
    void localInvalidationStillHappensWhenTheBrokerIsUnavailable() {
        var local = mock(ApplicationEventPublisher.class);
        var publisher = publisher();
        publisher.setApplicationEventPublisher(local);
        var event = ChangeEvent.of(UUID.randomUUID(), "tasks.save-task", NOW, null);
        doThrow(new AmqpConnectException(new RuntimeException("down")))
                .when(rabbit).convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertDoesNotThrow(() -> publisher.publish(event));

        verify(local).publishEvent(event);
    }
}
