package org.shark.renovatio.shared.domain;

import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Result of diff operation
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DiffResult extends ProviderResult {
    private String unifiedDiff;
    private Map<String, Object> semanticDiff;
    private Map<String, Object> hunks;

    public DiffResult(boolean success, String message) {
        super(success, message);
    }
}