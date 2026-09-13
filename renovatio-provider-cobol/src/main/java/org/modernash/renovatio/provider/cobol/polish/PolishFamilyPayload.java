package org.modernash.renovatio.provider.cobol.polish;

public sealed interface PolishFamilyPayload permits DomainNamingRefinement, PortExtraction,
        StrategyExtraction, FlagCollapse {

    PolishProposalFamily family();
}
