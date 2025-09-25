package com.tiagoramirez.template.health.adapters.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.tiagoramirez.template.health.dtos.response.BaseResponse;
import com.tiagoramirez.template.health.ports.in.web.HealthPort;

public class HealthControllerTests {

    @Test
    void testHealthCheck() {
        HealthPort helloWorldController = new HealthController();

        BaseResponse response = helloWorldController.check();

        assertEquals(response.getMessage(), "I'm alive!");
        assertNotNull(response.getDateTime());
    }
}
