package org.shark.renovatio.cli.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.cli.OutputWriter;
import org.shark.renovatio.cli.review.ManualActionItemReport;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Command(name = "review", description = "Render manual action items as a severity-ordered checklist.")
public final class ReviewCommand extends AbstractCoreCommand {

    private static final Path DEFAULT_REPORT = Path.of("build", "reports", "renovatio", "manual-action-items.json");

    private static final Pattern[] SECRET_PATTERNS = {
            Pattern.compile("(?i)(authorization\\s*[:=]\\s*)(?:bearer\\s+)?[^\\s,;]+"),
            Pattern.compile("(?i)((?:api[-_ ]?key|token|secret|password)\\s*[:=]\\s*)[^\\s,;]+"),
            Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+"),
            Pattern.compile("(?<![A-Za-z0-9])sk-[A-Za-z0-9_-]{8,}")
    };

    private static String redact(String value) {
        if (value == null) return null;
        String result = value;
        for (Pattern p : SECRET_PATTERNS) {
            result = p.matcher(result).replaceAll(m ->
                    m.groupCount() == 0 ? "[REDACTED]" : m.group(1) + "[REDACTED]");
        }
        return result;
    }

    @Option(names = "--report",
            description = "Path to manual-action-items.json (default: build/reports/renovatio/manual-action-items.json).")
    String reportPath;

    @Option(names = "--severity",
            description = "Show items at or above this severity level (error, warning, info).")
    String minSeverity;

    private static final int SEVERITY_ORDER(String s) {
        if (s == null) return 99;
        return switch (s.toLowerCase()) {
            case "critical" -> 0;
            case "error" -> 1;
            case "warning" -> 2;
            case "info" -> 3;
            default -> 99;
        };
    }

    @Override
    public Integer call() {
        Path reportFile = reportPath != null
                ? Path.of(reportPath).toAbsolutePath()
                : DEFAULT_REPORT.toAbsolutePath();

        if (!Files.isRegularFile(reportFile)) {
            System.err.println("error: report not found: " + reportFile);
            return 1;
        }

        try {
            ObjectMapper mapper = OutputWriter.mapper();
            ManualActionItemReport report = mapper.readValue(reportFile.toFile(), ManualActionItemReport.class);

            List<ManualActionItemReport.Item> items = report.getItems();
            if (items == null) items = List.of();

            int minOrder = minSeverity != null ? SEVERITY_ORDER(minSeverity) : 99;
            String minSev = minSeverity != null ? minSeverity.toLowerCase() : null;

            List<ManualActionItemReport.Item> filtered = items.stream()
                    .filter(item -> {
                        if (minSev == null) return true;
                        return SEVERITY_ORDER(item.getSeverity()) <= minOrder;
                    })
                    .sorted(Comparator
                            .comparing((ManualActionItemReport.Item i) -> SEVERITY_ORDER(i.getSeverity()))
                            .thenComparing(i -> i.getFailedGate() != null ? i.getFailedGate() : "")
                            .thenComparing(i -> i.getId() != null ? i.getId() : ""))
                    .toList();

            if (filtered.isEmpty()) {
                if (json) {
                    output().writeJson(List.of());
                } else {
                    System.out.println("no manual action items");
                }
                return 0;
            }

            if (json) {
                Map<String, Object> wrapper = new LinkedHashMap<>();
                wrapper.put("schemaVersion", report.getSchemaVersion());
                wrapper.put("items", filtered);
                output().writeJson(wrapper);
                return 0;
            }

            for (ManualActionItemReport.Item item : filtered) {
                String sev = item.getSeverity() != null ? item.getSeverity() : "unknown";
                String gate = item.getFailedGate() != null ? item.getFailedGate() : "unknown";
                String program = item.getProgram() != null ? item.getProgram() : "unknown";

                System.out.printf("[ ] %s · %s · %s%n", sev, gate, program);
                printField("reason", item.getReason());
                printField("required action", item.getRequiredHumanAction());
                printField("acceptance", item.getAcceptanceCondition());
                printField("reference", item.getDiagnosticReference());
                System.out.println();
            }
            return 0;
        } catch (Exception e) {
            System.err.println("error: could not read report: " + e.getMessage());
            return 1;
        }
    }

    private void printField(String label, String value) {
        if (value != null && !value.isBlank()) {
            System.out.printf("    %-18s%s%n", label + ":", redact(value));
        }
    }
}
