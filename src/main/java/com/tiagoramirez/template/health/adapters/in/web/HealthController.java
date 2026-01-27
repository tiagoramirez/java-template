package com.tiagoramirez.template.health.adapters.in.web;

import org.springframework.web.bind.annotation.RestController;

import com.tiagoramirez.template.health.domain.HealthStatus;
import com.tiagoramirez.template.health.ports.in.HealthPort;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final HealthPort healthPort;

    public HealthController(HealthPort healthPort) {
        this.healthPort = healthPort;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<HealthResponseDto> check() {
        HealthStatus healthStatus = healthPort.check();

        // This has no sense because the time is in Z (UTC)... It is just for example
        long secondsToArgentina = 3 * 60 * 60;

        return ResponseEntity.ok(new HealthResponseDto(healthStatus.message(),
                healthStatus.timestamp().minusSeconds(secondsToArgentina)));
    }

}
