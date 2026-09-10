package org.shark.renovatio.shared.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of a generation request.
 */
public record GenerationResult(
    boolean success,
    GeneratedCode output,
    List<String> errors,
    List<String> warnings,
    Map<String, Object> metadata
) {
    public GenerationResult {
        errors = errors != null ? List.copyOf(errors) : Collections.emptyList();
        warnings = warnings != null ? List.copyOf(warnings) : Collections.emptyList();
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private GeneratedCode output;
        private List<String> errors = Collections.emptyList();
        private List<String> warnings = Collections.emptyList();
        private Map<String, Object> metadata = Collections.emptyMap();

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder output(GeneratedCode output) {
            this.output = output;
            return this;
        }

        public Builder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings = warnings;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public GenerationResult build() {
            return new GenerationResult(success, output, errors, warnings, metadata);
        }
    }
}