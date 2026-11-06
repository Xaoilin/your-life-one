package com.yourlifeone.prayer.application.service;

import com.yourlifeone.commonservice.persistence.DatabaseUnavailableException;
import com.yourlifeone.commonservice.persistence.SchemaMigrator;
import com.yourlifeone.prayer.infrastructure.persistence.repository.PrayerProfileRepository;
import java.time.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Decides every reminder on the server and sends each to the user's open tabs once: deadline reminders
 * ({@code prayer.reminder}), prayer opportunities and momentum prompts ({@code prayer.notice}), and a
 * snoozed reminder once more when its snooze ends. The app shows each as a notification and a banner.
 */
@Component
@RequiredArgsConstructor
public class ReminderScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ReminderScheduler.class);

    private final PrayerProfileRepository profiles;
    private final ReminderEvaluationService evaluation;
    private final ReminderEvents events;
    private final SchemaMigrator migrator;
    private final Clock clock;
    private final MeterRegistry meters;
    private final AtomicLong lastSuccess = new AtomicLong();

    @PostConstruct
    void bindHealth() {
        Gauge.builder("yourlife.job.last.success.timestamp", lastSuccess, AtomicLong::doubleValue)
                .tag("task", "prayer-reminders").register(meters);
    }

    @Scheduled(fixedDelayString = "${app.prayer.reminder-check-interval}",
            initialDelayString = "${app.prayer.reminder-check-interval}")
    public void sendDueReminders() {
        try {
            migrator.ensureMigrated();
            var now = clock.instant();
            for (var userId : profiles.findEligibleUserIds()) {
                evaluation.claimDue(userId, now).forEach(reminder -> events.publish(userId, reminder));
            }
            lastSuccess.set(clock.instant().getEpochSecond());
            meters.counter("yourlife.job.runs", "task", "prayer-reminders", "outcome", "ok").increment();
        } catch (DatabaseUnavailableException | DataAccessException exception) {
            meters.counter("yourlife.job.runs", "task", "prayer-reminders", "outcome", "failed").increment();
            logger.warn("Prayer reminders skipped: {}", exception.getMessage());
        }
    }
}
