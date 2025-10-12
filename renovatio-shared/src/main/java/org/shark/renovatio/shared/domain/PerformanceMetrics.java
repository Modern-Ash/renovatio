package org.shark.renovatio.shared.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple container for execution performance metrics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetrics {
    private long executionTimeMs;
}
