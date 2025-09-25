package com.tiagoramirez.template.health.domain;

import java.time.Instant;

public record HealthStatus(String message, Instant timestamp) {
    public static HealthStatus ok() {
        return new HealthStatus("I'm alive!", Instant.now());
    }
}
