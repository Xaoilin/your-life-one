package com.yourlifeone.prayer.api.mapper;

import com.yourlifeone.prayer.api.response.DashboardResponse;
import com.yourlifeone.prayer.api.response.OutcomeChangeResponse;
import com.yourlifeone.prayer.api.response.OutcomeResponse;
import com.yourlifeone.prayer.api.response.PreferencesResponse;
import com.yourlifeone.prayer.api.response.RewardedOutcomeResponse;
import com.yourlifeone.prayer.api.response.RewardedOutcomesResponse;
import com.yourlifeone.prayer.api.response.StatsResponse;
import com.yourlifeone.prayer.api.response.TallyResponse;
import com.yourlifeone.prayer.api.response.TrackingResponse;
import com.yourlifeone.prayer.application.model.Dashboard;
import com.yourlifeone.prayer.application.model.OutcomeChange;
import com.yourlifeone.prayer.domain.Outcome;
import com.yourlifeone.prayer.domain.OutcomeStats;
import com.yourlifeone.prayer.domain.OutcomeTally;
import com.yourlifeone.prayer.domain.PrayerName;
import com.yourlifeone.prayer.domain.PrayerProfile;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps application results to API responses. */
@Mapper(uses = ScheduleResponseMapper.class)
public interface PrayerResponseMapper {
    OutcomeResponse toResponse(Outcome outcome);

    List<OutcomeResponse> toResponses(List<Outcome> outcomes);

    OutcomeChangeResponse toResponse(OutcomeChange change);

    RewardedOutcomeResponse toRewarded(Outcome outcome);

    default RewardedOutcomesResponse toRewarded(List<Outcome> outcomes) {
        return new RewardedOutcomesResponse(outcomes.stream().map(this::toRewarded).toList());
    }

    PreferencesResponse toPreferences(PrayerProfile profile);

    @Mapping(target = "trackingStartedAt", source = "tracking.trackingStartedAt")
    @Mapping(target = "activationDate", source = "tracking.activationDate")
    @Mapping(target = "activationPrayers", source = "tracking.activationPrayers")
    TrackingResponse toTracking(PrayerProfile profile);

    TallyResponse toResponse(OutcomeTally tally);

    StatsResponse toResponse(OutcomeStats stats);

    @Mapping(target = "preferences", source = "profile")
    @Mapping(target = "tracking", source = "profile")
    DashboardResponse toResponse(Dashboard dashboard);


    /** Keys per-prayer tallies by prayer name ("Fajr"), in prayer order. */
    default Map<String, TallyResponse> toPerPrayer(Map<PrayerName, OutcomeTally> perPrayer) {
        var byName = new LinkedHashMap<String, TallyResponse>();
        perPrayer.forEach((prayer, tally) -> byName.put(prayer.label(), toResponse(tally)));
        return byName;
    }
}
