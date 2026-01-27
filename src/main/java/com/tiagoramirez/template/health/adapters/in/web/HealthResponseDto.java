package com.tiagoramirez.template.health.adapters.in.web;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HealthResponseDto(
        String message,
        @JsonProperty("timestamp") Instant timestamp) {
}
