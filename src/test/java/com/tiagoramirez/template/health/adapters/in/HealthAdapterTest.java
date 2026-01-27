package com.tiagoramirez.template.health.adapters.in;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tiagoramirez.template.health.domain.HealthStatus;
import com.tiagoramirez.template.health.ports.out.TimeApiPort;

@ExtendWith(MockitoExtension.class)
public class HealthAdapterTest {

    @InjectMocks
    private HealthAdapter adapter;

    @Mock
    private TimeApiPort port;

    @Test
    void testCheck() {
        when(port.getCurrentTime()).thenReturn(Instant.now());

        HealthStatus status = adapter.check();

        assertEquals("I'm alive!", status.message());
        assertNotNull(status.timestamp());
    }
}
