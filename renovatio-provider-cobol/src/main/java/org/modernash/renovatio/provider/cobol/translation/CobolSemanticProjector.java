package org.modernash.renovatio.provider.cobol.translation;

import org.modernash.renovatio.cobol.ir.annotated.AnnotatedCobolContext;
import org.modernash.renovatio.cobol.ir.annotated.AnnotationFamily;
import org.modernash.renovatio.cobol.ir.annotated.AnnotationReview;
import org.modernash.renovatio.cobol.ir.annotated.CobolAnnotation;
import org.modernash.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.modernash.renovatio.cobol.ir.annotated.DataIntentPayload;
import org.modernash.renovatio.cobol.ir.model.CallStatement;
import org.modernash.renovatio.cobol.ir.model.CobolDataItem;
import org.modernash.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.modernash.renovatio.cobol.ir.model.CobolStatement;
import org.modernash.renovatio.cobol.ir.model.ComputeStatement;
import org.modernash.renovatio.cobol.ir.model.Db2Statement;
import org.modernash.renovatio.cobol.ir.model.EvaluateStatement;
import org.modernash.renovatio.cobol.ir.model.FileOperationStatement;
import org.modernash.renovatio.cobol.ir.model.IfStatement;
import org.modernash.renovatio.cobol.ir.model.MoveStatement;
import org.modernash.renovatio.cobol.runtime.PicType;
import org.modernash.renovatio.semantic.ir.SemanticProgram;
import org.modernash.renovatio.semantic.ir.SourceProvenance;
import org.modernash.renovatio.semantic.ir.SourceSpan;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministically projects COBOL IR and accepted sidecar intent into target-neutral semantic IR. */
public final class CobolSemanticProjector {
    private static final Pattern DATA_REFERENCE = Pattern.compile("[A-Za-z][A-Za-z0-9-]*");
    private static final Pattern STRING_LITERAL = Pattern.compile("'[^']*'|\"[^\"]*\"");
    private static final Pattern CICS_COMMAND = Pattern.compile("EXEC\\s+CICS\\s+([A-Z0-9-]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Set<String> EXPRESSION_KEYWORDS = Set.of(
            "AND", "OR", "NOT", "IS", "EQUAL", "GREATER", "LESS", "THAN", "TO", "ZERO",
            "ZEROS", "ZEROES", "SPACE", "SPACES", "HIGH-VALUE", "LOW-VALUE", "TRUE", "FALSE",
            "IN", "OF");
    private final CobolIrIdentityProjector identities = new CobolIrIdentityProjector();
    private final Db2SqlResourceExtractor db2Resources = new Db2SqlResourceExtractor();

