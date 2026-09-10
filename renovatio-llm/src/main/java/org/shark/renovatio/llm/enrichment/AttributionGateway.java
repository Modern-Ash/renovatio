package org.shark.renovatio.llm.enrichment;

/** Agora adapter seam. Production binds this to the enclosing llm-enrichment/enrich tool-run. */
public interface AttributionGateway {
    String begin(AttributionInput input);

    void complete(String runReference, AttributionResult result);
}
