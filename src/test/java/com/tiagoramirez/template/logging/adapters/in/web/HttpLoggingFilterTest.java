package com.tiagoramirez.template.logging.adapters.in.web;

import com.tiagoramirez.template.logging.domain.HttpLogEntry;
import com.tiagoramirez.template.logging.ports.out.LogPublisherPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpLoggingFilterTest {

    private LogPublisherPort logPublisherPort;
    private HttpLoggingFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        logPublisherPort = mock(LogPublisherPort.class);
        filter = new HttpLoggingFilter(logPublisherPort);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void doFilterInternal_ShouldPublishLogEntry() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilterInternal(request, response, filterChain);

        verify(logPublisherPort, times(1)).publish(any(HttpLogEntry.class));
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_ShouldPublishLogEntryWithCorrectMethod() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        ArgumentCaptor<HttpLogEntry> captor = ArgumentCaptor.forClass(HttpLogEntry.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(logPublisherPort).publish(captor.capture());
        assertEquals("POST", captor.getValue().method());
    }

    @Test
    void doFilterInternal_ShouldPublishLogEntryWithCorrectPath() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        ArgumentCaptor<HttpLogEntry> captor = ArgumentCaptor.forClass(HttpLogEntry.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(logPublisherPort).publish(captor.capture());
        assertEquals("/api/test", captor.getValue().path());
    }

    @Test
    void doFilterInternal_ShouldPublishLogEntryWithCorrectStatus() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);

        ArgumentCaptor<HttpLogEntry> captor = ArgumentCaptor.forClass(HttpLogEntry.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(logPublisherPort).publish(captor.capture());
        assertEquals(404, captor.getValue().status());
    }

    @Test
    void doFilterInternal_ShouldGenerateUniqueTraceId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        ArgumentCaptor<HttpLogEntry> captor = ArgumentCaptor.forClass(HttpLogEntry.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(logPublisherPort).publish(captor.capture());
        assertNotNull(captor.getValue().traceId());
        assertFalse(captor.getValue().traceId().isBlank());
    }

    @Test
    void doFilterInternal_ShouldMeasureDuration() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        ArgumentCaptor<HttpLogEntry> captor = ArgumentCaptor.forClass(HttpLogEntry.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(logPublisherPort).publish(captor.capture());
        assertTrue(captor.getValue().durationMs() >= 0);
    }

    @Test
    void doFilterInternal_ShouldExtractClientIpFromRemoteAddr() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/health");
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        ArgumentCaptor<HttpLogEntry> captor = ArgumentCaptor.forClass(HttpLogEntry.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(logPublisherPort).publish(captor.capture());
        assertEquals("192.168.1.1", captor.getValue().clientIp());
    }

    @Test
    void doFilterInternal_ShouldExtractClientIpFromXForwardedFor() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/health");
        request.setRemoteAddr("192.168.1.1");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        ArgumentCaptor<HttpLogEntry> captor = ArgumentCaptor.forClass(HttpLogEntry.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(logPublisherPort).publish(captor.capture());
        assertEquals("10.0.0.1", captor.getValue().clientIp());
    }

    @Test
    void doFilterInternal_ShouldExtractUserAgent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/health");
        request.addHeader("User-Agent", "Mozilla/5.0");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        ArgumentCaptor<HttpLogEntry> captor = ArgumentCaptor.forClass(HttpLogEntry.class);

        filter.doFilterInternal(request, response, filterChain);

        verify(logPublisherPort).publish(captor.capture());
        assertEquals("Mozilla/5.0", captor.getValue().userAgent());
    }

    @Test
    void shouldNotFilter_ActuatorPath_ShouldReturnTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/actuator/health");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_HealthPath_ShouldReturnFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/health");

        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void doFilterInternal_ShouldCallFilterChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(any(), any());
    }
}
