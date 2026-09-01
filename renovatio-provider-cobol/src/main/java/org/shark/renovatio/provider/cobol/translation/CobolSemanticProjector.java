package org.shark.renovatio.provider.cobol.translation;

import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolContext;
import org.shark.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;
import org.shark.renovatio.cobol.ir.annotated.CobolAnnotation;
import org.shark.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.shark.renovatio.cobol.ir.annotated.DataIntentPayload;
import org.shark.renovatio.cobol.ir.model.CallStatement;
import org.shark.renovatio.cobol.ir.model.CobolDataItem;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.model.CobolStatement;
import org.shark.renovatio.cobol.ir.model.Db2Statement;
import org.shark.renovatio.cobol.ir.model.EvaluateStatement;
import org.shark.renovatio.cobol.ir.model.FileOperationStatement;
import org.shark.renovatio.cobol.ir.model.IfStatement;
import org.shark.renovatio.cobol.runtime.PicType;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;
import org.shark.renovatio.semantic.ir.SourceSpan;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/** Deterministically projects COBOL IR and accepted sidecar intent into target-neutral semantic IR. */
public final class CobolSemanticProjector {
    private final CobolIrIdentityProjector identities = new CobolIrIdentityProjector();

    public SemanticProgram project(CobolIntermediateModel model, String sourcePath, byte[] sourceBytes,
                                   Optional<String> dialect, Optional<AnnotatedCobolContext> annotatedContext) {
        Objects.requireNonNull(model, "model");
        byte[] bytes = Objects.requireNonNull(sourceBytes, "sourceBytes").clone();
        String programId = model.getProgramId();
        SourceSpan programSpan = wholeSource(sourcePath, bytes);
        String baseHash = identities.baseIrHash(model);
        List<String> evidence = new ArrayList<>();
        evidence.add(baseHash);
        annotatedContext.ifPresent(context -> context.sidecar().annotations().stream()
                .map(CobolAnnotation::annotationId).forEach(evidence::add));
        SourceProvenance provenance = new SourceProvenance(sourcePath, sha256(bytes), "COBOL",
                dialect == null ? Optional.empty() : dialect, evidence);

        Map<String, String> sourceNodes = sourceDataNodes(model);
        List<SemanticProgram.SemanticType> types = new ArrayList<>();
        Map<String, String> typeIdsByName = new HashMap<>();
        for (int index = 0; index < model.getDataItems().size(); index++) {
            CobolDataItem item = model.getDataItems().get(index);
            String role = "data-item:" + item.name().toUpperCase(Locale.ROOT) + ":" + index;
            var header = SemanticProgram.Header.create(programId, SemanticProgram.NodeKind.TYPE, role, programSpan);
            PicType pic = item.picType();
            SemanticProgram.TypeKind kind = typeKind(pic, item.picture());
            SemanticProgram.Signedness signedness = pic == null ? SemanticProgram.Signedness.UNKNOWN
                    : pic.signed() ? SemanticProgram.Signedness.SIGNED : SemanticProgram.Signedness.UNSIGNED;
            OptionalInt precision = pic == null ? OptionalInt.empty() : OptionalInt.of(pic.digits());
            OptionalInt scale = pic == null ? OptionalInt.empty() : OptionalInt.of(pic.scale());
            OptionalInt cardinality = item.occurs() == null ? OptionalInt.empty() : OptionalInt.of(item.occurs());
            types.add(new SemanticProgram.SemanticType(header, item.name(), kind, signedness, precision, scale,
                    cardinality, cardinality, List.of()));
            typeIdsByName.putIfAbsent(item.name().toUpperCase(Locale.ROOT), header.id());
        }

        List<SemanticProgram.DataIntent> intents = projectIntents(model, annotatedContext, programSpan, sourceNodes);
        Projection projection = statements(model, programSpan, typeIdsByName);
        SemanticProgram.ControlFlow controlFlow = controlFlow(model, programSpan);

        return new SemanticProgram("1", SemanticProgram.Header.create(programId,
                SemanticProgram.NodeKind.PROGRAM, "program", programSpan), programId, provenance, types, intents,
                projection.effects(), projection.io(), controlFlow, projection.unclassified());
    }

