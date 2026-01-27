package com.tiagoramirez.template.health.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tiagoramirez.template.health.domain.HealthStatus;
import com.tiagoramirez.template.health.ports.out.TimeProviderPort;

@ExtendWith(MockitoExtension.class)
public class HealthServiceTest {

    @InjectMocks
    private HealthService service;

    @Mock
    private TimeProviderPort timeProvider;

    @Test
    void testCheck() {
        Instant now = Instant.now();
        when(timeProvider.getCurrentTime()).thenReturn(now);

        HealthStatus status = service.check();

        assertEquals("I'm alive!", status.message());
        assertEquals(now, status.timestamp());
    }
}
