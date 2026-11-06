package com.yourlifeone.calendar.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * An authorization code from Google's consent popup.
 *
 * @param redirectUri the redirect URI the popup used (the app's origin)
 * @param expectedEmail when reconnecting, the account the user meant; another account is refused
 */
public record ConnectGoogleAccountRequest(
        @NotBlank @Size(max = 2048) String code,
        @NotBlank @Size(max = 2048) String redirectUri,
        @Email @Size(max = 320) String expectedEmail) {
}
