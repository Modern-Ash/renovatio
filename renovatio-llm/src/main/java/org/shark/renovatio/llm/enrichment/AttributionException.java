package org.shark.renovatio.llm.enrichment;

public final class AttributionException extends RuntimeException {
    public enum Stage { INIT, FINALIZE }

    private final Stage stage;

    public AttributionException(Stage stage) {
        super(stage == Stage.INIT ? "LLM_ATTRIBUTION_INIT_FAILED" : "LLM_ATTRIBUTION_FINALIZE_FAILED");
        this.stage = stage;
    }

    public Stage stage() {
        return stage;
    }
}
