package org.shark.renovatio.shared.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the semantic intermediate representation of source code.
 * This is the output of parsing and input for further processing.
 */
public record SemanticModel(
    String language,
    String packageName,
    String className,
    List<SemanticElement> elements,
    Map<String, Object> metadata
) {
    public SemanticModel {
        elements = elements != null ? List.copyOf(elements) : Collections.emptyList();
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String language;
        private String packageName;
        private String className;
        private List<SemanticElement> elements = Collections.emptyList();
        private Map<String, Object> metadata = Collections.emptyMap();

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder elements(List<SemanticElement> elements) {
            this.elements = elements;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public SemanticModel build() {
            return new SemanticModel(language, packageName, className, elements, metadata);
        }
    }
}