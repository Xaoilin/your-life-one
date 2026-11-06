package com.yourlifeone.calendar.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Every calendar row is keyed by its owner and its own ID. */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserScopedId implements Serializable {
    private UUID userId;
    private String id;
}
