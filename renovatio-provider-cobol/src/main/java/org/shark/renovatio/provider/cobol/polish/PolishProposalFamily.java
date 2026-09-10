package org.shark.renovatio.provider.cobol.polish;

public enum PolishProposalFamily {
    DOMAIN_NAMING_REFINEMENT("cobol.polish.domain-naming.v1"),
    PORT_EXTRACTION("cobol.polish.port-extraction.v1"),
    STRATEGY_EXTRACTION("cobol.polish.strategy-extraction.v1"),
    FLAG_COLLAPSE("cobol.polish.flag-collapse.v1");

    private final String promptId;

    PolishProposalFamily(String promptId) {
        this.promptId = promptId;
    }

    public String promptId() {
        return promptId;
    }
}
