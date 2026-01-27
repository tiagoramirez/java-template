package com.tiagoramirez.template.health.domain;

import java.time.Instant;

public record HealthStatus(String message, Instant timestamp) {
    public static HealthStatus ok(Instant timestamp) {
        // This has no sense because the time is in Z (UTC)... It is just for example
        long secondsToArgentina = -3 * 60 * 60;
        return new HealthStatus("I'm alive!", timestamp.plusSeconds(secondsToArgentina));
    }
}