    private Map<String, String> sourceDataNodes(CobolIntermediateModel model) {
        Map<String, String> result = new LinkedHashMap<>();
        identities.nodes(model).stream()
                .filter(node -> node.nodeKind() == org.shark.renovatio.cobol.ir.annotated.AnnotatedNodeKind.DATA_ITEM)
                .forEach(node -> result.put(node.nodeId(), node.nodeId()));
        return result;
    }

    private List<SemanticProgram.DataIntent> projectIntents(CobolIntermediateModel model,
            Optional<AnnotatedCobolContext> context, SourceSpan span, Map<String, String> sourceNodes) {
        if (context == null || context.isEmpty()) return List.of();
        AnnotatedCobolContext value = context.orElseThrow();
        if (value.baseModel() != model && !identities.baseIrHash(value.baseModel()).equals(identities.baseIrHash(model))) {
            return List.of();
        }
        if (!identities.baseIrHash(model).equals(value.sidecar().baseIrHash())) return List.of();
        return value.sidecar().annotations().stream()
                .filter(annotation -> annotation.annotationFamily() == AnnotationFamily.DATA_INTENT)
                .filter(annotation -> annotation.review().reviewState() == AnnotationReview.ReviewState.ACCEPTED)
                .filter(annotation -> sourceNodes.containsKey(annotation.nodeId()))
                .sorted(java.util.Comparator.comparing(CobolAnnotation::nodeId)
                        .thenComparing(CobolAnnotation::annotationId))
                .map(annotation -> intent(model.getProgramId(), span, annotation)).toList();
    }

    private SemanticProgram.DataIntent intent(String programId, SourceSpan span, CobolAnnotation annotation) {
        DataIntentPayload payload = (DataIntentPayload) annotation.payload();
        SemanticProgram.IntentKind kind = switch (payload.construction()) {
            case REDEFINES -> SemanticProgram.IntentKind.OVERLAPPING_STORAGE;
            case OCCURS_DEPENDING_ON -> SemanticProgram.IntentKind.DEPENDENT_CARDINALITY;
        };
        return new SemanticProgram.DataIntent(SemanticProgram.Header.create(programId,
                SemanticProgram.NodeKind.DATA_INTENT, "data-intent:" + annotation.annotationId(), span),
                annotation.nodeId(), kind, payload.interpretation(), payload.assumptions(), annotation.annotationId());
    }

