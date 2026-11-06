package com.yourlifeone.profile.activity.application.insight;

import com.yourlifeone.profile.activity.domain.FunnelStage;
import com.yourlifeone.profile.activity.domain.SessionProgress;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FunnelTest {
    private final Funnel funnel = new Funnel();

    @Test
    void showsNoFunnelWithoutStartedSessions() {
        assertEquals(List.of(), funnel.stages(new SessionProgress(0, 0, 0)));
    }

    @Test
    void reportsEachStepAsAShareOfStartedSessions() {
        var stages = funnel.stages(new SessionProgress(3, 2, 1));

        assertEquals(List.of(
                new FunnelStage("sessions", "Sessions started", 3, 100),
                new FunnelStage("surface_views", "Sessions with a surface view", 2, 67),
                new FunnelStage("successful_opens", "Sessions with a successful surface open", 1, 33)), stages);
    }
}
