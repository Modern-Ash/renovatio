package org.shark.renovatio.llm.residual;

import java.util.ArrayList;
import java.util.List;

/** Pure, closed router. Ambiguous inputs fail closed to the deterministic lane. */
public final class ResidualRouter {

    public ResidualRoute route(ResidualEnrichmentRequest request) {
        if (hasIncompatibleSignals(request)) return ResidualRoute.DETERMINISTIC;
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
            case MOVE_CORRESPONDING -> add(matches, request.residualBusinessIntent(),
                    ResidualRoute.MOVE_CORRESPONDING_INTENT);
            case COMPUTE_OVERFLOW -> {
                // COMPUTE with implicit overflow is always deterministic (use overflow policy)
            }
            case UNSUPPORTED -> add(matches, request.unsupportedDiagnostic() != null,
                    ResidualRoute.UNSUPPORTED_EXPLANATION);
            default -> {
                // Supported semantic constructs remain deterministic.
            }
        }
        return matches.size() == 1 ? matches.get(0) : ResidualRoute.DETERMINISTIC;
    }

    private static boolean hasIncompatibleSignals(ResidualEnrichmentRequest request) {
        int families = 0;
        if (request.explicitDomainNamingRequest()) families++;
        if (request.irreducibleControlFlow() || request.containsGoTo()) families++;
        if (request.residualBusinessIntent()) families++;
        if (request.unsupportedDiagnostic() != null) families++;
        if (request.construction() == ResidualConstruction.COMPUTE_OVERFLOW) families++;
        return families > 1;
    }

    private static void add(List<ResidualRoute> matches, boolean condition, ResidualRoute route) {
        if (condition) {
            matches.add(route);
        }
    }
}
