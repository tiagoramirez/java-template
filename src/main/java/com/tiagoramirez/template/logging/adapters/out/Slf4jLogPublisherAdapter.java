package com.tiagoramirez.template.logging.adapters.out;

import com.tiagoramirez.template.logging.domain.HttpLogEntry;
import com.tiagoramirez.template.logging.ports.out.LogPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class Slf4jLogPublisherAdapter implements LogPublisherPort {

    private static final Logger logger = LoggerFactory.getLogger("HttpRequestLogger");

    @Override
    public void publish(HttpLogEntry logEntry) {
        try {
            MDC.put("trace_id", logEntry.traceId());
            MDC.put("http_method", logEntry.method());
            MDC.put("http_path", logEntry.path());
            MDC.put("http_status", String.valueOf(logEntry.status()));
            MDC.put("http_duration_ms", String.valueOf(logEntry.durationMs()));
            MDC.put("client_ip", logEntry.clientIp());
            MDC.put("user_agent", logEntry.userAgent());

            String message = String.format("HTTP %s %s - Status: %d - Duration: %dms",
                    logEntry.method(),
                    logEntry.path(),
                    logEntry.status(),
                    logEntry.durationMs()
            );

            if (logEntry.isServerError()) {
                logger.error(message);
            } else if (logEntry.isError()) {
                logger.warn(message);
            } else {
                logger.info(message);
            }
        } finally {
            MDC.clear();
        }
    }
}
