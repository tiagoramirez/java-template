package com.tiagoramirez.template.health.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tiagoramirez.template.health.ports.out.TimeProviderPort;

@ExtendWith(MockitoExtension.class)
class HealthStatusTest {
    private TimeProviderPort timeProvider;

    @BeforeEach
    void setUp() {
        timeProvider = mock(TimeProviderPort.class);
    }

    @Test
    void shouldCreateHealthStatusWithCurrentTimestamp() {
        when(timeProvider.getCurrentTime()).thenReturn(Instant.now());
        HealthStatus status = HealthStatus.ok(timeProvider);

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
