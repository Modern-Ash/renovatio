package org.shark.renovatio.shared.spi;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents generated code output.
 */
public record GeneratedCode(
    String language,
    String content,
    Path outputPath,
    List<GeneratedFile> files,
    Map<String, Object> metadata
) {
    public GeneratedCode {
        files = files != null ? List.copyOf(files) : Collections.emptyList();
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String language;
        private String content;
        private Path outputPath;
        private List<GeneratedFile> files = Collections.emptyList();
        private Map<String, Object> metadata = Collections.emptyMap();

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder outputPath(Path outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public Builder files(List<GeneratedFile> files) {
            this.files = files;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public GeneratedCode build() {
            return new GeneratedCode(language, content, outputPath, files, metadata);
        }
    }
}