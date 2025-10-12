package org.shark.renovatio.shared.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Result of stub generation
 */
@Getter
@Setter
@NoArgsConstructor
public class StubResult extends ProviderResult {
    private String targetLanguage;
    private Map<String, String> generatedFiles;
    private String stubTemplate;
    private Map<String, String> generatedCode;

    public StubResult(boolean success, String message) {
        super(success, message);
    }
}