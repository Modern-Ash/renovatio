package org.shark.renovatio.shared.spi;

/**
 * Exception thrown when parsing fails.
 */
public class ParseException extends Exception {

    private final String language;
    private final int lineNumber;
    private final int columnNumber;

    public ParseException(String message, String language, int lineNumber, int columnNumber) {
        super(message);
        this.language = language;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public ParseException(String message, String language, Throwable cause) {
        super(message, cause);
        this.language = language;
        this.lineNumber = -1;
        this.columnNumber = -1;
    }

    public String getLanguage() {
        return language;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    @Override
    public String toString() {
        if (lineNumber > 0) {
            return String.format("Parse error in %s at line %d, column %d: %s",
                    language, lineNumber, columnNumber, getMessage());
        }
        return String.format("Parse error in %s: %s", language, getMessage());
    }
}