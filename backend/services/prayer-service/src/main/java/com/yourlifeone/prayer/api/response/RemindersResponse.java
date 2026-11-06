package com.yourlifeone.prayer.api.response;

import java.util.List;

/**
 * @param active reminders sent and still to show now, newest first
 * @param momentum every pillar's momentum reminder preference
 */
public record RemindersResponse(List<ReminderResponse> active, List<MomentumReminderResponse> momentum) {
}
