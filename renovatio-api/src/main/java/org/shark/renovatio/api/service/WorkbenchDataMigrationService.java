package org.shark.renovatio.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.shark.renovatio.api.dto.WorkbenchChangeSetDto;
import org.shark.renovatio.api.dto.WorkbenchDataMigrationDto;
import org.shark.renovatio.domain.model.DomainModel;
import org.springframework.stereotype.Service;

@Service
public class WorkbenchDataMigrationService {
    private final WorkbenchDomainModelService domainModels;

    public WorkbenchDataMigrationService(WorkbenchDomainModelService domainModels) {
        this.domainModels = domainModels;
    }

    public WorkbenchDataMigrationDto plan(String projectId) {
        DomainModel model = domainModels.read(projectId).model();
        List<WorkbenchDataMigrationDto.TablePlan> tables = model.nodes().stream()
                .filter(node -> node.tableName() != null || node.sourceDataset() != null
                        || node.properties().stream().anyMatch(property -> property.columnName() != null
                        || property.sourceColumn() != null || property.sourceDataset() != null))
                .map(this::tablePlan)
                .toList();
        List<String> gaps = new ArrayList<>();
        if (tables.isEmpty()) {
            gaps.add("No DomainModel nodes define table/source mapping metadata yet.");
        }
        tables.forEach(table -> {
            if (table.sourceDataset() == null || table.sourceDataset().isBlank()) {
                gaps.add("Missing source dataset for " + table.domainNodeId());
            }
            table.columns().forEach(column -> {
                if (column.sourceColumn() == null || column.sourceColumn().isBlank()) {
                    gaps.add("Missing source column for " + table.domainNodeId() + "." + column.property());
                }
            });
        });
        String status = gaps.isEmpty() ? "planned" : "not-planned";
        return new WorkbenchDataMigrationDto(projectId, status, tables, dryRun(tables, gaps),
                List.copyOf(gaps), evidence(tables));
    }

    public WorkbenchChangeSetDto.CreateRequest generateChangeSetRequest(String projectId) {
        WorkbenchDataMigrationDto plan = plan(projectId);
        if (plan.tables().isEmpty()) {
            throw new DataMigrationException("No data migration mappings are available.");
        }
        List<WorkbenchChangeSetDto.FileChangeRequest> files = new ArrayList<>();
        files.add(new WorkbenchChangeSetDto.FileChangeRequest(
                "generated-data-migration/" + projectId + "/README.md", "create", readme(plan)));
        files.add(new WorkbenchChangeSetDto.FileChangeRequest(
                "generated-data-migration/" + projectId + "/staging-load.sql", "create", stagingSql(plan)));
        files.add(new WorkbenchChangeSetDto.FileChangeRequest(
                "generated-data-migration/" + projectId + "/dry-run-report.json", "create", dryRunJson(plan)));
        return new WorkbenchChangeSetDto.CreateRequest(
                "Generate governed data migration dry-run for " + projectId,
                true,
                files,
                List.of("data-migration-dry-run", "human-approval-required", "rollback-requires-snapshot-or-staging-swap"),
                plan.evidence());
    }

    private WorkbenchDataMigrationDto.TablePlan tablePlan(DomainModel.DomainNode node) {
        String targetTable = node.tableName() == null ? snake(node.name()) : node.tableName();
        String sourceDataset = node.sourceDataset();
        List<WorkbenchDataMigrationDto.ColumnMapping> columns = node.properties().stream()
                .map(property -> {
                    String targetColumn = property.columnName() == null ? snake(property.name()) : property.columnName();
                    String sourceColumn = property.sourceColumn();
                    String propertyDataset = property.sourceDataset() == null ? sourceDataset : property.sourceDataset();
                    String transformation = transformationFor(property.type(), property.required());
                    return new WorkbenchDataMigrationDto.ColumnMapping(property.name(), targetColumn, sourceColumn,
                            propertyDataset, sourceType(property.type()), property.type(), property.isKey(), transformation);
                })
                .toList();
        List<WorkbenchDataMigrationDto.TransformationRule> transformations = columns.stream()
                .filter(column -> !"none".equals(column.transformation()))
                .map(column -> new WorkbenchDataMigrationDto.TransformationRule(column.targetColumn(),
                        column.transformation(), description(column.transformation(), column.sourceType(), column.targetType())))
                .toList();
        List<String> risks = new ArrayList<>();
        if (sourceDataset == null || sourceDataset.isBlank()) risks.add("source-dataset-missing");
        if (columns.stream().noneMatch(WorkbenchDataMigrationDto.ColumnMapping::key)) risks.add("primary-key-not-marked");
        if (columns.stream().anyMatch(column -> column.sourceColumn() == null || column.sourceColumn().isBlank())) {
            risks.add("source-column-missing");
        }
        return new WorkbenchDataMigrationDto.TablePlan(node.id(), targetTable, sourceDataset,
                risks.isEmpty() ? "planned" : "not-planned", columns, transformations, List.copyOf(risks));
    }

    private WorkbenchDataMigrationDto.DryRun dryRun(List<WorkbenchDataMigrationDto.TablePlan> tables, List<String> gaps) {
        List<WorkbenchDataMigrationDto.PreviewRow> rows = tables.stream().limit(3)
                .map(table -> new WorkbenchDataMigrationDto.PreviewRow(table.targetTable(),
                        table.columns().stream().limit(6)
                                .map(column -> new WorkbenchDataMigrationDto.Cell(column.targetColumn(),
                                        sampleValue(column), transformSample(column)))
                                .toList()))
                .toList();
        return new WorkbenchDataMigrationDto.DryRun(gaps.isEmpty() ? "dry-run-ok" : "dry-run-failed",
                rows.size(), rows, List.copyOf(gaps));
    }

