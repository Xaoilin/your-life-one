package com.yourlifeone.life.inventory.domain;

import java.math.BigDecimal;

/** An optional envelope: at least one positive axis, with its unit. */
public record Dimensions(BigDecimal length, BigDecimal width, BigDecimal height, String unit) {
}
