package com.tiagoramirez.template.health.adapters.in.web;

import org.springframework.web.bind.annotation.RestController;

import com.tiagoramirez.template.health.dtos.response.BaseResponse;
import com.tiagoramirez.template.health.ports.in.web.HealthPort;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/health")
public class HealthController implements HealthPort {

    @Override
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public BaseResponse check() {
        return new BaseResponse("I'm alive!");
    }

}
