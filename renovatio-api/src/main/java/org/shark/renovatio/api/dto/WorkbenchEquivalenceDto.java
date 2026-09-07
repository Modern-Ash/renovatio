package org.shark.renovatio.api.dto;

import java.util.List;

/** Read-only inventory of persisted material relevant to an equivalence review. */
public record WorkbenchEquivalenceDto(List<Item> evidence, List<Item> generatedTargets, List<Verdict> verdicts) {
    public record Item(String id, String name) { }
    public record Verdict(String fixtureId, String classification, String reason, boolean blocksRelease) { }
}
