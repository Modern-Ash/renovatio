package org.shark.renovatio.shared.emission;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.semantic.ir.SemanticProgram;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Immutable target-neutral architectural slice carried to an emitter. */
public record TargetStructure(String schemaVersion, String requestHash, String moduleId,
                              MigrationProfile.ArchitectureStyle requestedStyle,
                              MigrationProfile.ArchitectureStyle effectiveStyle,
                              List<String> componentIds, List<String> artifactPaths,
                              List<String> diagnosticCodes) {
    public static final String SCHEMA_VERSION = "1";

    public TargetStructure {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("unsupported target structure schemaVersion");
        }
        requestHash = hash(requestHash, "requestHash");
        moduleId = hash(moduleId, "moduleId");
        Objects.requireNonNull(requestedStyle, "requestedStyle");
        Objects.requireNonNull(effectiveStyle, "effectiveStyle");
        componentIds = hashes(componentIds, "componentId");
        artifactPaths = text(artifactPaths, "artifactPath");
        diagnosticCodes = text(diagnosticCodes, "diagnosticCode");
    }

    public static TargetStructure identity(SemanticProgram program, String profileHash,
                                           MigrationProfile.ArchitectureStyle style) {
        Objects.requireNonNull(program, "program");
        String request = sha256("architecture.identity\n" + profileHash + "\n"
                + program.sourceProvenance().contentSha256());
        String module = sha256("architecture.module\n" + program.programId());
        return new TargetStructure(SCHEMA_VERSION, request, module, style, style,
                List.of(), List.of(), List.of());
    }

    private static List<String> hashes(List<String> values, String name) {
        return (values == null ? List.<String>of() : values).stream()
                .map(value -> hash(value, name)).distinct().sorted().toList();
    }

    private static List<String> text(List<String> values, String name) {
        return (values == null ? List.<String>of() : values).stream()
                .map(value -> textValue(value, name)).distinct().sorted().toList();
    }

    private static String hash(String value, String name) {
        String normalized = textValue(value, name).toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(name + " must be a lowercase SHA-256");
        }
        return normalized;
    }

    private static String textValue(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
        if (normalized.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
