package com.yourlifeone.calendar.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** A stored Google credential is keyed by its owner and the Google account's address. */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GoogleCredentialId implements Serializable {
    private UUID userId;
    private String email;
}