    private Projection statements(CobolIntermediateModel model, SourceSpan span, Map<String, String> typeIds) {
        List<SemanticProgram.SideEffect> effects = new ArrayList<>();
        List<SemanticProgram.IoOperation> io = new ArrayList<>();
        List<SemanticProgram.UnclassifiedDataAccess> residual = new ArrayList<>();
        int[] sequence = {0};
        model.getParagraphs().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                visitStatements(model.getProgramId(), entry.getValue().statements(), span, typeIds,
                        entry.getKey(), sequence, effects, io, residual));
        return new Projection(effects, io, residual);
    }

    private void visitStatements(String programId, List<CobolStatement> statements, SourceSpan span,
                                 Map<String, String> typeIds, String paragraph, int[] sequence,
                                 List<SemanticProgram.SideEffect> effects, List<SemanticProgram.IoOperation> io,
                                 List<SemanticProgram.UnclassifiedDataAccess> residual) {
        for (CobolStatement statement : statements) {
            int ordinal = sequence[0]++;
            String role = paragraph + ":" + ordinal;
            if (statement instanceof FileOperationStatement file) {
                SemanticProgram.Direction direction = switch (file.operationType()) {
                    case READ, OPEN, CLOSE -> SemanticProgram.Direction.READ;
                    case WRITE, REWRITE, DELETE -> SemanticProgram.Direction.WRITE;
                };
                io.add(new SemanticProgram.IoOperation(SemanticProgram.Header.create(programId,
                        SemanticProgram.NodeKind.IO_OPERATION, "file:" + role, span), SemanticProgram.IoKind.FILE,
                        file.operationType().name(), Optional.of(file.fileName()), direction, List.of()));
            } else if (statement instanceof Db2Statement db2) {
                String operation = firstToken(db2.sql());
                SemanticProgram.Direction direction = "SELECT".equals(operation)
                        ? SemanticProgram.Direction.READ : SemanticProgram.Direction.WRITE;
                io.add(new SemanticProgram.IoOperation(SemanticProgram.Header.create(programId,
                        SemanticProgram.NodeKind.IO_OPERATION, "database:" + role, span),
                        SemanticProgram.IoKind.DATABASE, operation, Optional.empty(), direction, List.of()));
            } else if (statement instanceof CallStatement call) {
                effects.add(new SemanticProgram.SideEffect(SemanticProgram.Header.create(programId,
                        SemanticProgram.NodeKind.SIDE_EFFECT, "external-call:" + role, span),
                        SemanticProgram.EffectKind.EXTERNAL_CALL, List.of(), "Calls " + call.target()));
            } else if (statement instanceof IfStatement branch) {
                visitStatements(programId, branch.thenStatements(), span, typeIds, paragraph, sequence, effects, io, residual);
                visitStatements(programId, branch.elseStatements(), span, typeIds, paragraph, sequence, effects, io, residual);
            } else if (statement instanceof EvaluateStatement evaluate) {
                evaluate.branches().forEach(branch -> visitStatements(programId, branch.statements(), span, typeIds,
                        paragraph, sequence, effects, io, residual));
            }
        }
    }

    private SemanticProgram.ControlFlow controlFlow(CobolIntermediateModel model, SourceSpan span) {
        Map<String, SemanticProgram.ControlFlowNode> nodes = new LinkedHashMap<>();
        Set<String> names = new HashSet<>(model.getControlFlowGraph().adjacency().keySet());
        model.getControlFlowGraph().adjacency().values().forEach(names::addAll);
        names.stream().sorted().forEach(name -> nodes.put(name, new SemanticProgram.ControlFlowNode(
                SemanticProgram.Header.create(model.getProgramId(), SemanticProgram.NodeKind.CONTROL_FLOW_NODE,
                        "control-flow:" + name, span))));
        List<SemanticProgram.ControlFlowEdge> edges = new ArrayList<>();
        model.getControlFlowGraph().adjacency().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                entry.getValue().stream().sorted().forEach(target -> edges.add(new SemanticProgram.ControlFlowEdge(
                        SemanticProgram.Header.create(model.getProgramId(), SemanticProgram.NodeKind.CONTROL_FLOW_EDGE,
                                "edge:" + entry.getKey() + ":" + target, span), nodes.get(entry.getKey()).header().id(),
                        nodes.get(target).header().id(), SemanticProgram.EdgeKind.UNKNOWN))));
        String entryName = model.getEntryParagraph().name();
        Optional<String> entry = Optional.ofNullable(nodes.get(entryName)).map(value -> value.header().id());
        return new SemanticProgram.ControlFlow(entry, new ArrayList<>(nodes.values()), edges);
    }

    private static SemanticProgram.TypeKind typeKind(PicType pic, String picture) {
        if (pic == null) return picture == null || picture.isBlank()
                ? SemanticProgram.TypeKind.GROUP : SemanticProgram.TypeKind.UNKNOWN;
        return switch (pic.category()) {
            case NUMERIC -> pic.scale() > 0 ? SemanticProgram.TypeKind.DECIMAL : SemanticProgram.TypeKind.INTEGER;
            case ALPHANUMERIC, ALPHABETIC -> SemanticProgram.TypeKind.TEXT;
        };
    }

    private static SourceSpan wholeSource(String sourcePath, byte[] bytes) {
        String source = new String(bytes, StandardCharsets.UTF_8);
        String[] lines = source.split("\\R", -1);
        int endLine = Math.max(1, lines.length);
        int endColumn = Math.max(1, lines[lines.length - 1].length());
        return new SourceSpan(sourcePath, 1, 1, endLine, endColumn);
    }

    private static String firstToken(String sql) {
        String trimmed = sql == null ? "" : sql.strip();
        if (trimmed.isEmpty()) return "UNKNOWN";
        int separator = trimmed.indexOf(' ');
        return (separator < 0 ? trimmed : trimmed.substring(0, separator)).toUpperCase(Locale.ROOT);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private record Projection(List<SemanticProgram.SideEffect> effects,
                              List<SemanticProgram.IoOperation> io,
                              List<SemanticProgram.UnclassifiedDataAccess> unclassified) { }
}
