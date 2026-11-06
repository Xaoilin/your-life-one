package com.yourlifeone.prayer.infrastructure.theshia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingLocation(
        Double latitude, Double longitude, String timezone, @JsonProperty("feature_code") String featureCode) {
}
