package org.shark.renovatio.shared.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a plan for code generation.
 */
public record GenerationPlan(
    String targetLanguage,
    SemanticModel sourceModel,
    List<GenerationStep> steps,
    Map<String, Object> options
) {
    public GenerationPlan {
        steps = steps != null ? List.copyOf(steps) : Collections.emptyList();
        options = options != null ? Map.copyOf(options) : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String targetLanguage;
        private SemanticModel sourceModel;
        private List<GenerationStep> steps = Collections.emptyList();
        private Map<String, Object> options = Collections.emptyMap();

        public Builder targetLanguage(String targetLanguage) {
            this.targetLanguage = targetLanguage;
            return this;
        }

        public Builder sourceModel(SemanticModel sourceModel) {
            this.sourceModel = sourceModel;
            return this;
        }

        public Builder steps(List<GenerationStep> steps) {
            this.steps = steps;
            return this;
        }

        public Builder options(Map<String, Object> options) {
            this.options = options;
            return this;
        }

        public GenerationPlan build() {
            return new GenerationPlan(targetLanguage, sourceModel, steps, options);
        }
    }
}