package com.tiagoramirez.template.health.application;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tiagoramirez.template.health.domain.HealthStatus;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @InjectMocks
    private HealthService healthService;

    @Test
    void shouldReturnHealthStatus() {
        HealthStatus result = healthService.check();

        assertNotNull(result);
        assertEquals("I'm alive!", result.message());
        assertNotNull(result.timestamp());
        assertTrue(result.timestamp().isBefore(Instant.now().plusSeconds(1)));
    }
}
