package com.yourlifeone.life.inventory.infrastructure.persistence.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One named specification row, kept in order by its position. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpecificationEmbeddable {
    private String name;
    private String value;
}
