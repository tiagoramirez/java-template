package com.tiagoramirez.template.logging.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpLogEntryTest {

    @Test
    void validConstruction_ShouldCreateInstance() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        HttpLogEntry logEntry = new HttpLogEntry(
                "trace-123",
                "GET",
                "/api/health",
                null,
                200,
                50L,
                "127.0.0.1",
                "Mozilla/5.0",
                headers,
                null,
                null,
                Instant.now()
        );

        assertNotNull(logEntry);
        assertEquals("trace-123", logEntry.traceId());
        assertEquals("GET", logEntry.method());
        assertEquals("/api/health", logEntry.path());
        assertEquals(200, logEntry.status());
    }

    @Test
    void nullTraceId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new HttpLogEntry(
                null,
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
        ));
    }

    @Test
    void blankTraceId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new HttpLogEntry(
                "",
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
        ));
    }

    @Test
    void nullMethod_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new HttpLogEntry(
                "trace-123",
                null,
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
        ));
    }

    @Test
    void nullPath_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new HttpLogEntry(
                "trace-123",
                "GET",
                null,
                null,
                200,
                50L,
                "127.0.0.1",
                "Mozilla/5.0",
                new HashMap<>(),
                null,
                null,
                Instant.now()
        ));
    }

    @Test
    void negativeDuration_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new HttpLogEntry(
                "trace-123",
                "GET",
                "/api/health",
                null,
                200,
                -1L,
                "127.0.0.1",
                "Mozilla/5.0",
                new HashMap<>(),
                null,
                null,
                Instant.now()
        ));
    }

    @Test
    void nullTimestamp_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new HttpLogEntry(
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
                null
        ));
    }

    @Test
    void isError_StatusGreaterThanOrEqualTo400_ShouldReturnTrue() {
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

        assertTrue(logEntry.isError());
    }

    @Test
    void isError_StatusLessThan400_ShouldReturnFalse() {
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

        assertFalse(logEntry.isError());
    }

    @Test
    void isServerError_StatusGreaterThanOrEqualTo500_ShouldReturnTrue() {
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

        assertTrue(logEntry.isServerError());
    }

    @Test
    void isServerError_StatusLessThan500_ShouldReturnFalse() {
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

        assertFalse(logEntry.isServerError());
    }
}
