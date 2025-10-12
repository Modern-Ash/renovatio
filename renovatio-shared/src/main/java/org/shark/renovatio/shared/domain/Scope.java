package org.shark.renovatio.shared.domain;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Scope definition for operations
 */
@Data
@NoArgsConstructor
public class Scope {
    private List<String> paths;
    private List<String> includePatterns;
    private List<String> excludePatterns;
    private Map<String, Object> properties;

    public Scope(List<String> paths) {
        this.paths = paths;
    }
}