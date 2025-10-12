package org.shark.renovatio.provider.cobol.domain;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

/**
 * Represents a parsed COBOL program structure
 */
@Getter
@Setter
public class CobolProgram {
    private String programId;
    private String programName;
    private CobolEnvironmentDivision environmentDivision;
    private CobolDataDivision dataDivision;
    private CobolProcedureDivision procedureDivision;
    private Map<String, Object> metadata;

    public CobolProgram() {
    }

    public CobolProgram(String programId, String programName) {
        this.programId = programId;
        this.programName = programName;
    }
}

/**
 * COBOL Environment Division representation
 */
@Getter
@Setter
class CobolEnvironmentDivision {
    private Map<String, String> configurationSection;
    private Map<String, String> inputOutputSection;
}

/**
 * COBOL Data Division representation
 */
@Getter
@Setter
class CobolDataDivision {
    private List<CobolDataItem> workingStorageSection;
    private List<CobolDataItem> fileSection;
    private List<CobolDataItem> linkageSection;
}

/**
 * COBOL Procedure Division representation
 */
@Getter
@Setter
class CobolProcedureDivision {
    private List<CobolParagraph> paragraphs;
    private List<CobolSection> sections;
}