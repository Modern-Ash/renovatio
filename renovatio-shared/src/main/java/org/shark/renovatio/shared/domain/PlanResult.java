package org.shark.renovatio.shared.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Result of plan operation
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanResult extends ProviderResult {
    private String planId;
    private String planContent;
    private Map<String, Object> steps;

    public PlanResult(boolean success, String message) {
        super(success, message);
    }
}