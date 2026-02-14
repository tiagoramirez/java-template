package com.tiagoramirez.template.logging.ports.out;

import com.tiagoramirez.template.logging.domain.HttpLogEntry;

public interface LogPublisherPort {
    void publish(HttpLogEntry logEntry);
}
