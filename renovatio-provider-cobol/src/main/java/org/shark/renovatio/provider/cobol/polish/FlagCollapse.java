package org.shark.renovatio.provider.cobol.polish;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record FlagCollapse(
        Set<String> fields,
        Set<String> observedCombinations,
        Map<String, String> stateMapping,
        boolean mutuallyExclusive,
        boolean exhaustive) implements PolishFamilyPayload {

    public FlagCollapse {
        fields = PolishContracts.nonBlankSet(fields, "fields");
        observedCombinations = PolishContracts.nonBlankSet(observedCombinations, "observedCombinations");
        stateMapping = PolishContracts.stringMap(stateMapping, "stateMapping");
        if (!mutuallyExclusive) {
            throw new IllegalArgumentException("Flags must be mutually exclusive");
        }
        if (!exhaustive) {
            throw new IllegalArgumentException("Flag combinations must be exhaustive");
        }
        if (!stateMapping.keySet().equals(observedCombinations)
                || new HashSet<>(stateMapping.values()).size() != stateMapping.size()) {
            throw new IllegalArgumentException("Flag combinations require a one-to-one state mapping");
        }
    }

    @Override
    public PolishProposalFamily family() {
        return PolishProposalFamily.FLAG_COLLAPSE;
    }
}
