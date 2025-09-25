package com.tiagoramirez.template.health.adapters.in.web;

import org.springframework.web.bind.annotation.RestController;

import com.tiagoramirez.template.health.domain.HealthStatus;
import com.tiagoramirez.template.health.dtos.response.HealthResponse;
import com.tiagoramirez.template.health.ports.in.web.HealthPort;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private HealthPort healthPort;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<HealthResponse> check() {
        HealthStatus healthStatus = healthPort.check();
        return ResponseEntity.ok(new HealthResponse(healthStatus.message(), healthStatus.timestamp()));
    }

}
