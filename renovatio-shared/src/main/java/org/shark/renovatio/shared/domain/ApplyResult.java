package org.shark.renovatio.shared.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;
import java.util.Map;

/**
 * Result of apply operation
 */
@Getter
@Setter
@NoArgsConstructor
public class ApplyResult extends ProviderResult {
    private String diff;
    private Map<String, Object> changes;
    private boolean dryRun;
    private List<String> modifiedFiles;

    public ApplyResult(boolean success, String message) {
        super(success, message);
    }
}