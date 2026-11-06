package com.yourlifeone.prayer.api.response;

import java.util.List;

/** Every pillar's momentum reminder preference, in pillar order. */
public record MomentumRemindersResponse(List<MomentumReminderResponse> pillars) {
}
