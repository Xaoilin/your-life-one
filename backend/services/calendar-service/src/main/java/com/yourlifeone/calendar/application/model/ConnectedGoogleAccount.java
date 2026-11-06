package com.yourlifeone.calendar.application.model;

/**
 * A Google account identified from an access token.
 *
 * @param email lower-case address of the account's primary calendar
 * @param name the primary calendar's name, or the address when it has none
 */
public record ConnectedGoogleAccount(String email, String name) {
}
