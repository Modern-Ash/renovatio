package org.shark.renovatio.shared.domain;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Result of analyze operation
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AnalyzeResult extends ProviderResult {
    private Map<String, Object> ast;
    private Map<String, Object> dependencies;
    private Map<String, Object> symbols;
    private Map<String, Object> data;
    private PerformanceMetrics performance;

    public AnalyzeResult(boolean success, String message) {
        super(success, message);
    }
}