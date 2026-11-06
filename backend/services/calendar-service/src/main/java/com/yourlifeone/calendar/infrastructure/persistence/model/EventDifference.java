package com.yourlifeone.calendar.infrastructure.persistence.model;

/** Null stored ID means insert; null Google ID means delete. Unchanged events are not returned. */
public record EventDifference(String storedId, String googleId) {
}
