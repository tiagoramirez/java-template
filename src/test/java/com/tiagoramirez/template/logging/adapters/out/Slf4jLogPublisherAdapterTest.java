package com.tiagoramirez.template.logging.adapters.out;

import com.tiagoramirez.template.logging.domain.HttpLogEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

class Slf4jLogPublisherAdapterTest {

    private final Slf4jLogPublisherAdapter adapter = new Slf4jLogPublisherAdapter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void publish_ValidLogEntry_ShouldNotThrowException() {
        HttpLogEntry logEntry = new HttpLogEntry(
                "trace-123",
                "GET",
                "/api/health",
                null,
                200,
                50L,
                "127.0.0.1",
                "Mozilla/5.0",
                new HashMap<>(),
                null,
                null,
                Instant.now()
        );

        assertDoesNotThrow(() -> adapter.publish(logEntry));
    }

    @Test
    void publish_Status200_ShouldLogAtInfoLevel() {
        HttpLogEntry logEntry = new HttpLogEntry(
                "trace-123",
                "GET",
                "/api/health",
                null,
                200,
                50L,
                "127.0.0.1",
                "Mozilla/5.0",
                new HashMap<>(),
                null,
                null,
                Instant.now()
        );

        assertDoesNotThrow(() -> adapter.publish(logEntry));
    }

    @Test
    void publish_Status400_ShouldLogAtWarnLevel() {
        HttpLogEntry logEntry = new HttpLogEntry(
                "trace-123",
                "GET",
                "/api/health",
                null,
                400,
                50L,
                "127.0.0.1",
                "Mozilla/5.0",
                new HashMap<>(),
                null,
                null,
                Instant.now()
        );

        assertDoesNotThrow(() -> adapter.publish(logEntry));
    }

    @Test
    void publish_Status500_ShouldLogAtErrorLevel() {
        HttpLogEntry logEntry = new HttpLogEntry(
                "trace-123",
                "GET",
                "/api/health",
                null,
                500,
                50L,
                "127.0.0.1",
                "Mozilla/5.0",
                new HashMap<>(),
                null,
                null,
                Instant.now()
        );

        assertDoesNotThrow(() -> adapter.publish(logEntry));
    }

    @Test
    void publish_ShouldClearMDC() {
        HttpLogEntry logEntry = new HttpLogEntry(
                "trace-123",
                "GET",
                "/api/health",
                null,
                200,
                50L,
                "127.0.0.1",
                "Mozilla/5.0",
                new HashMap<>(),
                null,
                null,
                Instant.now()
        );

        adapter.publish(logEntry);

        assertNull(MDC.get("trace_id"));
        assertNull(MDC.get("http_method"));
        assertNull(MDC.get("http_path"));
        assertNull(MDC.get("http_status"));
        assertNull(MDC.get("http_duration_ms"));
        assertNull(MDC.get("client_ip"));
        assertNull(MDC.get("user_agent"));
    }
}
