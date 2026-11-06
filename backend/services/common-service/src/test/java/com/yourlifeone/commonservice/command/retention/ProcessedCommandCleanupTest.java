package com.yourlifeone.commonservice.command.retention;

import com.yourlifeone.commonservice.command.WriteCommandProperties;
import com.yourlifeone.commonservice.command.persistence.ProcessedCommandRepository;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessedCommandCleanupTest {
    private static final WriteCommandProperties PROPERTIES = new WriteCommandProperties(
            "yourlife.test.commands", "test.processed_command", null, null, Duration.ofDays(30));

    @Mock
    private ProcessedCommandRepository processed;

    @Test
    void deletesWritesOlderThanTheRetention() {
        new ProcessedCommandCleanup(processed, PROPERTIES).deleteExpired();

        verify(processed).deleteOlderThan(Duration.ofDays(30));
    }

    @Test
    void aDatabaseOutageOnlyPostponesTheCleanup() {
        when(processed.deleteOlderThan(Duration.ofDays(30))).thenThrow(new DataAccessResourceFailureException("down"));

        assertDoesNotThrow(() -> new ProcessedCommandCleanup(processed, PROPERTIES).deleteExpired());
    }
}
