package org.modernash.renovatio.domain.model;

import org.modernash.renovatio.semantic.ir.SemanticProgram;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import static org.modernash.renovatio.domain.model.DomainModel.*;

/** Pure, conservative projection from Semantic IR to the neutral business model. */
public final class SemanticDomainProjector {
    public DomainModel project(String projectId, List<SemanticProgram> programs) {
        if (projectId == null || projectId.isBlank()) throw new IllegalArgumentException("projectId is required");
        List<DomainNode> nodes = new ArrayList<>();
        List<DomainRelation> relations = new ArrayList<>();
        // Boundary node id -> the record symbol it's structurally bound to
        // (the FD's actual "RECORD IS ..."), when the provider was able to
        // parse it — collected across every program so the cross-program
        // merge pass at the end can use it instead of guessing from names.
        Map<String, String> boundaryStructuralRecordById = new LinkedHashMap<>();
        // Repository name (upper-cased) -> the field name(s) declared as
        // its own RECORD KEY / ALTERNATE RECORD KEY — a VSAM/indexed
        // file's real identity key(s). Used after every program is
        // projected to infer relations *between different* entities (not
        // the file/record collapse above, which is the same resource
        // twice): if repository A carries a property with this exact
        // name, A holds a foreign key into repository B.
        Map<String, List<String>> keyFieldsByRepositoryName = new LinkedHashMap<>();
        // Repository name (upper-cased) -> the physical dataset it's
        // ASSIGN TO. The same physical VSAM/indexed file is frequently
        // declared under a different local SELECT name in every program
        // that opens it (ACCT-FILE, ACCOUNT-FILE, ACCTFILE-FILE all
        // ASSIGN TO ACCTFILE) — names too different for the fuzzy/
        // structural merge below to catch, yet unambiguously the same
        // resource. Used to merge those before FK inference runs, so a
        // same-file duplicate never gets mistaken for a foreign key.
        Map<String, String> assignTargetByRepositoryName = new LinkedHashMap<>();
        // Semantic field (type) id -> the repository it structurally
        // belongs to, i.e. it's a (possibly nested) member of that
        // repository's bound record. Built from the same structural FD ->
        // record binding as the file/record collapse above, extended
        // recursively through GROUP memberIds. Together with fieldFlows
        // below, this is what lets a real MOVE the program performs
        // ("MOVE XREF-ACCT-ID TO WS-ACCT-ID" before a keyed READ) be
        // traced back to "this field belongs to repository A" without any
        // hardcoded name — the actual procedure-division logic is the
        // evidence, per "podemos acceder al corte del control... por el
        // proceso que hace".
        Map<String, String> fieldTypeIdToRepositoryName = new LinkedHashMap<>();
        // Semantic field (type) id -> its own symbol name, upper-cased —
        // used to test a MOVE's target field against another repository's
        // declared key field name.
        Map<String, String> typeIdToFieldName = new LinkedHashMap<>();
        // Every MOVE (or equivalent single-field assignment) every program
        // performs, collected globally — the field ids are already unique
        // per-program (the id hash includes programId), so no collision
        // risk collecting them into one flat list.
        List<SemanticProgram.FieldFlow> allFieldFlows = new ArrayList<>();
        for (SemanticProgram program : programs == null ? List.<SemanticProgram>of() : programs) {
            String aggregateId = "aggregate:" + program.programId();
            String useCaseId = "use-case:" + program.programId();
            Evidence evidence = evidence(program, "program header");
            nodes.add(new DomainNode(aggregateId, Kind.AGGREGATE, program.programId(), List.of(evidence), Origin.DETERMINISTIC, 1.0));
            nodes.add(new DomainNode(useCaseId, Kind.USE_CASE, "Process " + program.programId(), List.of(evidence), Origin.DETERMINISTIC, 0.8));
            relations.add(new DomainRelation("relation:" + aggregateId + ":" + useCaseId,
                    aggregateId, useCaseId, RelationKind.CONTAINS));

            // Looked up when a GROUP type's memberIds need resolving to
            // their own symbol/typeKind below — memberIds are semantic
            // node ids, and for a GROUP type they're its child fields.
            Map<String, SemanticProgram.SemanticType> typeById = new LinkedHashMap<>();
            Map<String, String> typeIdBySymbol = new LinkedHashMap<>();
            for (SemanticProgram.SemanticType type : program.types()) {
                typeById.put(type.header().id(), type);
                if (type.symbol() != null && !type.symbol().isBlank()) {
                    String symbolKeyUpper = type.symbol().trim().toUpperCase(java.util.Locale.ROOT);
                    typeIdBySymbol.putIfAbsent(symbolKeyUpper, type.header().id());
                    typeIdToFieldName.put(type.header().id(), symbolKeyUpper);
                }
            }
            allFieldFlows.addAll(program.fieldFlows());

            Map<String, DomainNode> typeNodes = new LinkedHashMap<>();
            for (SemanticProgram.SemanticType type : program.types()) {
                if (type.symbol() == null || type.symbol().isBlank() || "FILLER".equalsIgnoreCase(type.symbol().trim())) {
                    continue;
                }
                String symbol = type.symbol().trim();
                String symbolKey = symbol.replaceAll("[^A-Za-z0-9_-]", "_");
                String id = "entity:" + program.programId() + ":" + symbolKey;
                Kind kind = type.typeKind() == SemanticProgram.TypeKind.GROUP ? Kind.ENTITY : Kind.VALUE_OBJECT;
                Evidence typeEvidence = evidence(program, symbol);
                // A GROUP type's own properties were never populated (every
                // DomainNode here used the no-properties constructor) —
                // that's the source of the "0 fields / Shape unresolved"
                // gap reported earlier, and it also meant a mapped record
                // could never pass a persistence-shape check (no columns
                // to show). Its memberIds are the child fields — walk them
                // into real Property entries instead of leaving the box
                // empty.
                List<Property> properties = kind == Kind.ENTITY ? groupProperties(program, type, typeById) : List.of();
                DomainNode existing = typeNodes.get(id);
                if (existing == null) {
                    typeNodes.put(id, new DomainNode(id, kind, symbol, properties, List.of(typeEvidence),
                            Origin.DETERMINISTIC, 0.9, null, null));
                } else {
                    Kind effectiveKind = (existing.kind() == Kind.ENTITY || kind == Kind.ENTITY) ? Kind.ENTITY : Kind.VALUE_OBJECT;
                    List<Evidence> evidenceList = new ArrayList<>(existing.evidence());
                    if (evidenceList.stream().noneMatch(e -> e.rationale().equalsIgnoreCase(symbol))) {
                        evidenceList.add(typeEvidence);
                    }
                    List<Property> mergedProperties = mergePropertiesByName(existing.properties(), properties);
                    typeNodes.put(id, new DomainNode(id, effectiveKind, existing.name(), mergedProperties, evidenceList,
                            Origin.DETERMINISTIC, Math.max(existing.confidence(), 0.9), null, null));
                }
            }

            Map<String, DomainNode> boundaryNodes = new LinkedHashMap<>();
            // Tracks which boundary ids came from plain COBOL FILE I/O (an
            // FD/SELECT), as opposed to DATABASE (embedded SQL) access —
            // only FILE-kind repositories are eligible for the file/record
            // merge below. A DB2 table and a COBOL host-variable record
            // genuinely can be two distinct things (one table, several
            // different projections queried into different records), so
            // those stay separate nodes joined by MAPS_TO.
            Map<String, Boolean> boundaryIsFileKind = new LinkedHashMap<>();
            for (SemanticProgram.IoOperation io : program.ioOperations()) {
                String resource = io.resourceReference().orElse("");
                boolean persistentResource = (io.ioKind() == SemanticProgram.IoKind.DATABASE
                        || (io.ioKind() == SemanticProgram.IoKind.FILE && !looksLikeOneWayInterchangeFile(resource)))
                        && isPhysicalResourceName(resource, io.operation());
                Kind kind = persistentResource ? Kind.REPOSITORY : Kind.EXTERNAL_SYSTEM;
                String name = io.resourceReference().orElse(io.operation()).trim();
                String resourceKey = name.replaceAll("[^A-Za-z0-9_-]", "_");
                String id = "boundary:" + program.programId() + ":" + resourceKey;
                Evidence newEvidence = evidence(program, io.operation());
                boundaryIsFileKind.merge(id, io.ioKind() == SemanticProgram.IoKind.FILE, (a, b) -> a || b);
                io.boundRecordSymbol().filter(value -> !value.isBlank())
                        .ifPresent(value -> {
                            boundaryStructuralRecordById.putIfAbsent(id, value);
                            String rootTypeId = typeIdBySymbol.get(value.trim().toUpperCase(java.util.Locale.ROOT));
                            if (rootTypeId != null) {
                                for (String memberTypeId : descendantTypeIds(typeById, rootTypeId)) {
                                    fieldTypeIdToRepositoryName.putIfAbsent(memberTypeId, name.toUpperCase(java.util.Locale.ROOT));
                                }
                            }
                        });
                if (!io.keyFieldSymbols().isEmpty()) {
                    keyFieldsByRepositoryName.putIfAbsent(name.toUpperCase(java.util.Locale.ROOT), io.keyFieldSymbols());
                }
                io.assignTarget().filter(value -> !value.isBlank())
                        .ifPresent(value -> assignTargetByRepositoryName.putIfAbsent(
                                name.toUpperCase(java.util.Locale.ROOT), value.toUpperCase(java.util.Locale.ROOT)));

                DomainNode existing = boundaryNodes.get(id);
                if (existing == null) {
                    boundaryNodes.put(id, new DomainNode(id, kind, name, List.of(newEvidence),
                            Origin.DETERMINISTIC, persistentResource ? 0.8 : 0.5));
                } else {
                    Kind effectiveKind = (existing.kind() == Kind.REPOSITORY || kind == Kind.REPOSITORY)
                            ? Kind.REPOSITORY : Kind.EXTERNAL_SYSTEM;
                    double confidence = Math.max(existing.confidence(), persistentResource ? 0.8 : 0.5);
                    List<Evidence> evidenceList = new ArrayList<>(existing.evidence());
                    if (evidenceList.stream().noneMatch(e -> e.rationale().equalsIgnoreCase(io.operation()))) {
                        evidenceList.add(newEvidence);
                    }
                    boundaryNodes.put(id, new DomainNode(id, effectiveKind, existing.name(), evidenceList,
                            Origin.DETERMINISTIC, confidence));
                }
            }

            // In plain COBOL file I/O, the FD/SELECT (the repository) and
            // its 01-level record are the same physical resource described
            // twice — the file is just the record's persisted form. Showing
            // them as two boxes joined by a "maps to" line (which the
            // MAPS_TO inference below still does for DATABASE access)
            // misrepresents that as two related-but-distinct entities.
            // Reported as a bug: "record y file... en cobol se trata de la
            // misma entidad". For a FILE-kind repository with exactly one
            // unambiguous name-matching ENTITY, collapse them into a single
            // node instead — the repository's id/kind, the record's fields.
            // An ambiguous match (more than one candidate, or the candidate
            // already claimed by another repository) is left unmerged
            // rather than guessed at.
            java.util.Set<String> absorbedTypeIds = new java.util.HashSet<>();
            Map<String, DomainNode> mergedBoundaryNodes = new LinkedHashMap<>();
            for (Map.Entry<String, DomainNode> entry : boundaryNodes.entrySet()) {
                DomainNode boundaryNode = entry.getValue();
                if (boundaryNode.kind() != Kind.REPOSITORY || !Boolean.TRUE.equals(boundaryIsFileKind.get(entry.getKey()))) {
                    continue;
                }
                // A structural binding (the FD's actual RECORD IS clause,
                // parsed by the COBOL provider) is unambiguous by
                // definition — an exact name match on it always wins over
                // guessing, and doesn't need the "exactly one candidate"
                // caution the fuzzy fallback below requires.
                // A structural match also accepts VALUE_OBJECT (unlike the
                // fuzzy fallback, which stays ENTITY-only) — an unstructured
                // record with no sub-fields of its own (e.g. "01 VBR-REC
                // PIC X(80)." — the whole record is one elementary item)
                // projects as VALUE_OBJECT, not ENTITY, but the exact-name
                // structural binding still proves it's the same resource.
                String structuralRecord = boundaryStructuralRecordById.get(boundaryNode.id());
                List<DomainNode> candidates = structuralRecord != null
                        ? typeNodes.values().stream()
                                .filter(typeNode -> (typeNode.kind() == Kind.ENTITY || typeNode.kind() == Kind.VALUE_OBJECT)
                                        && !absorbedTypeIds.contains(typeNode.id()))
                                .filter(typeNode -> typeNode.name().equalsIgnoreCase(structuralRecord))
                                .toList()
                        : typeNodes.values().stream()
                                .filter(typeNode -> typeNode.kind() == Kind.ENTITY && !absorbedTypeIds.contains(typeNode.id()))
                                .filter(typeNode -> namesLikelyRelated(boundaryNode.name(), typeNode.name()))
                                .toList();
                if (candidates.size() != 1) continue;
                DomainNode matchedRecord = candidates.get(0);
                absorbedTypeIds.add(matchedRecord.id());
                List<Evidence> mergedEvidence = new ArrayList<>(boundaryNode.evidence());
                for (Evidence recordEvidence : matchedRecord.evidence()) {
                    if (mergedEvidence.stream().noneMatch(e -> e.rationale().equalsIgnoreCase(recordEvidence.rationale()))) {
                        mergedEvidence.add(recordEvidence);
                    }
                }
                mergedBoundaryNodes.put(boundaryNode.id(), new DomainNode(boundaryNode.id(), Kind.REPOSITORY,
                        boundaryNode.name(), matchedRecord.properties(), mergedEvidence, Origin.DETERMINISTIC,
                        Math.max(boundaryNode.confidence(), matchedRecord.confidence()), null, null));
            }

            for (DomainNode typeNode : typeNodes.values()) {
                if (absorbedTypeIds.contains(typeNode.id())) continue;
                nodes.add(typeNode);
                relations.add(new DomainRelation("relation:" + aggregateId + ":" + typeNode.id(),
                        aggregateId, typeNode.id(), RelationKind.CONTAINS));
            }
            for (DomainNode boundaryNode : boundaryNodes.values()) {
                DomainNode toEmit = mergedBoundaryNodes.getOrDefault(boundaryNode.id(), boundaryNode);
                nodes.add(toEmit);
                relations.add(new DomainRelation("relation:" + useCaseId + ":" + toEmit.id(),
                        useCaseId, toEmit.id(), RelationKind.USES));
            }

            // The domain model previously never linked a REPOSITORY to the
            // record shape it actually reads/writes — every diagram
            // (native VS Code + Workbench) expects a MAPS_TO relation for
            // exactly this (see mergeRepositories/mappedNodes downstream),
            // but nothing ever produced one, so entity diagrams never had
            // any cross-entity line to draw. The semantic IR doesn't carry
            // a direct link from an IoOperation to the SemanticType it
            // reads into, so this infers it the same way a COBOL reader
            // would: a repository's resource name (the FD/file name) and a
            // record's symbol (the 01-level group name) usually share a
            // common root once naming noise like FILE/RECORD/IN/OUT is
            // stripped — e.g. CUSTOMER-FILE reads into CUSTOMER-RECORD.
            // Conservative on purpose: only ENTITY-kind (group) types are
            // candidates, and only a genuine name-root match qualifies —
            // no match, no relation. Skips anything already merged above.
            for (DomainNode boundaryNode : boundaryNodes.values()) {
                if (boundaryNode.kind() != Kind.REPOSITORY || mergedBoundaryNodes.containsKey(boundaryNode.id())) continue;
                for (DomainNode typeNode : typeNodes.values()) {
                    if (typeNode.kind() != Kind.ENTITY || absorbedTypeIds.contains(typeNode.id())) continue;
                    if (!namesLikelyRelated(boundaryNode.name(), typeNode.name())) continue;
                    relations.add(new DomainRelation("relation:" + boundaryNode.id() + ":" + typeNode.id(),
                            boundaryNode.id(), typeNode.id(), RelationKind.MAPS_TO));
                }
            }

            Map<String, DomainNode> unclassifiedNodes = new LinkedHashMap<>();
            for (SemanticProgram.UnclassifiedDataAccess access : program.unclassifiedDataAccesses()) {
                String subject = access.subject() == null ? "" : access.subject().trim();
                String subjectKey = subject.replaceAll("[^A-Za-z0-9_-]", "_");
                String id = "unresolved:" + program.programId() + ":" + subjectKey;
                Evidence accessEvidence = evidence(program, access.reason());
                DomainNode existing = unclassifiedNodes.get(id);
                if (existing == null) {
                    unclassifiedNodes.put(id, new DomainNode(id, Kind.EXTERNAL_SYSTEM, subject,
                            List.of(accessEvidence), Origin.DETERMINISTIC, 0.3));
                } else {
                    List<Evidence> evidenceList = new ArrayList<>(existing.evidence());
                    if (evidenceList.stream().noneMatch(e -> e.rationale().equalsIgnoreCase(access.reason()))) {
                        evidenceList.add(accessEvidence);
                    }
                    unclassifiedNodes.put(id, new DomainNode(id, Kind.EXTERNAL_SYSTEM, existing.name(),
                            evidenceList, Origin.DETERMINISTIC, existing.confidence()));
                }
            }
            for (DomainNode unresolvedNode : unclassifiedNodes.values()) {
                nodes.add(unresolvedNode);
                relations.add(new DomainRelation("relation:" + useCaseId + ":" + unresolvedNode.id(),
                        useCaseId, unresolvedNode.id(), RelationKind.USES));
            }
        }
        mergeCrossProgramRepositories(nodes, relations, boundaryStructuralRecordById, assignTargetByRepositoryName);
        java.util.Set<String> inferredForeignKeyPairs = new java.util.HashSet<>();
        inferForeignKeyRelations(nodes, relations, keyFieldsByRepositoryName, inferredForeignKeyPairs);
        inferForeignKeyRelationsFromDataFlow(nodes, relations, allFieldFlows, fieldTypeIdToRepositoryName,
                typeIdToFieldName, keyFieldsByRepositoryName, inferredForeignKeyPairs);
        return new DomainModel(DomainModel.SCHEMA_VERSION, projectId, nodes, relations, List.of());
    }

