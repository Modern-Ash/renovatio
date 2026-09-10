package org.shark.renovatio.shared.spi;

import java.nio.file.Path;

/**
 * Represents source code to be parsed.
 */
public record SourceCode(
    String language,
    String content,
    Path filePath,
    String encoding
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String language;
        private String content;
        private Path filePath;
        private String encoding = "UTF-8";

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder filePath(Path filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder encoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        public SourceCode build() {
            return new SourceCode(language, content, filePath, encoding);
        }
    }
}