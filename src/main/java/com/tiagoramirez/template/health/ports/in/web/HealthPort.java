package com.tiagoramirez.template.health.ports.in.web;

import com.tiagoramirez.template.health.domain.HealthStatus;

public interface HealthPort {
    HealthStatus check();
}
