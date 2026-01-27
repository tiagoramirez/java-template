package com.tiagoramirez.template.health.adapters.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.tiagoramirez.template.health.domain.HealthStatus;
import com.tiagoramirez.template.health.ports.in.HealthPort;

@ExtendWith(MockitoExtension.class)
public class HealthControllerTest {

    @InjectMocks
    private HealthController controller;

    @Mock
    private HealthPort port;

    @Test
    void testHealthCheck() {
        long secondsToArgentina = 3 * 60 * 60;
        Instant now = Instant.now();
        when(port.check()).thenReturn(new HealthStatus("I'm alive!", now));

        ResponseEntity<HealthResponseDto> response = controller.check();

        assertEquals("I'm alive!", response.getBody().message());
        assertEquals(now.minusSeconds(secondsToArgentina), response.getBody().timestamp());
    }
}
