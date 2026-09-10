package org.shark.renovatio.shared.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a semantic element within a semantic model.
 */
public record SemanticElement(
    String name,
    String kind,
    String type,
    List<SemanticElement> children,
    Map<String, Object> properties
) {
    public SemanticElement {
        children = children != null ? List.copyOf(children) : Collections.emptyList();
        properties = properties != null ? Map.copyOf(properties) : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String kind;
        private String type;
        private List<SemanticElement> children = Collections.emptyList();
        private Map<String, Object> properties = Collections.emptyMap();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder kind(String kind) {
            this.kind = kind;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder children(List<SemanticElement> children) {
            this.children = children;
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        public SemanticElement build() {
            return new SemanticElement(name, kind, type, children, properties);
        }
    }
}