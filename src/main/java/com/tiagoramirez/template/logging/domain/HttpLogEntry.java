package com.tiagoramirez.template.logging.domain;

import java.time.Instant;
import java.util.Map;

public record HttpLogEntry(
        String traceId,
        String method,
        String path,
        String query,
        int status,
        long durationMs,
        String clientIp,
        String userAgent,
        Map<String, String> requestHeaders,
        String requestBody,
        String responseBody,
        Instant timestamp
) {
    public HttpLogEntry {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId cannot be null or blank");
        }
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method cannot be null or blank");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path cannot be null or blank");
        }
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp cannot be null");
        }
    }

    public boolean isError() {
        return status >= 400;
    }

    public boolean isServerError() {
        return status >= 500;
    }
}
