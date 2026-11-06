package com.yourlifeone.profile.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

/** Different choices are separate rows, so one device cannot replace another device's unrelated choice. */
public record ViewPreferenceKey(UUID userId, String preferenceKey) implements Serializable {
}