    // A type id's own repository membership plus every field nested under
    // it (GROUP memberIds walked recursively) — a repository's key isn't
    // always declared directly on its top-level record, and a MOVE source
    // can equally be a deeply-nested field.
    private java.util.Set<String> descendantTypeIds(Map<String, SemanticProgram.SemanticType> typeById, String rootId) {
        java.util.Set<String> result = new java.util.LinkedHashSet<>();
        java.util.Deque<String> stack = new java.util.ArrayDeque<>();
        stack.push(rootId);
        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (!result.add(current)) continue;
            SemanticProgram.SemanticType type = typeById.get(current);
            if (type != null) type.memberIds().forEach(stack::push);
        }
        return result;
    }

    /** Conservative bridge for serialized provider analysis maps. */
    public DomainModel projectSerialized(String projectId, Map<String, Object> analysis) {
        List<DomainNode> nodes = new ArrayList<>();
        Object raw = analysis == null ? null : analysis.get("programs");
        if (raw instanceof List<?> programs) {
            for (Object value : programs) {
                if (!(value instanceof Map<?, ?> program)) continue;
                Object idValue = program.get("programId");
                if (!(idValue instanceof String id) || id.isBlank()) continue;
                String source = String.valueOf(program.containsKey("sourcePath") ? program.get("sourcePath") : "analysis:" + id);
                String hash = String.valueOf(program.containsKey("contentSha256") ? program.get("contentSha256") : "serialized-analysis");
                Evidence evidence = new Evidence(source, hash, "serialized program identity");
                nodes.add(new DomainNode("aggregate:" + id, Kind.AGGREGATE, id, List.of(evidence), Origin.DETERMINISTIC, 0.6));
                nodes.add(new DomainNode("use-case:" + id, Kind.USE_CASE, "Process " + id, List.of(evidence), Origin.DETERMINISTIC, 0.5));
            }
        }
        return new DomainModel(DomainModel.SCHEMA_VERSION, projectId, nodes, List.of(), List.of());
    }

    // Naming noise stripped before comparing a repository's resource name
    // against a record's symbol — see the MAPS_TO inference above.
    private static final java.util.Set<String> NAME_NOISE_TOKENS = java.util.Set.of(
            "FILE", "RECORD", "REC", "TABLE", "DATA", "IN", "OUT", "INPUT", "OUTPUT", "DB", "DB2");

    private String coreName(String raw) {
        if (raw == null) return "";
        String normalized = raw.strip().toUpperCase().replaceAll("[^A-Z0-9]+", " ").trim();
        if (normalized.isEmpty()) return "";
        StringBuilder core = new StringBuilder();
        for (String token : normalized.split(" ")) {
            if (!NAME_NOISE_TOKENS.contains(token)) core.append(token);
        }
        return core.length() >= 3 ? core.toString() : normalized.replace(" ", "");
    }

    // A short core name (e.g. "OUT" from OUT-FILE, "VBRC" from VBRC-FILE)
    // is a substring of almost anything sharing that prefix — VBRC-FILE
    // was matching VBRC-REC1, VBRC-REC2 and VBRCFILE-STATUS this way, none
    // of which are its actual record (reported: "arquitectura tengo file
    // y record" — a repository fanning out to unrelated status/array
    // variables via MAPS_TO, not a real duplicate pair). Exact matches are
    // always trusted regardless of length; substring containment only
    // kicks in once both sides are specific enough that a coincidental
    // prefix match is unlikely.
    private static final int MIN_CORE_LENGTH_FOR_SUBSTRING_MATCH = 5;

    private boolean namesLikelyRelated(String repositoryName, String entityName) {
        String a = coreName(repositoryName);
        String b = coreName(entityName);
        if (a.isEmpty() || b.isEmpty()) return false;
        if (a.equals(b)) return true;
        if (a.length() < MIN_CORE_LENGTH_FOR_SUBSTRING_MATCH || b.length() < MIN_CORE_LENGTH_FOR_SUBSTRING_MATCH) {
            return false;
        }
        return a.contains(b) || b.contains(a);
    }

    // A GROUP type's memberIds are its child fields (other TYPE nodes,
    // themselves possibly nested GROUPs for a COBOL 01-level with 05/10
    // sub-groups). Only the group's *direct* members become properties of
    // this record — a nested group's own members show up as that nested
    // group's own properties when it's projected as its own type, not
    // flattened in here, so this stays a one-level walk.
    private List<Property> groupProperties(SemanticProgram program, SemanticProgram.SemanticType group,
                                            Map<String, SemanticProgram.SemanticType> typeById) {
        List<Property> properties = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String memberId : group.memberIds()) {
            SemanticProgram.SemanticType member = typeById.get(memberId);
            if (member == null || member.symbol() == null || member.symbol().isBlank()
                    || "FILLER".equalsIgnoreCase(member.symbol().trim())) {
                continue;
            }
            String name = member.symbol().trim();
            if (!seen.add(name.toUpperCase())) continue;
            properties.add(new Property(name, propertyType(member), false,
                    List.of(evidence(program, name))));
        }
        return properties;
    }

    private String propertyType(SemanticProgram.SemanticType type) {
        return switch (type.typeKind()) {
            case GROUP -> "group";
            case TEXT -> "string";
            case INTEGER -> "integer";
            case DECIMAL -> "decimal";
            case BOOLEAN -> "boolean";
            case UNKNOWN -> "unknown";
        };
    }

    private List<Property> mergePropertiesByName(List<Property> existing, List<Property> additional) {
        if (additional.isEmpty()) return existing;
        Map<String, Property> byName = new LinkedHashMap<>();
        for (Property property : existing) byName.put(property.name().toUpperCase(), property);
        for (Property property : additional) byName.putIfAbsent(property.name().toUpperCase(), property);
        return List.copyOf(byName.values());
    }

    // The per-program merge above only ever looked within one program's own
    // boundary/type nodes — but a real COBOL file (an FD/SELECT) is a
    // shared, program-independent resource: the same "customer file" read
    // by one program and a record named "CUSTOMER-RECORD" that's really the
    // same file's I/O boundary in another program end up as two separate
    // `boundary:<program>:<name>` nodes purely because the id embeds the
    // program id. Reported as still-broken after the per-program fix
    // (#280 follow-up: "sigo viendo customer file y customer record como 2
    // entidades" — confirmed via a live analysis run that they're two
    // REPOSITORY nodes from different programs, not a file+GROUP-type pair
    // within one program). This second, global pass runs once after every
    // program has been projected and collapses same-core-name REPOSITORY
    // nodes across the whole model, redirecting every relation that named
    // an absorbed id to the surviving one.
    private void mergeCrossProgramRepositories(List<DomainNode> nodes, List<DomainRelation> relations,
            Map<String, String> boundaryStructuralRecordById, Map<String, String> assignTargetByRepositoryName) {
        List<DomainNode> repositories = nodes.stream().filter(node -> node.kind() == Kind.REPOSITORY).toList();

        // Union-find over repository ids: two edges feed it. (1) Fuzzy —
        // same core name after stripping FILE/RECORD/IN/OUT noise words,
        // the fallback for when nothing more precise is available. (2)
        // Structural — a repository's own name exactly matches another
        // repository's parsed FD-to-record binding (e.g. ACCTFILE-FILE is
        // structurally bound to ACCOUNT-RECORD, and some other program
        // separately has a boundary node literally named ACCOUNT-RECORD);
        // this catches real pairs the fuzzy pass misses whenever COBOL
        // naming conventions diverge between a file and its record (FILE
        // abbreviated into the name itself, ACCT vs ACCOUNT, TRAN vs
        // TRANSACT, ...) — reported as still-broken after the fuzzy-only
        // pass: "fijate que ahora quedaron casos que no son customer...
        // asociar siempre el record al file".
        Map<String, String> parent = new LinkedHashMap<>();
        for (DomainNode node : repositories) parent.put(node.id(), node.id());

        Map<String, List<DomainNode>> byCoreName = new LinkedHashMap<>();
        for (DomainNode node : repositories) {
            String core = coreName(node.name());
            if (!core.isEmpty()) byCoreName.computeIfAbsent(core, key -> new ArrayList<>()).add(node);
        }
        for (List<DomainNode> group : byCoreName.values()) {
            for (int i = 1; i < group.size(); i++) union(parent, group.get(0).id(), group.get(i).id());
        }

        Map<String, String> repositoryIdByUpperName = new LinkedHashMap<>();
        for (DomainNode node : repositories) repositoryIdByUpperName.putIfAbsent(node.name().trim().toUpperCase(java.util.Locale.ROOT), node.id());
        for (DomainNode node : repositories) {
            String structuralRecord = boundaryStructuralRecordById.get(node.id());
            if (structuralRecord == null || structuralRecord.isBlank()) continue;
            String targetId = repositoryIdByUpperName.get(structuralRecord.trim().toUpperCase(java.util.Locale.ROOT));
            if (targetId != null && !targetId.equals(node.id())) union(parent, node.id(), targetId);
        }

        // Physical dataset — the strongest signal of all: two repositories
        // ASSIGN TO the exact same dataset name are, by definition, the
        // same physical file, whatever their local SELECT names happen to
        // look like (ACCT-FILE / ACCOUNT-FILE / ACCTFILE-FILE all ASSIGN
        // TO ACCTFILE). Confirmed via CardDemo: these names are too
        // dissimilar for the fuzzy core-name pass above, and this gap was
        // exactly what let a later same-key-field coincidence get
        // mistaken by inferForeignKeyRelations for a real foreign key
        // between two supposedly different entities.
        Map<String, List<DomainNode>> byAssignTarget = new LinkedHashMap<>();
        for (DomainNode node : repositories) {
            String assignTarget = assignTargetByRepositoryName.get(node.name().trim().toUpperCase(java.util.Locale.ROOT));
            if (assignTarget != null && !assignTarget.isBlank()) {
                byAssignTarget.computeIfAbsent(assignTarget, key -> new ArrayList<>()).add(node);
            }
        }
        for (List<DomainNode> group : byAssignTarget.values()) {
            for (int i = 1; i < group.size(); i++) union(parent, group.get(0).id(), group.get(i).id());
        }

        Map<String, List<DomainNode>> groups = new LinkedHashMap<>();
        for (DomainNode node : repositories) groups.computeIfAbsent(find(parent, node.id()), key -> new ArrayList<>()).add(node);

        Map<String, String> idRemap = new LinkedHashMap<>();
        Map<String, DomainNode> replacementById = new LinkedHashMap<>();
        for (List<DomainNode> group : groups.values()) {
            if (group.size() < 2) continue;
            DomainNode base = group.stream()
                    .max(java.util.Comparator.comparingInt((DomainNode candidate) -> candidate.properties().size())
                            .thenComparingDouble(DomainNode::confidence))
                    .orElse(group.get(0));
            List<Property> mergedProperties = base.properties();
            List<Evidence> mergedEvidence = new ArrayList<>(base.evidence());
            double maxConfidence = base.confidence();
            for (DomainNode member : group) {
                if (member == base) continue;
                mergedProperties = mergePropertiesByName(mergedProperties, member.properties());
                for (Evidence memberEvidence : member.evidence()) {
                    if (mergedEvidence.stream().noneMatch(e -> e.rationale().equalsIgnoreCase(memberEvidence.rationale()))) {
                        mergedEvidence.add(memberEvidence);
                    }
                }
                maxConfidence = Math.max(maxConfidence, member.confidence());
                idRemap.put(member.id(), base.id());
            }
            replacementById.put(base.id(), new DomainNode(base.id(), Kind.REPOSITORY, base.name(),
                    mergedProperties, mergedEvidence, Origin.DETERMINISTIC, maxConfidence, null, null));
        }
        if (idRemap.isEmpty()) return;

        nodes.removeIf(node -> idRemap.containsKey(node.id()));
        for (int i = 0; i < nodes.size(); i++) {
            DomainNode replacement = replacementById.get(nodes.get(i).id());
            if (replacement != null) nodes.set(i, replacement);
        }

        java.util.Set<String> seenRelations = new java.util.HashSet<>();
        java.util.ListIterator<DomainRelation> relationIterator = relations.listIterator();
        while (relationIterator.hasNext()) {
            DomainRelation relation = relationIterator.next();
            String from = idRemap.getOrDefault(relation.fromId(), relation.fromId());
            String to = idRemap.getOrDefault(relation.toId(), relation.toId());
            if (from.equals(to)) {
                relationIterator.remove();
                continue;
            }
            String dedupeKey = from + "->" + to + ":" + relation.kind();
            if (!seenRelations.add(dedupeKey)) {
                relationIterator.remove();
                continue;
            }
            if (!from.equals(relation.fromId()) || !to.equals(relation.toId())) {
                relationIterator.set(new DomainRelation("relation:" + from + ":" + to + ":" + relation.kind(),
                        from, to, relation.kind(), relation.sourceCardinality(), relation.targetCardinality(),
                        relation.foreignKey()));
            }
        }
    }

    // Real relational structure between *different* business entities —
    // Account references Customer, a card-cross-reference record
    // references both Account and Card — was simply never inferred at
    // all: the only relation this projector ever produced between two
    // repositories was MAPS_TO from a file to its own record, the same
    // physical resource. A repository's RECORD KEY / ALTERNATE RECORD KEY
    // (see keyFieldsByRepositoryName, collected from FILE-CONTROL) is its
    // real identity key; if some *other* repository carries a property
    // with that exact name, that other repository holds a foreign key
    // into this one — the standard way to reverse-engineer relational
    // structure out of VSAM/indexed COBOL files. Runs last, once every
    // repository's final name and properties are settled by the merges
    // above.
    private void inferForeignKeyRelations(List<DomainNode> nodes, List<DomainRelation> relations,
            Map<String, List<String>> keyFieldsByRepositoryName, java.util.Set<String> seenPairs) {
        if (keyFieldsByRepositoryName.isEmpty()) return;
        Map<String, String> repositoryIdByName = new LinkedHashMap<>();
        for (DomainNode node : nodes) {
            if (node.kind() == Kind.REPOSITORY) repositoryIdByName.putIfAbsent(node.name().toUpperCase(java.util.Locale.ROOT), node.id());
        }
        // key field name -> the repository whose own key it is
        Map<String, String> repositoryNameByKeyField = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : keyFieldsByRepositoryName.entrySet()) {
            for (String key : entry.getValue()) {
                repositoryNameByKeyField.putIfAbsent(key.toUpperCase(java.util.Locale.ROOT), entry.getKey());
            }
        }

        for (DomainNode node : nodes) {
            if (node.kind() != Kind.REPOSITORY) continue;
            // A repository's own declared key fields (it may carry more
            // than one surviving local name after the cross-program merge,
            // since merge keeps only the "base" node's name) — if the
            // matching property is ALSO one of THIS repository's own keys,
            // it isn't a foreign key at all: it's the same physical file
            // being seen under two different local SELECT names that the
            // merge above failed to unify (a same-key collision), not a
            // reference to a genuinely different entity.
            java.util.Set<String> ownKeyFields = keyFieldsByRepositoryName
                    .getOrDefault(node.name().toUpperCase(java.util.Locale.ROOT), List.of()).stream()
                    .map(key -> key.toUpperCase(java.util.Locale.ROOT))
                    .collect(java.util.stream.Collectors.toSet());
            for (Property property : node.properties()) {
                String propertyName = property.name().toUpperCase(java.util.Locale.ROOT);
                if (ownKeyFields.contains(propertyName)) continue;
                String ownerName = repositoryNameByKeyField.get(propertyName);
                if (ownerName == null || ownerName.equals(node.name().toUpperCase(java.util.Locale.ROOT))) continue;
                String targetId = repositoryIdByName.get(ownerName);
                if (targetId == null || targetId.equals(node.id())) continue;
                String key = node.id() + "->" + targetId;
                if (!seenPairs.add(key)) continue;
                relations.add(new DomainRelation("relation:fk:" + node.id() + ":" + targetId,
                        node.id(), targetId, RelationKind.ASSOCIATES_WITH,
                        Cardinality.ZERO_OR_MORE, Cardinality.ONE));
            }
        }
    }

    // The other half of relation inference: not "this repository happens
    // to have a field with the same name as another's key" (a static,
    // name-based coincidence), but "the program itself moves a value out
    // of one entity's fields into a field that's another repository's own
    // declared key" — the actual join a COBOL procedure performs, traced
    // through fieldFlows (every MOVE the CobolSemanticProjector captured
    // with both endpoints resolved to real data items). Requested
    // explicitly: "podemos acceder al corte del control... por el proceso
    // que hace" — this is exactly that: procedure-division data flow, not
    // a name heuristic, and it needs no hardcoded field/file names to work
    // for programs never seen before.
    private void inferForeignKeyRelationsFromDataFlow(List<DomainNode> nodes, List<DomainRelation> relations,
            List<SemanticProgram.FieldFlow> fieldFlows, Map<String, String> fieldTypeIdToRepositoryName,
            Map<String, String> typeIdToFieldName, Map<String, List<String>> keyFieldsByRepositoryName,
            java.util.Set<String> seenPairs) {
        if (fieldFlows.isEmpty() || fieldTypeIdToRepositoryName.isEmpty()) return;
        Map<String, String> repositoryIdByName = new LinkedHashMap<>();
        for (DomainNode node : nodes) {
            if (node.kind() == Kind.REPOSITORY) repositoryIdByName.putIfAbsent(node.name().toUpperCase(java.util.Locale.ROOT), node.id());
        }
        Map<String, String> repositoryNameByKeyField = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : keyFieldsByRepositoryName.entrySet()) {
            for (String key : entry.getValue()) {
                repositoryNameByKeyField.putIfAbsent(key.toUpperCase(java.util.Locale.ROOT), entry.getKey());
            }
        }

        for (SemanticProgram.FieldFlow flow : fieldFlows) {
            String sourceRepositoryName = fieldTypeIdToRepositoryName.get(flow.sourceTypeId());
            if (sourceRepositoryName == null) continue;
            String targetFieldName = typeIdToFieldName.get(flow.targetTypeId());
            if (targetFieldName == null) continue;
            String targetRepositoryName = repositoryNameByKeyField.get(targetFieldName);
            if (targetRepositoryName == null || targetRepositoryName.equals(sourceRepositoryName)) continue;
            String fromId = repositoryIdByName.get(sourceRepositoryName);
            String toId = repositoryIdByName.get(targetRepositoryName);
            if (fromId == null || toId == null || fromId.equals(toId)) continue;
            String key = fromId + "->" + toId;
            if (!seenPairs.add(key)) continue;
            relations.add(new DomainRelation("relation:fk-flow:" + fromId + ":" + toId, fromId, toId,
                    RelationKind.ASSOCIATES_WITH, Cardinality.ZERO_OR_MORE, Cardinality.ONE));
        }
    }

    private String find(Map<String, String> parent, String id) {
        String root = id;
        while (!parent.get(root).equals(root)) root = parent.get(root);
        String current = id;
        while (!current.equals(root)) {
            String next = parent.get(current);
            parent.put(current, root);
            current = next;
        }
        return root;
    }

    private void union(Map<String, String> parent, String a, String b) {
        String rootA = find(parent, a);
        String rootB = find(parent, b);
        if (!rootA.equals(rootB)) parent.put(rootA, rootB);
    }

    private boolean isPhysicalResourceName(String value, String operation) {
        if (value == null || value.isBlank()) return false;
        return operation == null || !value.strip().equalsIgnoreCase(operation.strip());
    }

    // A COBOL SELECT/FD name ending in -INPUT/-OUTPUT/-IN/-OUT (optionally
    // followed by -RECORD) reads as a one-way batch interchange file — an
    // extract, an import feed, an error report — not a persisted data
    // store. The semantic IR doesn't yet carry FILE-CONTROL ORGANIZATION
    // (INDEXED/RELATIVE vs SEQUENTIAL) to tell a keyed store from a plain
    // stream directly, so this name-based heuristic is a stop-gap: it kept
    // misclassifying files like CARD-OUTPUT or CUSTOMER-INPUT as
    // persistence "Repository" nodes purely because they were FILE-kind
    // I/O with a non-blank resource name (see isPhysicalResourceName,
    // which only rules out a blank/verb-echoing name — no organization
    // signal at all). A name like CUSTOMER-FILE or ACCTFILE-FILE still
    // passes through as a REPOSITORY candidate, which is the right default
    // absent stronger evidence either way.
    private static final Pattern ONE_WAY_INTERCHANGE_FILE_NAME =
            Pattern.compile("(?i).*-(INPUT|OUTPUT|IN|OUT)(-RECORD)?$");

    private boolean looksLikeOneWayInterchangeFile(String resource) {
        if (resource == null) return false;
        return ONE_WAY_INTERCHANGE_FILE_NAME.matcher(resource.strip()).matches();
    }

    private Evidence evidence(SemanticProgram program, String rationale) {
        return new Evidence(program.sourceProvenance().sourcePath(),
                program.sourceProvenance().contentSha256(), rationale);
    }
}
