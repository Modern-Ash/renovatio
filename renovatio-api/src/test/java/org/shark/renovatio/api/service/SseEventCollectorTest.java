package org.shark.renovatio.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SseEventCollectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SseEventCollector eventCollector = new SseEventCollector(objectMapper);

    @Test
    void shouldSubscribeToJobEvents() {
        SseEmitter emitter = eventCollector.subscribe("job-123");
        assertNotNull(emitter);
    }

    @Test
    void shouldCompleteEmitter() {
        eventCollector.subscribe("job-456");
        eventCollector.complete("job-456");
    }

    @Test
    void shouldCompleteNonExistentJob() {
        eventCollector.complete("non-existent");
    }
}
