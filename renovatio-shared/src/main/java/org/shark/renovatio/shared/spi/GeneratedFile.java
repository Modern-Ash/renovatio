package org.shark.renovatio.shared.spi;

import java.nio.file.Path;

/**
 * Represents a single generated file.
 */
public record GeneratedFile(
    String name,
    String content,
    Path path,
    String language
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String content;
        private Path path;
        private String language;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder path(Path path) {
            this.path = path;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public GeneratedFile build() {
            return new GeneratedFile(name, content, path, language);
        }
    }
}