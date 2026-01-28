package com.tiagoramirez.template.health.adapters.out;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.tiagoramirez.template.health.ports.out.TimeProviderPort;

@Component
public class TimeProviderAdapter implements TimeProviderPort {

    @Override
    public Instant getCurrentTime() {
        return Instant.now();
    }
}
