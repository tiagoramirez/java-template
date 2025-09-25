package com.tiagoramirez.template.health.dtos.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HealthResponse(
        String message,
        @JsonProperty("timestamp") Instant timestamp) {
}
