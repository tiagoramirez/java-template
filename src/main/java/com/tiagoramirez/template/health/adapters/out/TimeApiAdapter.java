package com.tiagoramirez.template.health.adapters.out;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.tiagoramirez.template.health.ports.out.TimeApiPort;

@Component
public class TimeApiAdapter implements TimeApiPort {

    @Override
    public Instant getCurrentTime() {
        return Instant.now();
    }
}
