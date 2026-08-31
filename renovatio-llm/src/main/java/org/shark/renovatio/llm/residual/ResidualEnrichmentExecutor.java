package org.shark.renovatio.llm.residual;

import com.fasterxml.jackson.databind.JsonNode;

/** Adapter seam for the governed catalog/cache/provider runtime. */
@FunctionalInterface
public interface ResidualEnrichmentExecutor {
    JsonNode enrich(ResidualRoute route, ResidualEnrichmentRequest request);
}
