package com.tiagoramirez.template.health.ports.in.web;

import com.tiagoramirez.template.health.dtos.response.BaseResponse;

public interface HealthPort {
    BaseResponse check();
}
