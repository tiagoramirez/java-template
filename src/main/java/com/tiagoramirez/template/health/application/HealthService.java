package com.tiagoramirez.template.health.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.tiagoramirez.template.health.domain.HealthStatus;
import com.tiagoramirez.template.health.ports.in.HealthPort;
import com.tiagoramirez.template.health.ports.out.TimeProviderPort;

@Component
public class HealthService implements HealthPort {
    @Autowired
    private TimeProviderPort timeProvider;

    @Override
    public HealthStatus check() {
        return HealthStatus.ok(timeProvider.getCurrentTime());
    }
}