package com.tiagoramirez.template.health.adapters.in;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.tiagoramirez.template.health.domain.HealthStatus;
import com.tiagoramirez.template.health.ports.in.HealthPort;
import com.tiagoramirez.template.health.ports.out.TimeApiPort;

@Component
public class HealthAdapter implements HealthPort {
    @Autowired
    private TimeApiPort timeApiPort;

    @Override
    public HealthStatus check() {
        return HealthStatus.ok(timeApiPort);
    }
}