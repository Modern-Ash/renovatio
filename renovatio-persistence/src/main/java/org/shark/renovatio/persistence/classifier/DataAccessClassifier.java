package org.shark.renovatio.persistence.classifier;

import org.shark.renovatio.semantic.ir.SemanticIdentity;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceSpan;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure classifier that consumes a F2 SemanticProgram and produces DataAccessClassification records.
 */
public final class DataAccessClassifier {

    private static final String KEY_READ_OPS = List.of("READ", "READ KEY", "READ EQUAL", "READ NEXT", "READ PREVIOUS").stream().toList().toString();
    private static final List<String> VSAM_KEY_OPS = List.of("READ KEY", "READ EQUAL", "START");
    private static final List<String> VSAM_SEQ_OPS = List.of("READ NEXT", "READ PREVIOUS");
    private static final List<String> DB2_OPS = List.of("SELECT", "INSERT", "UPDATE", "DELETE", "MERGE", "EXEC SQL");

    /**
     * Classify all data accesses in the given semantic program.
     */
    public List<DataAccessClassification> classify(SemanticProgram program) {
        Objects.requireNonNull(program, "program");
        List<DataAccessClassification> result = new ArrayList<>();

        for (SemanticProgram.IoOperation io : program.ioOperations()) {
            result.add(classifyIoOperation(program, io));
        }

        for (SemanticProgram.UnclassifiedDataAccess unclassified : program.unclassifiedDataAccesses()) {
            result.add(classifyUnclassified(program, unclassified));
        }

        return result.stream()
                .sorted((a, b) -> a.id().compareTo(b.id()))
                .toList();
    }

    private DataAccessClassification classifyIoOperation(SemanticProgram program, SemanticProgram.IoOperation io) {
        String programId = program.programId();
        SourceSpan span = io.header().sourceSpan();
        String resourceRef = io.resourceReference().orElse(null);
        String op = io.operation().toUpperCase(Locale.ROOT);

        if (io.ioKind() == SemanticProgram.IoKind.DATABASE) {
            return buildClassification(programId, span, resourceRef,
                    DataAccessKind.EXEC_SQL,
                    DataAccessClassification.KeyShape.NONE,
                    new DataAccessClassification.RecordShape(null, Optional.ofNullable(resourceRef), List.of()),
                    1.0, io.header().semanticRole());
        }

        if (io.ioKind() == SemanticProgram.IoKind.FILE) {
            if (isVsamKeyAccess(op)) {
                return buildClassification(programId, span, resourceRef,
                        DataAccessKind.VSAM_KEY,
                        new DataAccessClassification.KeyShape(List.of(resourceRef != null ? resourceRef : "KEY")),
                        new DataAccessClassification.RecordShape(resourceRef, Optional.empty(), List.of()),
                        1.0, io.header().semanticRole());
            }
            if (isVsamSequentialAccess(op)) {
                return buildClassification(programId, span, resourceRef,
                        DataAccessKind.VSAM_SEQUENTIAL,
                        DataAccessClassification.KeyShape.NONE,
                        new DataAccessClassification.RecordShape(resourceRef, Optional.empty(), List.of()),
                        1.0, io.header().semanticRole());
            }
            return buildClassification(programId, span, resourceRef,
                    DataAccessKind.SEQUENTIAL_FD,
                    DataAccessClassification.KeyShape.NONE,
                    new DataAccessClassification.RecordShape(resourceRef, Optional.empty(), List.of()),
                    0.8, io.header().semanticRole());
        }

        return buildClassification(programId, span, resourceRef,
                DataAccessKind.RESIDUAL,
                DataAccessClassification.KeyShape.NONE,
                DataAccessClassification.RecordShape.UNKNOWN,
                0.0, io.header().semanticRole());
    }

    private DataAccessClassification classifyUnclassified(SemanticProgram program, SemanticProgram.UnclassifiedDataAccess ua) {
        String programId = program.programId();
        SourceSpan span = ua.header().sourceSpan();
        return buildClassification(programId, span, ua.subject(),
                DataAccessKind.RESIDUAL,
                DataAccessClassification.KeyShape.NONE,
                DataAccessClassification.RecordShape.UNKNOWN,
                0.0, ua.header().semanticRole());
    }

    private boolean isVsamKeyAccess(String op) {
        return VSAM_KEY_OPS.stream().anyMatch(op::contains);
    }

    private boolean isVsamSequentialAccess(String op) {
        return VSAM_SEQ_OPS.stream().anyMatch(op::contains);
    }

    private static String sha256(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private DataAccessClassification buildClassification(
            String programId, SourceSpan span, String resourceRef,
            DataAccessKind kind, DataAccessClassification.KeyShape keyShape,
            DataAccessClassification.RecordShape recordShape, double confidence, String role) {

        String normalizedProgram = SemanticIdentity.normalizeProgramId(programId);
        String normalizedResource = resourceRef != null ? resourceRef.toUpperCase(Locale.ROOT) : "";
        String normalizedRole = role != null ? role : "";
        String idInput = String.join("\n",
                "persistence-classification.v1",
                normalizedProgram,
                normalizedResource,
                kind.name(),
                span.sourcePath() + ":" + span.startLine() + ":" + span.startColumn() + ":" + span.endLine() + ":" + span.endColumn(),
                normalizedRole);
        String id = sha256(idInput);

        DataAccessClassification.ClassifierProvenance provenance = new DataAccessClassification.ClassifierProvenance(
                span.sourcePath(), "", "COBOL");

        return new DataAccessClassification(
                id, programId, kind,
                Optional.ofNullable(resourceRef),
                keyShape, recordShape,
                Optional.empty(), List.of(),
                confidence, List.of(), provenance);
    }
}
