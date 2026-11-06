package com.yourlifeone.commonservice.command;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WriteCommandPropertiesTest {
    @Test
    void defaultsToAnEightSecondWaitOneConsumerAndThirtyDaysOfMemory() {
        var properties = new WriteCommandProperties("yourlife.test.commands", "test.processed_command", null, null, null);

        assertEquals(Duration.ofSeconds(8), properties.replyTimeout());
        assertEquals(1, properties.consumers());
        assertEquals(Duration.ofDays(30), properties.retention());
    }

    @Test
    void refusesARetentionThatWouldForgetAWriteWhileItsRequestIsStillWaiting() {
        assertThrows(IllegalArgumentException.class, () -> new WriteCommandProperties(
                "yourlife.test.commands", "test.processed_command", Duration.ofSeconds(8), 1, Duration.ofSeconds(8)));
    }

    @Test
    void refusesATableNameThatIsNotSchemaQualified() {
        assertThrows(IllegalArgumentException.class, () -> new WriteCommandProperties(
                "yourlife.test.commands", "processed_command; drop table x", null, null, null));
    }

    @Test
    void refusesNoQueueOrNoConsumers() {
        assertThrows(IllegalArgumentException.class, () -> new WriteCommandProperties(
                " ", "test.processed_command", null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new WriteCommandProperties(
                "yourlife.test.commands", "test.processed_command", null, 0, null));
    }
}
