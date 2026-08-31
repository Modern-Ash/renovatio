package org.shark.renovatio.provider.cobol.polish;

import java.util.Set;

public record StrategyExtraction(
        String conditionalRegion,
        Set<String> branches,
        boolean exhaustive,
        boolean behaviorPreserved) implements PolishFamilyPayload {

    public StrategyExtraction {
        conditionalRegion = PolishContracts.nonBlank(conditionalRegion, "conditionalRegion");
        branches = PolishContracts.nonBlankSet(branches, "branches");
        if (branches.size() < 2) {
            throw new IllegalArgumentException("Strategy extraction requires at least two branches");
        }
        if (!exhaustive) {
            throw new IllegalArgumentException("Strategy branches must be exhaustive");
        }
        if (!behaviorPreserved) {
            throw new IllegalArgumentException("Strategy extraction must preserve behavior");
        }
    }

    @Override
    public PolishProposalFamily family() {
        return PolishProposalFamily.STRATEGY_EXTRACTION;
    }
}
