package org.shark.renovatio.shared.spi;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * Represents a request for code generation.
 */
public record GenerationRequest(
    String sourceLanguage,
    String targetLanguage,
    SourceCode sourceCode,
    Path outputPath,
    Map<String, Object> options
) {
    public GenerationRequest {
        options = options != null ? Map.copyOf(options) : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sourceLanguage;
        private String targetLanguage;
        private SourceCode sourceCode;
        private Path outputPath;
        private Map<String, Object> options = Collections.emptyMap();

        public Builder sourceLanguage(String sourceLanguage) {
            this.sourceLanguage = sourceLanguage;
            return this;
        }

        public Builder targetLanguage(String targetLanguage) {
            this.targetLanguage = targetLanguage;
            return this;
        }

        public Builder sourceCode(SourceCode sourceCode) {
            this.sourceCode = sourceCode;
            return this;
        }

        public Builder outputPath(Path outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public Builder options(Map<String, Object> options) {
            this.options = options;
            return this;
        }

        public GenerationRequest build() {
            return new GenerationRequest(sourceLanguage, targetLanguage, sourceCode, outputPath, options);
        }
    }
}