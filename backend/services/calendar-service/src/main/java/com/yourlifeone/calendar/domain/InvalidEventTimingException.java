package com.yourlifeone.calendar.domain;

/** An event's start and end do not describe a valid time span. */
public class InvalidEventTimingException extends RuntimeException {
    public InvalidEventTimingException(String message) {
        super(message);
    }
}
