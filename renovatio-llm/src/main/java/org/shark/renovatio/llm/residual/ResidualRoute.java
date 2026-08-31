package org.shark.renovatio.llm.residual;

/** Closed residual families. DETERMINISTIC has deliberately no prompt. */
public enum ResidualRoute {
    DOMAIN_NAMING("cobol.domain.naming.v1"),
    CONTROL_FLOW_PLAN("cobol.goto.restructure.v1"),
    REDEFINES_INTENT("cobol.redefines.intent.v1"),
    OCCURS_DEPENDING_ON_INTENT("cobol.occurs-depending.intent.v1"),
    UNSUPPORTED_EXPLANATION("cobol.unsupported.explain.v1"),
    DETERMINISTIC(null);

    private final String promptId;

    ResidualRoute(String promptId) {
        this.promptId = promptId;
    }

    public String promptId() {
        if (promptId == null) {
            throw new IllegalStateException("The deterministic route has no prompt");
        }
        return promptId;
    }

    public boolean isResidual() {
        return this != DETERMINISTIC;
    }
}
