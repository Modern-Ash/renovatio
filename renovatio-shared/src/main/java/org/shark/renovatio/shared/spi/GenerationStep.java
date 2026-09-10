package org.shark.renovatio.shared.spi;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a single step in a generation plan.
 */
public record GenerationStep(
    String name,
    String type,
    Map<String, Object> parameters
) {
    public GenerationStep {
        parameters = parameters != null ? Map.copyOf(parameters) : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String type;
        private Map<String, Object> parameters = Collections.emptyMap();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public GenerationStep build() {
            return new GenerationStep(name, type, parameters);
        }
    }
}