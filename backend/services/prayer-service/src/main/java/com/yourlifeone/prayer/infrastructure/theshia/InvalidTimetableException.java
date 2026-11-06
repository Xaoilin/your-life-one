package com.yourlifeone.prayer.infrastructure.theshia;

/** The provider answered, but its timetable cannot be used. */
public class InvalidTimetableException extends RuntimeException {
    public InvalidTimetableException(String detail) {
        super(detail);
    }
}
