package org.shark.renovatio.shared.domain;

import java.util.HashMap;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Aggregated report containing migration metrics and provider statuses.
 */
@Getter
@ToString
@EqualsAndHashCode
public class MigrationReport {
    private final Map<String, Double> metrics = new HashMap<>();
    private final Map<String, String> statuses = new HashMap<>();

    /**
     * Add metrics from a provider to the aggregate.
     */
    public void addMetrics(Map<String, ? extends Number> data) {
        if (data == null) {
            return;
        }
        data.forEach((k, v) -> metrics.merge(k, v.doubleValue(), Double::sum));
    }

    /**
     * Record the status for a provider.
     */
    public void addStatus(String provider, boolean success) {
        statuses.put(provider, success ? "SUCCESS" : "FAILED");
    }
}