    public SemanticProgram project(CobolIntermediateModel model, String sourcePath, byte[] sourceBytes,
                                   Optional<String> dialect, Optional<AnnotatedCobolContext> annotatedContext) {
        Objects.requireNonNull(model, "model");
        byte[] bytes = Objects.requireNonNull(sourceBytes, "sourceBytes").clone();
        String programId = model.getProgramId();
        SourceSpan programSpan = wholeSource(sourcePath, bytes);
        String baseHash = identities.baseIrHash(model);

        List<SemanticProgram.SemanticType> types = new ArrayList<>();
        Map<String, String> typeIdsByName = new HashMap<>();
        Map<String, String> typeIdsBySourceNode = new LinkedHashMap<>();
        // Two passes: memberIds was never populated here at all — every
        // SemanticType this projector built passed List.of() regardless of
        // level, so a GROUP type could never show its own fields even once
        // it existed. Pass 1 builds each item's header/basic fields and
        // walks COBOL's level-number nesting (a higher level number is a
        // child of the most recent lower one — 01 owns the 05s under it,
        // a 05 owns the 10s under it, etc.) to work out parent -> children
        // by header id; pass 2 constructs the final immutable SemanticType
        // list with that hierarchy filled in.
        record PendingType(SemanticProgram.Header header, CobolDataItem item) { }
        List<PendingType> pending = new ArrayList<>();
        Map<String, List<String>> childHeaderIdsByParent = new LinkedHashMap<>();
        List<String> levelStackIds = new ArrayList<>();
        List<Integer> levelStackLevels = new ArrayList<>();
        for (int index = 0; index < model.getDataItems().size(); index++) {
            CobolDataItem item = model.getDataItems().get(index);
            String role = "data-item:" + item.name().toUpperCase(Locale.ROOT) + ":" + index;
            var header = SemanticProgram.Header.create(programId, SemanticProgram.NodeKind.TYPE, role, programSpan);
            pending.add(new PendingType(header, item));
            typeIdsByName.putIfAbsent(item.name().toUpperCase(Locale.ROOT), header.id());
            String sourceNodeId = identities.node(item, "/dataItems/" + index).nodeId();
            if (typeIdsBySourceNode.putIfAbsent(sourceNodeId, header.id()) != null) {
                throw new IllegalArgumentException("duplicate COBOL data-item node: " + sourceNodeId);
            }

            while (!levelStackLevels.isEmpty() && levelStackLevels.get(levelStackLevels.size() - 1) >= item.level()) {
                levelStackLevels.remove(levelStackLevels.size() - 1);
                levelStackIds.remove(levelStackIds.size() - 1);
            }
            if (!levelStackIds.isEmpty()) {
                childHeaderIdsByParent.computeIfAbsent(levelStackIds.get(levelStackIds.size() - 1),
                        key -> new ArrayList<>()).add(header.id());
            }
            levelStackLevels.add(item.level());
            levelStackIds.add(header.id());
        }
        for (PendingType entry : pending) {
            CobolDataItem item = entry.item();
            PicType pic = item.picType();
            SemanticProgram.TypeKind kind = typeKind(pic, item.picture());
            SemanticProgram.Signedness signedness = pic == null ? SemanticProgram.Signedness.UNKNOWN
                    : pic.signed() ? SemanticProgram.Signedness.SIGNED : SemanticProgram.Signedness.UNSIGNED;
            OptionalInt precision = pic == null ? OptionalInt.empty() : OptionalInt.of(pic.digits());
            OptionalInt scale = pic == null ? OptionalInt.empty() : OptionalInt.of(pic.scale());
            OptionalInt cardinality = item.occurs() == null ? OptionalInt.empty() : OptionalInt.of(item.occurs());
            List<String> memberIds = childHeaderIdsByParent.getOrDefault(entry.header().id(), List.of());
            types.add(new SemanticProgram.SemanticType(entry.header(), item.name(), kind, signedness, precision,
                    scale, cardinality, cardinality, memberIds));
        }

        List<CobolAnnotation> contributingAnnotations = contributingAnnotations(model, annotatedContext,
                typeIdsBySourceNode);
        List<SemanticProgram.DataIntent> intents = contributingAnnotations.stream()
                .map(annotation -> intent(model.getProgramId(), programSpan, annotation,
                        typeIdsBySourceNode.get(annotation.nodeId())))
                .toList();
        List<String> evidence = new ArrayList<>();
        evidence.add(baseHash);
        contributingAnnotations.forEach(annotation -> {
            evidence.add(annotation.annotationId());
            evidence.add(annotation.provenance().outputHash());
        });
        SourceProvenance provenance = new SourceProvenance(sourcePath, sha256(bytes), "COBOL",
                dialect == null ? Optional.empty() : dialect, evidence);

        Projection projection = statements(model, programSpan, typeIdsByName);
        List<SemanticProgram.IoOperation> ioOperations = new ArrayList<>(projection.io());
        projectCics(programId, new String(bytes, StandardCharsets.UTF_8), programSpan, ioOperations);
        SemanticProgram.ControlFlow controlFlow = controlFlow(model, programSpan);

        return new SemanticProgram("1", SemanticProgram.Header.create(programId,
                SemanticProgram.NodeKind.PROGRAM, "program", programSpan), programId, provenance, types, intents,
                projection.effects(), ioOperations, controlFlow, projection.unclassified());
    }

    private static void projectCics(String programId, String source, SourceSpan span,
                                    List<SemanticProgram.IoOperation> target) {
        Matcher matcher = CICS_COMMAND.matcher(source);
        int ordinal = 0;
        while (matcher.find()) {
            String command = matcher.group(1).toUpperCase(Locale.ROOT);
            target.add(new SemanticProgram.IoOperation(SemanticProgram.Header.create(programId,
                    SemanticProgram.NodeKind.IO_OPERATION, "cics:" + ordinal++, span),
                    SemanticProgram.IoKind.TRANSACTION, command, Optional.empty(),
                    SemanticProgram.Direction.UNKNOWN, List.of()));
        }
    }

