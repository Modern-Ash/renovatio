package org.shark.renovatio.llm.residual;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/** Enforces the no-call deterministic boundary before the governed runtime seam. */
public final class ResidualEnrichmentCoordinator {
    private final ResidualRouter router;
    private final ResidualEnrichmentExecutor executor;

    public ResidualEnrichmentCoordinator(ResidualRouter router, ResidualEnrichmentExecutor executor) {
        this.router = Objects.requireNonNull(router);
        this.executor = Objects.requireNonNull(executor);
    }

    public ResidualEnrichmentOutcome enrich(ResidualEnrichmentRequest request,
                                            JsonNode deterministicResult) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(deterministicResult, "deterministicResult");
        ResidualRoute route = router.route(request);
        if (!route.isResidual()) {
            return ResidualEnrichmentOutcome.deterministic(deterministicResult);
        }
        JsonNode proposal = Objects.requireNonNull(executor.enrich(route, request), "proposal");
        return ResidualEnrichmentOutcome.enriched(route, deterministicResult, proposal);
    }
}
