package com.tiagoramirez.template.health.ports.out;

import java.time.Instant;

public interface TimeProviderPort {
    Instant getCurrentTime();
}
