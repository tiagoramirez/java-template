package com.tiagoramirez.template.health.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class HealthStatusTest {

    @Test
    void shouldCreateHealthStatusWithCurrentTimestamp() {
        HealthStatus status = HealthStatus.ok(Instant.now());

        assertEquals("I'm alive!", status.message());
        assertNotNull(status.timestamp());
        assertTrue(status.timestamp().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void shouldCreateHealthStatusWithCustomValues() {
        String testMessage = "Test message";
        Instant testTimestamp = Instant.now();

        HealthStatus status = new HealthStatus(testMessage, testTimestamp);

        assertEquals(testMessage, status.message());
        assertEquals(testTimestamp, status.timestamp());
    }
}
