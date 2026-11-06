package com.yourlifeone.prayer.infrastructure.theshia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingResponse(List<GeocodingLocation> results) {
}
