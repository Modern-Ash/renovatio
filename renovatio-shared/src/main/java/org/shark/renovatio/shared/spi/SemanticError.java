package org.shark.renovatio.shared.spi;

/**
 * Represents a validation error in a semantic model.
 */
public record SemanticError(
    String message,
    ErrorSeverity severity,
    int lineNumber,
    int columnNumber,
    String code
) {
    public enum ErrorSeverity {
        ERROR,
        WARNING,
        INFO
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private ErrorSeverity severity = ErrorSeverity.ERROR;
        private int lineNumber = -1;
        private int columnNumber = -1;
        private String code;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder severity(ErrorSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder lineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public Builder columnNumber(int columnNumber) {
            this.columnNumber = columnNumber;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public SemanticError build() {
            return new SemanticError(message, severity, lineNumber, columnNumber, code);
        }
    }
}