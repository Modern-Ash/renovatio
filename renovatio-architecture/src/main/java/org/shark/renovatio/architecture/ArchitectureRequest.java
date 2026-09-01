package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.semantic.ir.SemanticProgram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Complete deterministic input to one project architecture transformation. */
public record ArchitectureRequest(String schemaVersion, List<SemanticProgram> programs,
                                  MigrationProfiles.EffectiveProfile effectiveProfile,
                                  GroupingConfiguration grouping,
                                  Map<String, List<String>> programCopybooks,
                                  List<String> acceptedEvidenceHashes,
                                  String requestHash) {
    public static final String SCHEMA_VERSION = "1";

    public ArchitectureRequest {
        if (!SCHEMA_VERSION.equals(schemaVersion)) throw new IllegalArgumentException("unsupported schemaVersion");
        Objects.requireNonNull(effectiveProfile, "effectiveProfile");
        grouping = grouping == null ? GroupingConfiguration.empty() : grouping;
        List<SemanticProgram> ordered = new ArrayList<>(Objects.requireNonNull(programs, "programs"));
        if (ordered.isEmpty()) throw new IllegalArgumentException("programs must not be empty");
        if (ordered.stream().anyMatch(Objects::isNull)) throw new NullPointerException("program");
        ordered.sort(Comparator.comparing((SemanticProgram value) -> value.sourceProvenance().sourcePath())
                .thenComparing(SemanticProgram::programId));
        if (new HashSet<>(ordered.stream().map(SemanticProgram::programId).toList()).size() != ordered.size()) {
            throw new IllegalArgumentException("duplicate programId");
        }
        if (new HashSet<>(ordered.stream().map(value -> value.sourceProvenance().sourcePath()).toList()).size()
                != ordered.size()) {
            throw new IllegalArgumentException("duplicate sourcePath");
        }
        programs = List.copyOf(ordered);
        programCopybooks = copybooks(programCopybooks, programs);
        acceptedEvidenceHashes = (acceptedEvidenceHashes == null ? List.<String>of() : acceptedEvidenceHashes)
                .stream().map(value -> ArchitectureSupport.hash(value, "evidenceHash")).distinct().sorted().toList();
        String computed = identity(programs, effectiveProfile, grouping, programCopybooks, acceptedEvidenceHashes);
        requestHash = requestHash == null ? computed : ArchitectureSupport.hash(requestHash, "requestHash");
        if (!computed.equals(requestHash)) throw new IllegalArgumentException("requestHash does not match request");
    }

    public static ArchitectureRequest create(List<SemanticProgram> programs,
                                             MigrationProfiles.EffectiveProfile effectiveProfile,
                                             GroupingConfiguration grouping,
                                             Map<String, List<String>> programCopybooks,
                                             List<String> acceptedEvidenceHashes) {
        return new ArchitectureRequest(SCHEMA_VERSION, programs, effectiveProfile, grouping,
                programCopybooks, acceptedEvidenceHashes, null);
    }

    private static Map<String, List<String>> copybooks(Map<String, List<String>> values,
                                                       List<SemanticProgram> programs) {
        TreeMap<String, List<String>> ordered = new TreeMap<>();
        if (values != null) values.forEach((key, raw) -> ordered.put(ArchitectureSupport.program(key),
                (raw == null ? List.<String>of() : raw).stream()
                        .map(value -> ArchitectureSupport.text(value, "copybook").toUpperCase(java.util.Locale.ROOT))
                        .distinct().sorted().toList()));
        SetView known = new SetView(programs.stream().map(SemanticProgram::programId).toList());
        if (ordered.keySet().stream().anyMatch(key -> !known.contains(key))) {
            throw new IllegalArgumentException("copybooks reference an unknown program");
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(ordered));
    }

    private static String identity(List<SemanticProgram> programs,
                                   MigrationProfiles.EffectiveProfile effective,
                                   GroupingConfiguration grouping,
                                   Map<String, List<String>> copybooks,
                                   List<String> evidence) {
        StringBuilder projection = new StringBuilder("architecture.v1\n")
                .append(effective.profileHash()).append('\n');
        programs.forEach(value -> projection.append(value.programId()).append('|')
                .append(value.sourceProvenance().sourcePath()).append('|')
                .append(value.sourceProvenance().contentSha256()).append('\n'));
        projection.append(grouping.singleModuleName()).append('\n');
        appendMap(projection, grouping.manualModules());
        appendMap(projection, grouping.domainCopybooks());
        appendMap(projection, grouping.prefixModules());
        copybooks.forEach((key, value) -> projection.append(key).append('=').append(String.join(",", value)).append('\n'));
        evidence.forEach(value -> projection.append("evidence=").append(value).append('\n'));
        return ArchitectureSupport.sha256(projection.toString());
    }

    private static void appendMap(StringBuilder target, Map<String, String> values) {
        values.forEach((key, value) -> target.append(key).append('=').append(value).append('\n'));
    }

    private record SetView(java.util.Set<String> values) {
        SetView(List<String> values) { this(new HashSet<>(values)); }
        boolean contains(String value) { return values.contains(value); }
    }
}
