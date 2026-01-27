package com.tiagoramirez.template.health.domain;

import java.time.Instant;

import com.tiagoramirez.template.health.ports.out.TimeProviderPort;

public record HealthStatus(String message, Instant timestamp) {
    public static HealthStatus ok(TimeProviderPort timeApiPort) {
        Instant now = timeApiPort.getCurrentTime();
        return new HealthStatus("I'm alive!", now);
    }
}
