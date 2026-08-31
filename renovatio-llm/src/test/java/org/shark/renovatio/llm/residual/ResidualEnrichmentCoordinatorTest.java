package org.shark.renovatio.llm.residual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResidualEnrichmentCoordinatorTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void supportedDeterministicConstructsNeverInvokeResidualRuntime() {
        AtomicInteger calls = new AtomicInteger();
        ResidualEnrichmentCoordinator coordinator = new ResidualEnrichmentCoordinator(
                new ResidualRouter(), (route, request) -> {
                    calls.incrementAndGet();
                    throw new AssertionError("deterministic construction reached the LLM runtime");
                });
        JsonNode deterministic = JSON.createObjectNode().put("preserved", true);

        for (ResidualConstruction construction : new ResidualConstruction[]{
                ResidualConstruction.MOVE, ResidualConstruction.COMPUTE, ResidualConstruction.IF,
                ResidualConstruction.EVALUATE, ResidualConstruction.SIMPLE_PERFORM,
                ResidualConstruction.BASIC_PIC, ResidualConstruction.LEVEL_88}) {
            ResidualEnrichmentOutcome outcome = coordinator.enrich(request(construction), deterministic);
            assertEquals(ResidualRoute.DETERMINISTIC, outcome.route());
            assertSame(deterministic, outcome.deterministicResult());
        }
        assertEquals(0, calls.get());
    }

    @Test
    void residualRoutesSelectOnlyTheirVersionedPrompt() {
        ResidualRouter router = new ResidualRouter();

        assertEquals("cobol.domain.naming.v1", router.route(domainRequest()).promptId());
        assertEquals("cobol.goto.restructure.v1", router.route(controlFlowRequest()).promptId());
        assertEquals("cobol.redefines.intent.v1",
                router.route(intentRequest(ResidualConstruction.REDEFINES)).promptId());
        assertEquals("cobol.occurs-depending.intent.v1",
                router.route(intentRequest(ResidualConstruction.OCCURS_DEPENDING_ON)).promptId());
        assertEquals("cobol.unsupported.explain.v1", router.route(unsupportedRequest()).promptId());
    }

    @Test
    void merelyEncounteringEligibleNodeDoesNotOptIn() {
        assertEquals(ResidualRoute.DETERMINISTIC, new ResidualRouter().route(request(ResidualConstruction.PARAGRAPH)));
    }

    @Test
    void reducibleOrGotoFreeControlFlowRemainsDeterministic() {
        assertEquals(ResidualRoute.DETERMINISTIC, new ResidualRouter().route(new ResidualEnrichmentRequest(
                "cobol-ir.v1", "node-1", "CONTROL_FLOW", ResidualConstruction.CONTROL_FLOW_COMPONENT,
                false, false, true, false, null, java.util.List.of(), false, null)));
        assertEquals(ResidualRoute.DETERMINISTIC, new ResidualRouter().route(new ResidualEnrichmentRequest(
                "cobol-ir.v1", "node-1", "CONTROL_FLOW", ResidualConstruction.CONTROL_FLOW_COMPONENT,
                false, true, false, false, null, java.util.List.of(), false, null)));
    }

    @Test
    void incompatibleResidualSignalsFailClosedWithoutExecutorCall() {
        AtomicInteger calls = new AtomicInteger();
        ResidualEnrichmentCoordinator coordinator = new ResidualEnrichmentCoordinator(new ResidualRouter(),
                (route, request) -> { calls.incrementAndGet(); return JSON.createObjectNode(); });
        ResidualEnrichmentRequest ambiguous = new ResidualEnrichmentRequest("cobol-ir.v1", "node-1",
                "PARAGRAPH", ResidualConstruction.PARAGRAPH, true, true, true, false, null,
                java.util.List.of("existingMethod"), true, "tool-20260830t12345678901234z");

        assertEquals(ResidualRoute.DETERMINISTIC,
                coordinator.enrich(ambiguous, JSON.createObjectNode()).route());
        assertEquals(0, calls.get());
    }

    @Test
    void explicitDomainRequestCarriesGovernedScopeSignatureAndToolRun() {
        ResidualEnrichmentRequest request = domainRequest();
        assertEquals(java.util.List.of("existingMethod"), request.collisionScope());
        assertEquals(true, request.publicSignatureProtected());
        assertEquals("tool-20260830t12345678901234z", request.agoraToolRunRef());
        assertThrows(IllegalArgumentException.class, () -> new ResidualEnrichmentRequest(
                "cobol-ir.v1", "node-1", "PARAGRAPH", ResidualConstruction.PARAGRAPH,
                true, false, false, false, null, java.util.List.of(), false, null));
    }

    @Test
    void residualCoordinatorInvokesExecutorExactlyOnceAndPreservesDeterministicResult() {
        AtomicInteger calls = new AtomicInteger();
        JsonNode proposal = JSON.createObjectNode().put("suggestedName", "calculateInterest");
        JsonNode deterministic = JSON.createObjectNode().put("paragraph", "1000-PROC");
        ResidualEnrichmentCoordinator coordinator = new ResidualEnrichmentCoordinator(
                new ResidualRouter(), (route, request) -> {
                    calls.incrementAndGet();
                    assertEquals(ResidualRoute.DOMAIN_NAMING, route);
                    return proposal;
                });

        ResidualEnrichmentOutcome outcome = coordinator.enrich(domainRequest(), deterministic);

        assertEquals(1, calls.get());
        assertSame(deterministic, outcome.deterministicResult());
        assertSame(proposal, outcome.proposal());
    }

    @Test
    void deterministicRouteCannotExposePromptId() {
        assertThrows(IllegalStateException.class, ResidualRoute.DETERMINISTIC::promptId);
    }

    private static ResidualEnrichmentRequest request(ResidualConstruction construction) {
        return new ResidualEnrichmentRequest("cobol-ir.v1", "node-1", construction.name(), construction,
                false, false, false, false, null, java.util.List.of(), false, null);
    }

    private static ResidualEnrichmentRequest domainRequest() {
        return new ResidualEnrichmentRequest("cobol-ir.v1", "node-1", "PARAGRAPH",
                ResidualConstruction.PARAGRAPH, true, false, false, false, null,
                java.util.List.of("existingMethod"), true, "tool-20260830t12345678901234z");
    }

    private static ResidualEnrichmentRequest controlFlowRequest() {
        return new ResidualEnrichmentRequest("cobol-ir.v1", "node-1", "CONTROL_FLOW",
                ResidualConstruction.CONTROL_FLOW_COMPONENT, false, true, true, false, null,
                java.util.List.of(), false, null);
    }

    private static ResidualEnrichmentRequest intentRequest(ResidualConstruction construction) {
        return new ResidualEnrichmentRequest("cobol-ir.v1", "node-1", "DATA_ITEM", construction,
                false, false, false, true, null, java.util.List.of(), false, null);
    }

    private static ResidualEnrichmentRequest unsupportedRequest() {
        return new ResidualEnrichmentRequest("cobol-ir.v1", "node-1", "STATEMENT",
                ResidualConstruction.UNSUPPORTED, false, false, false, false, "COBOL_UNSUPPORTED",
                java.util.List.of(), false, null);
    }
}
