package com.tiagoramirez.template.health.application;

import org.springframework.stereotype.Service;

import com.tiagoramirez.template.health.domain.HealthStatus;
import com.tiagoramirez.template.health.ports.in.web.HealthPort;

@Service
public class HealthService implements HealthPort {

    @Override
    public HealthStatus check() {
        return HealthStatus.ok();
    }
}
