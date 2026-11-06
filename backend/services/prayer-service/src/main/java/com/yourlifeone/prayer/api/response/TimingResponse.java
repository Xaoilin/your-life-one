package com.yourlifeone.prayer.api.response;

import lombok.Builder;

/**
 * @param time local clock time, HH:mm
 * @param type "prayer" for an obligatory prayer, "event" for sunrise, sunset and midnight
 */
@Builder
public record TimingResponse(String name, String nameArabic, String time, String type) {
}