    private List<CobolAnnotation> contributingAnnotations(CobolIntermediateModel model,
            Optional<AnnotatedCobolContext> context, Map<String, String> typeIdsBySourceNode) {
        if (context == null || context.isEmpty()) return List.of();
        AnnotatedCobolContext value = context.orElseThrow();
        if (value.baseModel() != model && !identities.baseIrHash(value.baseModel()).equals(identities.baseIrHash(model))) {
            return List.of();
        }
        if (!identities.baseIrHash(model).equals(value.sidecar().baseIrHash())) return List.of();
        return value.sidecar().annotations().stream()
                .filter(annotation -> annotation.annotationFamily() == AnnotationFamily.DATA_INTENT)
                .filter(annotation -> annotation.review().reviewState() == AnnotationReview.ReviewState.ACCEPTED)
                .filter(annotation -> typeIdsBySourceNode.containsKey(annotation.nodeId()))
                .sorted(java.util.Comparator.comparing(CobolAnnotation::nodeId)
                        .thenComparing(CobolAnnotation::annotationId))
                .toList();
    }

    private SemanticProgram.DataIntent intent(String programId, SourceSpan span, CobolAnnotation annotation,
                                              String semanticTypeId) {
        DataIntentPayload payload = (DataIntentPayload) annotation.payload();
        SemanticProgram.IntentKind kind = switch (payload.construction()) {
            case REDEFINES -> SemanticProgram.IntentKind.OVERLAPPING_STORAGE;
            case OCCURS_DEPENDING_ON -> SemanticProgram.IntentKind.DEPENDENT_CARDINALITY;
            case MOVE_CORRESPONDING -> SemanticProgram.IntentKind.MOVE_CORRESPONDING;
        };
        return new SemanticProgram.DataIntent(SemanticProgram.Header.create(programId,
                SemanticProgram.NodeKind.DATA_INTENT, "data-intent:" + annotation.annotationId(), span),
                semanticTypeId, kind, payload.interpretation(), payload.assumptions(), annotation.annotationId());
    }

