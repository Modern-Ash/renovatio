package org.shark.renovatio.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEventCollector {
    private static final Logger log = LoggerFactory.getLogger(SseEventCollector.class);
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SseEventCollector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe(String jobId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(jobId, emitter);
        emitter.onCompletion(() -> {
            emitters.remove(jobId);
            log.debug("SSE completed for job: {}", jobId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(jobId);
            log.debug("SSE timed out for job: {}", jobId);
        });
        emitter.onError(e -> {
            emitters.remove(jobId);
            log.debug("SSE error for job: {}", jobId, e);
        });
        log.debug("SSE subscription added for job: {}", jobId);
        return emitter;
    }

    public void send(String jobId, String eventType, Object data) {
        SseEmitter emitter = emitters.get(jobId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventType)
                        .data(objectMapper.writeValueAsString(data)));
            } catch (IOException e) {
                emitters.remove(jobId);
                log.debug("Failed to send SSE event for job: {}", jobId, e);
            }
        }
    }

    public void complete(String jobId) {
        SseEmitter emitter = emitters.remove(jobId);
        if (emitter != null) {
            emitter.complete();
            log.debug("SSE emitter completed for job: {}", jobId);
        }
    }
}
