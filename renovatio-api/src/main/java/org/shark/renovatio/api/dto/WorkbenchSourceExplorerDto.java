package org.shark.renovatio.api.dto;

import java.util.List;

/**
 * Read-only structural projection of a project's legacy COBOL, copybook and JCL
 * assets for the Workbench Source Explorer (GitHub issue #178).
 *
 * <p>This DTO never implies that analysis was executed; it is a navigable view
 * over files that already exist inside the project's workspace boundary.</p>
 */
public record WorkbenchSourceExplorerDto(List<SourceFile> files, List<Dataset> datasets) {

    public record SourceFile(
            String id,
            String name,
            String kind,
            String path,
            String hash,
            String encoding,
            String analysisStatus,
            List<Symbol> symbols,
            List<Diagnostic> diagnostics) {
    }

    public record Symbol(
            String id,
            String kind,
            String name,
            int line,
            int column,
            String parentId,
            String irCoordinate) {
    }

    public record Diagnostic(String severity, String message, int line) {
    }

    public record Dataset(String id, String name, List<String> referencedBy) {
    }
}
