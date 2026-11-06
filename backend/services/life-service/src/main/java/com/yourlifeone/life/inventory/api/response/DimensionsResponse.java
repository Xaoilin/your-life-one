package com.yourlifeone.life.inventory.api.response;

import java.math.BigDecimal;

public record DimensionsResponse(BigDecimal length, BigDecimal width, BigDecimal height, String unit) {
}
