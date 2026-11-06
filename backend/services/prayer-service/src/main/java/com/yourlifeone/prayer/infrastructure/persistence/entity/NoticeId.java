package com.yourlifeone.prayer.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NoticeId implements Serializable {
    private UUID userId;
    private String key;
}
