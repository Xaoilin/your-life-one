package com.yourlifeone.live.infrastructure.messaging;

import com.yourlifeone.commonservice.changes.ChangeEvent;
import com.yourlifeone.live.application.service.LiveStreams;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Forwards each change event on this server to its user's open streams. */
@Component
@RequiredArgsConstructor
public class ChangeEventListener {
    private final LiveStreams streams;

    @RabbitListener(queues = "#{liveEventQueue.name}")
    public void receive(ChangeEvent event) {
        streams.deliver(event);
    }
}
