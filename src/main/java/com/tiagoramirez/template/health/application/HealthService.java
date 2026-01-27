package com.tiagoramirez.template.health.application;

import org.springframework.stereotype.Component;
import com.tiagoramirez.template.health.domain.HealthStatus;
import com.tiagoramirez.template.health.ports.in.HealthPort;
import com.tiagoramirez.template.health.ports.out.TimeProviderPort;

@Component
public class HealthService implements HealthPort {
    private final TimeProviderPort timeProvider;

    public HealthService(TimeProviderPort timeProvider) {
        this.timeProvider = timeProvider;
    }

    @Override
    public HealthStatus check() {
        return HealthStatus.ok(timeProvider.getCurrentTime());
    }
}