    private Projection statements(CobolIntermediateModel model, SourceSpan span, Map<String, String> typeIds) {
        List<SemanticProgram.SideEffect> effects = new ArrayList<>();
        List<SemanticProgram.IoOperation> io = new ArrayList<>();
        List<SemanticProgram.UnclassifiedDataAccess> residual = new ArrayList<>();
        int[] sequence = {0};
        // FD name -> the 01-level record declared right under it in the
        // FILE SECTION, parsed structurally (see #280: name-similarity
        // guessing between a file and its record missed real pairs like
        // ACCTFILE-FILE/ACCOUNT-RECORD where COBOL naming abbreviations
        // diverge). Threaded down so a FILE-kind IoOperation can carry its
        // actual bound record symbol instead of leaving a domain projector
        // to guess it from the name alone.
        Map<String, String> fileToRecordMapping = stringMap(model, "getFileToRecordMapping");
        Map<String, java.util.List<String>> fileKeyFields = stringListMap(model, "getFileKeyFields");
        Map<String, String> fileAssignTarget = stringMap(model, "getFileAssignTarget");
        model.getParagraphs().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                visitStatements(model.getProgramId(), entry.getValue().statements(), span, typeIds,
                        entry.getKey(), sequence, effects, io, residual, fileToRecordMapping, fileKeyFields,
                        fileAssignTarget));
        return new Projection(effects, io, residual);
    }

    private void visitStatements(String programId, List<CobolStatement> statements, SourceSpan span,
                                 Map<String, String> typeIds, String paragraph, int[] sequence,
                                 List<SemanticProgram.SideEffect> effects, List<SemanticProgram.IoOperation> io,
                                 List<SemanticProgram.UnclassifiedDataAccess> residual,
                                 Map<String, String> fileToRecordMapping,
                                 Map<String, java.util.List<String>> fileKeyFields,
                                 Map<String, String> fileAssignTarget) {
        for (CobolStatement statement : statements) {
            int ordinal = sequence[0]++;
            String role = paragraph + ":" + ordinal;
            if (statement instanceof FileOperationStatement file) {
                SemanticProgram.Direction direction = switch (file.operationType()) {
                    case READ -> SemanticProgram.Direction.READ;
                    case WRITE, REWRITE, DELETE -> SemanticProgram.Direction.WRITE;
                    case OPEN, CLOSE -> SemanticProgram.Direction.UNKNOWN;
                };
                String boundRecord = fileToRecordMapping.get(file.fileName().toUpperCase(java.util.Locale.ROOT));
                java.util.List<String> keyFields = fileKeyFields.getOrDefault(
                        file.fileName().toUpperCase(java.util.Locale.ROOT), List.of());
                String assignTarget = fileAssignTarget.get(file.fileName().toUpperCase(java.util.Locale.ROOT));
                io.add(ioOperation(SemanticProgram.Header.create(programId,
                        SemanticProgram.NodeKind.IO_OPERATION, "file:" + role, span), SemanticProgram.IoKind.FILE,
                        file.operationType().name(), Optional.of(file.fileName()), direction,
                        boundRecord == null || boundRecord.isBlank() ? Optional.empty() : Optional.of(boundRecord),
                        keyFields,
                        assignTarget == null || assignTarget.isBlank() ? Optional.empty() : Optional.of(assignTarget)));
            } else if (statement instanceof Db2Statement db2) {
                String operation = firstToken(db2.sql());
                SemanticProgram.Direction direction = databaseDirection(operation);
                io.add(new SemanticProgram.IoOperation(SemanticProgram.Header.create(programId,
                        SemanticProgram.NodeKind.IO_OPERATION, "database:" + role, span),
                        SemanticProgram.IoKind.DATABASE, operation, db2Resources.physicalResource(operation, db2.sql()),
                        direction, List.of()));
            } else if (statement instanceof CallStatement call) {
                effects.add(new SemanticProgram.SideEffect(SemanticProgram.Header.create(programId,
                        SemanticProgram.NodeKind.SIDE_EFFECT, "external-call:" + role, span),
                        SemanticProgram.EffectKind.EXTERNAL_CALL, List.of(), "Calls " + call.target()));
                for (int index = 0; index < call.arguments().size(); index++) {
                    recordExpressionReads(programId, call.arguments().get(index), span, typeIds,
                            role + ":call-argument:" + index, effects, residual);
                }
            } else if (statement instanceof MoveStatement move) {
                recordExpressionReads(programId, move.source(), span, typeIds,
                        role + ":move-source", effects, residual);
                recordWrite(programId, move.target(), span, typeIds,
                        role + ":move-target", effects, residual);
            } else if (statement instanceof ComputeStatement compute) {
                recordExpressionReads(programId, compute.expression(), span, typeIds,
                        role + ":compute-expression", effects, residual);
                recordWrite(programId, compute.target(), span, typeIds,
                        role + ":compute-target", effects, residual);
            } else if (statement instanceof IfStatement branch) {
                recordExpressionReads(programId, branch.condition(), span, typeIds,
                        role + ":if-condition", effects, residual);
                visitStatements(programId, branch.thenStatements(), span, typeIds, paragraph, sequence, effects, io, residual, fileToRecordMapping, fileKeyFields, fileAssignTarget);
                visitStatements(programId, branch.elseStatements(), span, typeIds, paragraph, sequence, effects, io, residual, fileToRecordMapping, fileKeyFields, fileAssignTarget);
            } else if (statement instanceof EvaluateStatement evaluate) {
                recordExpressionReads(programId, evaluate.expression(), span, typeIds,
                        role + ":evaluate-expression", effects, residual);
                for (int index = 0; index < evaluate.branches().size(); index++) {
                    EvaluateStatement.EvaluateWhenBranch branch = evaluate.branches().get(index);
                    recordExpressionReads(programId, branch.condition(), span, typeIds,
                            role + ":when-condition:" + index, effects, residual);
                    visitStatements(programId, branch.statements(), span, typeIds,
                            paragraph, sequence, effects, io, residual, fileToRecordMapping, fileKeyFields, fileAssignTarget);
                }
            }
        }
    }

    private static SemanticProgram.Direction databaseDirection(String operation) {
        return switch (operation) {
            case "SELECT", "FETCH", "VALUES" -> SemanticProgram.Direction.READ;
            case "INSERT", "UPDATE", "DELETE", "MERGE" -> SemanticProgram.Direction.WRITE;
            default -> SemanticProgram.Direction.UNKNOWN;
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> stringMap(CobolIntermediateModel model, String methodName) {
        try {
            Object value = model.getClass().getMethod(methodName).invoke(model);
            if (!(value instanceof Map<?, ?> map)) return Map.of();
            Map<String, String> result = new LinkedHashMap<>();
            map.forEach((key, entry) -> {
                if (key != null && entry != null) result.put(key.toString(), entry.toString());
            });
            return result;
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return Map.of();
        }
    }

    private static Map<String, List<String>> stringListMap(CobolIntermediateModel model, String methodName) {
        try {
            Object value = model.getClass().getMethod(methodName).invoke(model);
            if (!(value instanceof Map<?, ?> map)) return Map.of();
            Map<String, List<String>> result = new LinkedHashMap<>();
            map.forEach((key, entry) -> {
                if (key == null || !(entry instanceof List<?> list)) return;
                result.put(key.toString(), list.stream().filter(Objects::nonNull).map(Object::toString).toList());
            });
            return result;
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return Map.of();
        }
    }

    private static SemanticProgram.IoOperation ioOperation(SemanticProgram.Header header,
                                                           SemanticProgram.IoKind kind,
                                                           String operation,
                                                           Optional<String> resourceReference,
                                                           SemanticProgram.Direction direction,
                                                           Optional<String> boundRecord,
                                                           List<String> keyFields,
                                                           Optional<String> assignTarget) {
        try {
            var constructor = SemanticProgram.IoOperation.class.getConstructor(SemanticProgram.Header.class,
                    SemanticProgram.IoKind.class, String.class, Optional.class, SemanticProgram.Direction.class,
                    List.class, Optional.class, List.class, Optional.class);
            return constructor.newInstance(header, kind, operation, resourceReference, direction, List.of(),
                    boundRecord, keyFields, assignTarget);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return new SemanticProgram.IoOperation(header, kind, operation, resourceReference, direction, List.of());
        }
    }

    private void recordExpressionReads(String programId, String expression, SourceSpan span,
                                       Map<String, String> typeIds, String role,
                                       List<SemanticProgram.SideEffect> effects,
                                       List<SemanticProgram.UnclassifiedDataAccess> residual) {
        if (expression == null || expression.isBlank() || isLiteral(expression)) return;
        Matcher matcher = DATA_REFERENCE.matcher(STRING_LITERAL.matcher(expression).replaceAll(" "));
        int reference = 0;
        while (matcher.find()) {
            String subject = matcher.group();
            if (EXPRESSION_KEYWORDS.contains(subject.toUpperCase(Locale.ROOT))) continue;
            recordRead(programId, subject, span, typeIds, role + ":" + reference++, effects, residual);
        }
    }

    private void recordRead(String programId, String subject, SourceSpan span, Map<String, String> typeIds,
                            String role, List<SemanticProgram.SideEffect> effects,
                            List<SemanticProgram.UnclassifiedDataAccess> residual) {
        recordStateAccess(programId, subject, "READ", SemanticProgram.EffectKind.STATE_READ,
                span, typeIds, role, effects, residual);
    }

    private void recordWrite(String programId, String subject, SourceSpan span, Map<String, String> typeIds,
                             String role, List<SemanticProgram.SideEffect> effects,
                             List<SemanticProgram.UnclassifiedDataAccess> residual) {
        recordStateAccess(programId, subject, "WRITE", SemanticProgram.EffectKind.STATE_WRITE,
                span, typeIds, role, effects, residual);
    }

    private void recordStateAccess(String programId, String subject, String operation,
                                   SemanticProgram.EffectKind effectKind, SourceSpan span,
                                   Map<String, String> typeIds, String role,
                                   List<SemanticProgram.SideEffect> effects,
                                   List<SemanticProgram.UnclassifiedDataAccess> residual) {
        if (subject == null || subject.isBlank() || isLiteral(subject)) return;
        String normalized = subject.strip().toUpperCase(Locale.ROOT);
        String typeId = typeIds.get(normalized);
        if (typeId != null) {
            effects.add(new SemanticProgram.SideEffect(SemanticProgram.Header.create(programId,
                    SemanticProgram.NodeKind.SIDE_EFFECT, "state-" + operation.toLowerCase(Locale.ROOT) + ":" + role, span),
                    effectKind, List.of(typeId), operation + " " + subject.strip()));
        } else {
            residual.add(new SemanticProgram.UnclassifiedDataAccess(SemanticProgram.Header.create(programId,
                    SemanticProgram.NodeKind.UNCLASSIFIED_DATA_ACCESS, "unclassified:" + role, span),
                    subject.strip(), operation, "No matching semantic data node", List.of()));
        }
    }

    private static boolean isLiteral(String value) {
        String text = value.strip();
        if (text.isEmpty()) return true;
        if ((text.startsWith("\"") && text.endsWith("\""))
                || (text.startsWith("'") && text.endsWith("'"))) return true;
        return text.matches("[+-]?[0-9]+(?:\\.[0-9]+)?");
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
