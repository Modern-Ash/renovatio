package org.shark.renovatio.shared.domain;

import java.util.Map;

import lombok.Data;

/**
 * Base class for provider operation results
 */
@Data
public abstract class ProviderResult {
    private boolean success;
    private String message;
    private String runId;
    private Map<String, Object> metadata;
    private long timestamp;

    public ProviderResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public ProviderResult(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }
}