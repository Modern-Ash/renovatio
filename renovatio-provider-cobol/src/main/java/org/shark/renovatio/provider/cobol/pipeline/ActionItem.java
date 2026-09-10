package org.shark.renovatio.provider.cobol.pipeline;

/**
 * Represents a semantic gap detected during COBOL-to-Java translation.
 * Action items track unsupported COBOL constructs that need attention.
 */
public record ActionItem(
    String statementType,
    String location,
    String paragraph,
    Severity severity,
    String description,
    String suggestion
) {
    /**
     * Severity levels for action items.
     */
    public enum Severity {
        /**
         * Blocking issue that prevents successful translation.
         */
        BLOCKING,
        
        /**
         * Warning that should be addressed but doesn't block.
         */
        WARNING,
        
        /**
         * Informational item for tracking purposes.
         */
        INFO
    }

    /**
     * Create a blocking action item.
     */
    public static ActionItem blocking(String statementType, String location, String paragraph, 
                                     String description, String suggestion) {
        return new ActionItem(
            statementType,
            location,
            paragraph,
            Severity.BLOCKING,
            description,
            suggestion
        );
    }

    /**
     * Create a warning action item.
     */
    public static ActionItem warning(String statementType, String location, String paragraph, 
                                    String description, String suggestion) {
        return new ActionItem(
            statementType,
            location,
            paragraph,
            Severity.WARNING,
            description,
            suggestion
        );
    }

    /**
     * Create an info action item.
     */
    public static ActionItem info(String statementType, String location, String paragraph, 
                                 String description, String suggestion) {
        return new ActionItem(
            statementType,
            location,
            paragraph,
            Severity.INFO,
            description,
            suggestion
        );
    }

    /**
     * Check if this action item blocks the pipeline.
     */
    public boolean isBlocking() {
        return severity == Severity.BLOCKING;
    }
}
