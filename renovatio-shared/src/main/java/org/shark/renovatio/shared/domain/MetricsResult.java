package org.shark.renovatio.shared.domain;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class MetricsResult {
    private boolean success;
    private String message;
    private Map<String, Number> metrics = new HashMap<>();
    private Map<String, Object> details = new HashMap<>();
    private String runId;

    public MetricsResult() {
    }

    public MetricsResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public void setMetrics(Map<String, Number> metrics) {
        this.metrics = (metrics != null) ? new HashMap<>(metrics) : new HashMap<>();
    }

    public void setDetails(Map<String, Object> details) {
        this.details = (details != null) ? details : new HashMap<>();
    }
}
