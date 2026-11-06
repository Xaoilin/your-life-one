package com.yourlifeone.prayer.infrastructure.theshia.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TheShiaMeta(String tz, String method) {
}
