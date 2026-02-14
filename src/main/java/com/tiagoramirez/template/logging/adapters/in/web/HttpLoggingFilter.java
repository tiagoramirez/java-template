package com.tiagoramirez.template.logging.adapters.in.web;

import com.tiagoramirez.template.logging.domain.HttpLogEntry;
import com.tiagoramirez.template.logging.ports.out.LogPublisherPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpLoggingFilter extends OncePerRequestFilter {

    private final LogPublisherPort logPublisherPort;

    public HttpLoggingFilter(LogPublisherPort logPublisherPort) {
        this.logPublisherPort = logPublisherPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 10000);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        String traceId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        Instant timestamp = Instant.now();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            String clientIp = extractClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            Map<String, String> headers = extractHeaders(request);

            HttpLogEntry logEntry = new HttpLogEntry(
                    traceId,
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString(),
                    response.getStatus(),
                    duration,
                    clientIp,
                    userAgent,
                    headers,
                    null,
                    null,
                    timestamp
            );

            logPublisherPort.publish(logEntry);

            wrappedResponse.copyBodyToResponse();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/actuator");
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        return Collections.unmodifiableMap(headers);
    }
}
