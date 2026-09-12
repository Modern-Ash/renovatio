package org.shark.renovatio.api.dto;

import java.util.List;

public record WorkbenchDataMigrationDto(String projectId,
                                        String status,
                                        List<TablePlan> tables,
                                        DryRun dryRun,
                                        List<String> gaps,
                                        List<String> evidence) {
    public record TablePlan(String domainNodeId,
                            String targetTable,
                            String sourceDataset,
                            String state,
                            List<ColumnMapping> columns,
                            List<TransformationRule> transformations,
                            List<String> risks) { }

    public record ColumnMapping(String property,
                                String targetColumn,
                                String sourceColumn,
                                String sourceDataset,
                                String sourceType,
                                String targetType,
                                boolean key,
                                String transformation) { }

    public record TransformationRule(String column,
                                     String kind,
                                     String description) { }

    public record DryRun(String status,
                         int sampledRows,
                         List<PreviewRow> previewRows,
                         List<String> warnings) { }

    public record PreviewRow(String table,
                             List<Cell> cells) { }

    public record Cell(String column,
                       String sourceValue,
                       String transformedValue) { }
}
