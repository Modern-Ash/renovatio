package org.shark.renovatio.shared.equivalence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/** Writes a compact, canonical JSON evidence document suitable for an external release gate. */
public final class EquivalenceEvidenceWriter {
    private EquivalenceEvidenceWriter() { }

    public static void write(Path destination, EquivalenceReport report) throws IOException {
        Path parent = destination.toAbsolutePath().normalize().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(destination, render(report), StandardCharsets.UTF_8);
    }

    public static String render(EquivalenceReport report) {
        return "{\n"
                + "  \"fixtureId\": \"" + quote(report.fixtureId()) + "\",\n"
                + "  \"sourceHash\": \"" + quote(report.sourceHash()) + "\",\n"
                + "  \"targetHash\": \"" + quote(report.targetHash()) + "\",\n"
                + "  \"sourceInvariants\": " + map(report.sourceInvariants()) + ",\n"
                + "  \"targetInvariants\": " + map(report.targetInvariants()) + ",\n"
                + "  \"classification\": \"" + report.classification() + "\",\n"
                + "  \"reason\": \"" + quote(report.reason()) + "\",\n"
                + "  \"blocksRelease\": " + report.blocksRelease() + "\n"
                + "}\n";
    }

    private static String map(Map<String, String> values) {
        return new TreeMap<>(values).entrySet().stream()
                .map(entry -> "\"" + quote(entry.getKey()) + "\": \"" + quote(entry.getValue()) + "\"")
                .collect(java.util.stream.Collectors.joining(", ", "{", "}"));
    }

    private static String quote(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
