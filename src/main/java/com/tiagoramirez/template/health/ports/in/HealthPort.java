package com.tiagoramirez.template.health.ports.in;

import com.tiagoramirez.template.health.domain.HealthStatus;

public interface HealthPort {
    HealthStatus check();
}
