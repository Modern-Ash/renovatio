package org.shark.renovatio.shared.nql;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Result of NQL compilation (Natural Language -> NQL)
 */
@Data
public class NqlCompileResult {
    private boolean success;
    private NqlQuery query;
    private String reasoning;
    private List<String> errors;
    private Map<String, Object> metadata;

    public NqlCompileResult() {
    }

    public NqlCompileResult(boolean success, NqlQuery query, String reasoning) {
        this.success = success;
        this.query = query;
        this.reasoning = reasoning;
    }
}