package org.shark.renovatio.decisions;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.profile.MigrationProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.shark.renovatio.decisions.DecisionPoint.*;

class DecisionDomainTest {
    private static final Instant NOW = Instant.parse("2026-09-01T00:00:00Z");
    private static final String IR = "a".repeat(64);

    @Test
    void catalogIsExactStableAndHighConfidence() {
        List<DecisionPoint> points = F1DecisionCatalog.create(IR, NOW);
        assertEquals(7, points.size());
        assertEquals(List.of("java.numeric.unscaled-type", "java.naming.identifier-mapping",
                        "java.generated-package", "java.accessor-convention", "java.framework-coupling",
                        "cobol.pic.default-usage", "java.value-initializer-policy"),
                points.stream().map(DecisionPoint::decisionKey).toList());
        assertTrue(points.stream().allMatch(value -> value.confidence().compareTo(BigDecimal.ONE) == 0));
        assertTrue(points.stream().allMatch(value -> value.location().equals(Location.project())));
        assertEquals(points.get(0).id(), DecisionIdentity.id(points.get(0).category(),
                points.get(0).decisionKey(), points.get(0).location()));
    }

    @Test
    void identityDoesNotDependOnPresentationFields() {
        DecisionPoint original = F1DecisionCatalog.create(IR, NOW).get(0);
        DecisionPoint changed = new DecisionPoint("1", original.id(), original.category(),
                original.decisionKey(), original.location(), "Different question",
                List.of("BIG_INTEGER", "ALWAYS_LONG", "CURRENT_PIC_MAPPING"),
                original.defaultOption(), original.chosenOption(),
                original.source(), original.confidence(), "Different rationale", List.of("other evidence"),
                original.status(), "b".repeat(64), false, null, 2, NOW, NOW.plusSeconds(1), true);
        assertEquals(original.id(), changed.id());
    }

    @Test
    void patchConfirmsOverridesAndIsIdempotent() {
        DecisionPoint auto = F1DecisionCatalog.create(IR, NOW).get(3);
        DecisionPoint confirmed = DecisionTransitions.patch(auto, "JAVA_BEANS", 1, NOW.plusSeconds(1));
        assertEquals(Status.CONFIRMED, confirmed.status());
        assertEquals(Source.HEURISTIC, confirmed.source());
        assertEquals(2, confirmed.revision());
        assertSame(confirmed, DecisionTransitions.patch(confirmed, "JAVA_BEANS", 2, NOW.plusSeconds(2)));

        DecisionPoint overridden = DecisionTransitions.patch(confirmed, "FLUENT", 2, NOW.plusSeconds(2));
        assertEquals(Status.OVERRIDDEN, overridden.status());
        assertEquals(Source.USER, overridden.source());
        assertEquals(BigDecimal.ONE, overridden.confidence());
        assertSame(overridden, DecisionTransitions.patch(overridden, "FLUENT", 3, NOW.plusSeconds(3)));
        assertThrows(DecisionTransitions.StaleDecisionException.class,
                () -> DecisionTransitions.patch(overridden, "JAVA_BEANS", 2, NOW));
        assertThrows(DecisionTransitions.InvalidOptionException.class,
                () -> DecisionTransitions.patch(overridden, "UNKNOWN", 3, NOW));
    }

    @Test
    void reconcilePreservesValidUserChoiceAndInvalidatesRemovedChoice() {
        DecisionPoint base = F1DecisionCatalog.create(IR, NOW).get(3);
        DecisionPoint overridden = DecisionTransitions.patch(base, "FLUENT", 1, NOW.plusSeconds(1));
        DecisionPoint next = F1DecisionCatalog.create("b".repeat(64), NOW.plusSeconds(2)).get(3);
        DecisionPoint preserved = DecisionTransitions.reconcile(overridden, next, NOW.plusSeconds(2));
        assertEquals("FLUENT", preserved.chosenOption());
        assertEquals(Status.OVERRIDDEN, preserved.status());

        DecisionPoint restricted = new DecisionPoint("1", next.id(), next.category(), next.decisionKey(),
                next.location(), next.question(), List.of("JAVA_BEANS", "RECORDS"), "JAVA_BEANS",
                "JAVA_BEANS", Source.HEURISTIC, BigDecimal.ONE, next.rationale(), next.evidence(), Status.AUTO,
                next.semanticIrHash(), false, null, 1, NOW, NOW, true);
        DecisionPoint reset = DecisionTransitions.reconcile(overridden, restricted, NOW.plusSeconds(3));
        assertEquals(Status.AUTO, reset.status());
        assertEquals("JAVA_BEANS", reset.chosenOption());
        assertTrue(reset.evidence().contains("PREVIOUS_CHOICE_INVALIDATED"));
    }

    @Test
    void bulkConfirmUsesInclusiveThresholdAndStableIdOrder() {
        List<DecisionPoint> input = new ArrayList<>(F1DecisionCatalog.create(IR, NOW));
        var result = DecisionTransitions.bulkConfirm(input, BigDecimal.ONE, NOW.plusSeconds(1));
        assertEquals(7, result.confirmed());
        assertEquals(0, result.skipped());
        assertEquals(result.items().stream().map(DecisionPoint::id).sorted().toList(),
                result.items().stream().map(DecisionPoint::id).toList());
        var repeated = DecisionTransitions.bulkConfirm(result.items(), BigDecimal.ONE, NOW.plusSeconds(2));
        assertEquals(0, repeated.confirmed());
        assertEquals(7, repeated.skipped());
    }

    @Test
    void resolverUsesDefaultsUntilChoicesAreAccepted() {
        List<DecisionPoint> points = new ArrayList<>(F1DecisionCatalog.create(IR, NOW));
        DecisionPoint accessor = points.stream().filter(value -> value.decisionKey().equals("java.accessor-convention"))
                .findFirst().orElseThrow();
        points.set(points.indexOf(accessor), DecisionTransitions.patch(accessor, "FLUENT", 1, NOW));
        var effective = new DecisionResolver().resolve(MigrationProfiles.emptyOverlay(), points);
        assertEquals(org.shark.renovatio.profile.MigrationProfile.Naming.FLUENT,
                effective.profile().style().naming());
        assertEquals(1, effective.appliedDecisionIds().size());
        assertEquals(7, effective.resolvedDecisions().size());
    }

    @Test
    void retirementIsIdempotentAndDuplicateActiveIdentityIsRejected() {
        DecisionPoint first = F1DecisionCatalog.create(IR, NOW).get(0);
        DecisionPoint retired = DecisionTransitions.retire(first, NOW.plusSeconds(1));
        assertFalse(retired.active());
        assertSame(retired, DecisionTransitions.retire(retired, NOW.plusSeconds(2)));

        DecisionPoint duplicate = new DecisionPoint(first.schemaVersion(), "b".repeat(64), first.category(),
                first.decisionKey(), first.location(), first.question(), first.options(), first.defaultOption(),
                first.chosenOption(), first.source(), first.confidence(), first.rationale(), first.evidence(),
                first.status(), first.semanticIrHash(), false, null, first.revision(), first.createdAt(),
                first.updatedAt(), true);
        assertThrows(IllegalStateException.class,
                () -> new DecisionResolver().resolve(MigrationProfiles.emptyOverlay(), List.of(first, duplicate)));
    }
}
