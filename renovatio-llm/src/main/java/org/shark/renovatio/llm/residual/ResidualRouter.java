package org.shark.renovatio.llm.residual;

import java.util.ArrayList;
import java.util.List;

/** Pure, closed router. Ambiguous inputs fail closed to the deterministic lane. */
public final class ResidualRouter {

    public ResidualRoute route(ResidualEnrichmentRequest request) {
        List<ResidualRoute> matches = new ArrayList<>(2);
        switch (request.construction()) {
            case PARAGRAPH, DATA_ITEM -> add(matches, request.explicitDomainNamingRequest(),
                    ResidualRoute.DOMAIN_NAMING);
            case CONTROL_FLOW_COMPONENT -> add(matches,
                    request.irreducibleControlFlow() && request.containsGoTo(),
                    ResidualRoute.CONTROL_FLOW_PLAN);
            case REDEFINES -> add(matches, request.residualBusinessIntent(),
                    ResidualRoute.REDEFINES_INTENT);
            case OCCURS_DEPENDING_ON -> add(matches, request.residualBusinessIntent(),
                    ResidualRoute.OCCURS_DEPENDING_ON_INTENT);
            case UNSUPPORTED -> add(matches, request.unsupportedDiagnostic() != null,
                    ResidualRoute.UNSUPPORTED_EXPLANATION);
            default -> {
                // Supported semantic constructs remain deterministic.
            }
        }
        return matches.size() == 1 ? matches.get(0) : ResidualRoute.DETERMINISTIC;
    }

    private static void add(List<ResidualRoute> matches, boolean condition, ResidualRoute route) {
        if (condition) {
            matches.add(route);
        }
    }
}