    private List<String> evidence(List<WorkbenchDataMigrationDto.TablePlan> tables) {
        return tables.stream()
                .flatMap(table -> table.columns().stream()
                        .map(column -> table.domainNodeId() + ":" + table.sourceDataset() + ":" + column.sourceColumn()))
                .filter(value -> !value.contains("null"))
                .distinct()
                .toList();
    }

    private static String readme(WorkbenchDataMigrationDto plan) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Governed Data Migration Dry Run\n\n");
        builder.append("Project: ").append(plan.projectId()).append("\n\n");
        builder.append("Status: ").append(plan.status()).append("\n\n");
        builder.append("This change set is a dry-run artifact. Production execution requires a pre-load snapshot or staging-table swap for rollback.\n\n");
        builder.append("## Tables\n\n");
        for (WorkbenchDataMigrationDto.TablePlan table : plan.tables()) {
            builder.append("- ").append(table.targetTable()).append(" from ")
                    .append(Objects.toString(table.sourceDataset(), "<missing-source>"))
                    .append(" (").append(table.state()).append(")\n");
        }
        if (!plan.gaps().isEmpty()) {
            builder.append("\n## Gaps\n\n");
            plan.gaps().forEach(gap -> builder.append("- ").append(gap).append("\n"));
        }
        return builder.toString();
    }

    private static String stagingSql(WorkbenchDataMigrationDto plan) {
        StringBuilder builder = new StringBuilder("-- Generated by Renovatio Workbench data migration dry-run\n");
        builder.append("-- Rollback strategy: load into staging tables, verify checksums, then swap or discard staging.\n\n");
        for (WorkbenchDataMigrationDto.TablePlan table : plan.tables()) {
            builder.append("CREATE TABLE IF NOT EXISTS staging_").append(table.targetTable()).append(" (\n");
            for (int i = 0; i < table.columns().size(); i++) {
                WorkbenchDataMigrationDto.ColumnMapping column = table.columns().get(i);
                builder.append("  ").append(column.targetColumn()).append(" ").append(sqlType(column.targetType()));
                if (column.key()) builder.append(" PRIMARY KEY");
                builder.append(i + 1 == table.columns().size() ? "\n" : ",\n");
            }
            builder.append(");\n\n");
        }
        return builder.toString();
    }

    private static String dryRunJson(WorkbenchDataMigrationDto plan) {
        StringBuilder builder = new StringBuilder("{\n");
        builder.append("  \"projectId\": \"").append(escape(plan.projectId())).append("\",\n");
        builder.append("  \"status\": \"").append(escape(plan.dryRun().status())).append("\",\n");
        builder.append("  \"sampledRows\": ").append(plan.dryRun().sampledRows()).append(",\n");
        builder.append("  \"warnings\": [");
        for (int i = 0; i < plan.dryRun().warnings().size(); i++) {
            builder.append("\"").append(escape(plan.dryRun().warnings().get(i))).append("\"");
            if (i + 1 < plan.dryRun().warnings().size()) builder.append(", ");
        }
        builder.append("]\n}\n");
        return builder.toString();
    }

    private static String transformationFor(String type, boolean required) {
        String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (normalized.contains("comp-3") || normalized.contains("decimal") || normalized.contains("numeric")) {
            return "numeric-conversion";
        }
        return required ? "trim-spaces" : "blank-to-null";
    }

    private static String description(String transformation, String sourceType, String targetType) {
        return switch (transformation) {
            case "numeric-conversion" -> "Convert " + sourceType + " legacy numeric representation into " + targetType + ".";
            case "blank-to-null" -> "Map COBOL spaces/low-values to NULL for optional target fields.";
            case "trim-spaces" -> "Trim fixed-width COBOL padding before loading required target fields.";
            default -> "No transformation.";
        };
    }

    private static String sourceType(String type) {
        String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (normalized.contains("comp-3")) return "COMP-3";
        if (normalized.contains("decimal") || normalized.contains("numeric")) return "PIC 9";
        return "PIC X";
    }

    private static String sampleValue(WorkbenchDataMigrationDto.ColumnMapping column) {
        if ("PIC 9".equals(column.sourceType()) || "COMP-3".equals(column.sourceType())) return "00000123";
        return (column.sourceColumn() == null || column.sourceColumn().isBlank() ? column.property() : column.sourceColumn()).toUpperCase(Locale.ROOT);
    }

    private static String transformSample(WorkbenchDataMigrationDto.ColumnMapping column) {
        return switch (column.transformation()) {
            case "numeric-conversion" -> "123";
            case "blank-to-null" -> sampleValue(column).isBlank() ? "NULL" : sampleValue(column).trim();
            default -> sampleValue(column).trim();
        };
    }

    private static String snake(String value) {
        return value == null || value.isBlank() ? "unnamed" : value.trim()
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toLowerCase(Locale.ROOT);
    }

    private static String sqlType(String type) {
        String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (normalized.contains("int") || normalized.contains("long")) return "BIGINT";
        if (normalized.contains("decimal") || normalized.contains("numeric") || normalized.contains("comp-3")) return "DECIMAL(18, 2)";
        if (normalized.contains("date")) return "DATE";
        return "VARCHAR(255)";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static class DataMigrationException extends RuntimeException {
        public DataMigrationException(String message) {
            super(message);
        }
    }
}
