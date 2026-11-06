package com.yourlifeone.profile.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ViewPreferenceRequest(@NotBlank @Size(max = 64) String value) {
}
