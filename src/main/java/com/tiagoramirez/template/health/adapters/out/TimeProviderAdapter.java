package com.tiagoramirez.template.health.adapters.out;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.tiagoramirez.template.health.ports.out.TimeProviderPort;

@Component
public class TimeProviderAdapter implements TimeProviderPort {

    @Override
    public Instant getCurrentTime() {
        // This has no sense because the time is in Z (UTC)... It is just for example
        long secondsToArgentina = -3 * 60 * 60;
        return Instant.now().plusSeconds(secondsToArgentina);
    }
}
