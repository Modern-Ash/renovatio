package org.shark.renovatio.api.dto;

import java.util.List;
import java.util.Map;

/** Integrated Equivalence Lab inventory, execution history, divergences, gate and audit surface. */
public record WorkbenchEquivalenceDto(List<Item> evidence,
                                      List<Item> generatedTargets,
                                      List<Verdict> verdicts,
                                      List<Fixture> fixtures,
                                      List<Run> runs,
                                      Gate gate,
                                      List<AuditEvent> history) {
    public record Item(String id, String name) { }
    public record Verdict(String fixtureId, String classification, String reason, boolean blocksRelease) { }
    public record Fixture(String id,
                          String name,
                          String sourceId,
                          String baselineId,
                          String candidateId,
                          Inputs inputs,
                          List<String> evidenceRefs,
                          String reproducibilityHash) { }
    public record Inputs(Map<String, String> fields,
                         List<SequentialFile> sequentialFiles,
                         List<Db2Response> db2Responses) { }
    public record SequentialFile(String ddName, String contentHash, String preview) { }
    public record Db2Response(String statementId, String sqlState, List<Map<String, String>> rows) { }
    public record Run(String id,
                      String fixtureId,
                      String state,
                      int progress,
                      String startedAt,
                      String finishedAt,
                      String baselineHash,
                      String candidateHash,
                      String commit,
                      String profileHash,
                      String changeSetId,
                      List<String> logs,
                      Comparison comparison,
                      List<Divergence> divergences,
                      String reportHash) { }
    public record Comparison(String state,
                             boolean outputsMatch,
                             boolean filesMatch,
                             boolean sqlMatches,
                             List<String> comparedArtifacts) { }
    public record Divergence(String id,
                             String kind,
                             String severity,
                             String evidenceRef,
                             String summary,
                             String triageStatus,
                             String triageReason) { }
    public record Gate(boolean promotable,
                       String status,
                       List<String> blockers,
                       String readinessReason) { }
    public record AuditEvent(String actor, String action, String at, String targetId, String reason, String hash) { }
    public record RunRequest(String fixtureId, String baselineId, String candidateId, Map<String, String> inputOverrides,
                             String commit, String profileHash, String changeSetId) { }
    public record TriageRequest(String action, String reason) { }
}
