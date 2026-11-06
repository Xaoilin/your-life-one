package com.yourlifeone.profile.activity.application.insight;

import com.yourlifeone.profile.activity.domain.FunnelStage;
import com.yourlifeone.profile.activity.domain.SessionProgress;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * How far started sessions get: started, viewed a surface, then opened a surface successfully. Only
 * sessions that started in the window count at each step; no funnel is shown without any.
 */
@Component
public class Funnel {
    public static final String SESSIONS = "sessions";
    public static final String SURFACE_VIEWS = "surface_views";
    public static final String SUCCESSFUL_OPENS = "successful_opens";
    private static final int ALL = 100;

    public List<FunnelStage> stages(SessionProgress progress) {
        if (progress.started() == 0) return List.of();
        return List.of(
                stage(SESSIONS, "Sessions started", progress.started(), ALL),
                stage(SURFACE_VIEWS, "Sessions with a surface view", progress.viewed(),
                        Percentages.of(progress.viewed(), progress.started())),
                stage(SUCCESSFUL_OPENS, "Sessions with a successful surface open", progress.opened(),
                        Percentages.of(progress.opened(), progress.started())));
    }

    private static FunnelStage stage(String id, String label, long sessions, Integer conversionPercent) {
        return FunnelStage.builder().id(id).label(label).sessionCount(sessions).conversionPercent(conversionPercent)
                .build();
    }
}
