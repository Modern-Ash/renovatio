package org.shark.renovatio.cli.review;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Jackson binding for a {@code manual-action-item.v1} report file.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ManualActionItemReport {

    @JsonProperty("schemaVersion")
    private String schemaVersion;

    @JsonProperty("items")
    private List<Item> items;

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public List<Item> getItems() {
        return items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Item {

        @JsonProperty("id")
        private String id;

        @JsonProperty("sourceFile")
        private String sourceFile;

        @JsonProperty("program")
        private String program;

        @JsonProperty("reason")
        private String reason;

        @JsonProperty("failedGate")
        private String failedGate;

        @JsonProperty("diagnosticReference")
        private String diagnosticReference;

        @JsonProperty("requiredHumanAction")
        private String requiredHumanAction;

        @JsonProperty("acceptanceCondition")
        private String acceptanceCondition;

        @JsonProperty("severity")
        private String severity;

        @JsonProperty("reviewStatus")
        private String reviewStatus;

        public String getId() {
            return id;
        }

        public String getSourceFile() {
            return sourceFile;
        }

        public String getProgram() {
            return program;
        }

        public String getReason() {
            return reason;
        }

        public String getFailedGate() {
            return failedGate;
        }

        public String getDiagnosticReference() {
            return diagnosticReference;
        }

        public String getRequiredHumanAction() {
            return requiredHumanAction;
        }

        public String getAcceptanceCondition() {
            return acceptanceCondition;
        }

        public String getSeverity() {
            return severity;
        }

        public String getReviewStatus() {
            return reviewStatus;
        }
    }
}
