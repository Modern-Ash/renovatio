package org.shark.renovatio.decisions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.profile.TemplateReference;

import static org.junit.jupiter.api.Assertions.*;

class DecisionPoliciesTest {
    private static final Instant NOW = Instant.parse("2026-09-04T00:00:00Z");
    @TempDir Path temporary;

    @Test
    void exportsAndAutoConfirmsEquivalentDecisionWithProvenance() {
        DecisionPoint source = confirmed(F1DecisionCatalog.create("a".repeat(64), NOW).get(0));
        var catalog = DecisionPolicies.exportCatalog("bank", "1", "project-a", "analyzer-1",
                List.of(source), Map.of(), null, null, NOW);
        DecisionPoint target = F1DecisionCatalog.create("b".repeat(64), NOW).get(0);

        var result = DecisionPolicies.apply(catalog, List.of(target), "analyzer-1", Map.of(), NOW.plusSeconds(1));

        assertEquals(1, result.report().autoConfirmed());
        assertEquals(0, result.report().suggested());
        DecisionPoint applied = result.decisions().get(0);
        assertEquals(DecisionPoint.Status.CONFIRMED, applied.status());
        assertEquals(DecisionPoint.Source.POLICY, applied.source());
        assertEquals("bank", applied.policyProvenance().catalogName());
        assertEquals("1", applied.policyProvenance().catalogVersion());
        assertFalse(applied.policyProvenance().stale());

        DecisionPoint overridden = DecisionTransitions.patch(applied, applied.options().get(1), applied.revision(), NOW.plusSeconds(2));
        assertEquals(DecisionPoint.Source.USER, overridden.source());
        assertEquals(DecisionPoint.Status.OVERRIDDEN, overridden.status());
        assertNotNull(overridden.policyProvenance());
    }

    @Test
    void suggestsSubThresholdAndStaleMatchesAndIgnoresWeakMatches() {
        DecisionPoint source = confirmed(F1DecisionCatalog.create("a".repeat(64), NOW).get(0));
        Map<String, String> base = Map.of("a", "1", "b", "2", "c", "3", "d", "4");
        var catalog = DecisionPolicies.exportCatalog("bank", "1", "project-a", "analyzer-1",
                List.of(source), Map.of(source.id(), base), new BigDecimal("0.95"), new BigDecimal("0.75"), NOW);
        DecisionPoint target = F1DecisionCatalog.create("b".repeat(64), NOW).get(0);

        var suggested = DecisionPolicies.apply(catalog, List.of(target), "analyzer-1",
                Map.of(target.id(), Map.of("a", "1", "b", "2", "c", "3", "d", "x")), NOW.plusSeconds(1));
        assertEquals(1, suggested.report().suggested());
        assertEquals(DecisionPoint.Status.SUGGESTED, suggested.decisions().get(0).status());

        var weak = DecisionPolicies.apply(catalog, List.of(target), "analyzer-1",
                Map.of(target.id(), Map.of("a", "x", "b", "x", "c", "x", "d", "x")), NOW.plusSeconds(1));
        assertEquals(1, weak.report().unmatched());
        assertEquals(DecisionPoint.Source.HEURISTIC, weak.decisions().get(0).source());

        var stale = DecisionPolicies.apply(catalog, List.of(target), "analyzer-2",
                Map.of(target.id(), base), NOW.plusSeconds(1));
        assertEquals(1, stale.report().suggested());
        assertTrue(stale.decisions().get(0).policyProvenance().stale());
    }

    @Test
    void preservesLocallyConfirmedHeuristicAndLlmDecisions() {
        DecisionPoint source = confirmed(F1DecisionCatalog.create("a".repeat(64), NOW).get(0));
        source = DecisionTransitions.patch(source, source.options().get(1), source.revision(), NOW.plusSeconds(2));
        var catalog = DecisionPolicies.exportCatalog("bank", "1", "project-a", "analyzer-1",
                List.of(source), Map.of(), null, null, NOW);
        DecisionPoint base = F1DecisionCatalog.create("b".repeat(64), NOW).get(0);
        DecisionPoint heuristic = confirmed(base);
        DecisionPoint llm = DecisionTransitions.suggest(base, base.chosenOption(), new BigDecimal("0.8"),
                "A reviewable LLM suggestion.", NOW.plusSeconds(1));
        llm = DecisionTransitions.patch(llm, llm.chosenOption(), llm.revision(), NOW.plusSeconds(2));

        var result = DecisionPolicies.apply(catalog, List.of(heuristic, llm), "analyzer-1", Map.of(),
                NOW.plusSeconds(3));

        assertEquals(List.of(heuristic, llm), result.decisions());
        assertEquals(0, result.report().autoConfirmed());
        assertEquals(2, result.report().unmatched());
    }

    @Test
    void reportsRemovedPolicyOptionAsStaleSuggestionWithoutApplyingIt() {
        DecisionPoint source = F1DecisionCatalog.create("a".repeat(64), NOW).get(0);
        source = DecisionTransitions.patch(source, source.options().get(1), source.revision(), NOW.plusSeconds(1));
        var catalog = DecisionPolicies.exportCatalog("bank", "1", "project-a", "analyzer-1",
                List.of(source), Map.of(), null, null, NOW);
        DecisionPoint original = F1DecisionCatalog.create("b".repeat(64), NOW).get(0);
        DecisionPoint changed = withOptions(original, List.of(original.options().get(0), "REPLACEMENT"));

        var result = DecisionPolicies.apply(catalog, List.of(changed), "analyzer-1", Map.of(), NOW.plusSeconds(2));

        assertEquals(changed, result.decisions().get(0));
        assertEquals(0, result.report().autoConfirmed());
        assertEquals(1, result.report().suggested());
        assertEquals(0, result.report().unmatched());
        assertEquals(DecisionPolicies.MatchKind.SUGGESTED, result.report().matches().get(0).kind());
        assertTrue(result.report().matches().get(0).stale());
        assertNotNull(result.report().matches().get(0).policyId());
    }

    @Test
    void storesExplicitCoexistingVersionsAndRejectsDifferentContent() {
        DecisionPoint source = confirmed(F1DecisionCatalog.create("a".repeat(64), NOW).get(0));
        var v1 = DecisionPolicies.exportCatalog("bank", "1", "project-a", "analyzer-1",
                List.of(source), Map.of(), null, null, NOW);
        DecisionPoint changed = DecisionTransitions.patch(source, source.options().get(1), source.revision(), NOW.plusSeconds(1));
        var v2 = DecisionPolicies.exportCatalog("bank", "2", "project-a", "analyzer-1",
                List.of(changed), Map.of(), null, null, NOW);
        var repository = new FileDecisionPolicyRepository(temporary.resolve("policies"));
        repository.save(v1);
        repository.save(v2);

        assertEquals(source.chosenOption(), repository.find(new PolicyReference("bank", "1")).orElseThrow()
                .entries().get(0).chosenOption());
        assertEquals(changed.chosenOption(), repository.find(new PolicyReference("bank", "2")).orElseThrow()
                .entries().get(0).chosenOption());
        assertEquals(List.of("1", "2"), repository.list().stream().map(DecisionPolicyCatalog::version).toList());

        var conflict = DecisionPolicyCatalog.create("bank", "1", "analyzer-1", BigDecimal.ONE,
                BigDecimal.ONE, List.of(), NOW);
        assertThrows(FileDecisionPolicyRepository.VersionConflictException.class, () -> repository.save(conflict));
        assertThrows(IllegalArgumentException.class, () -> new PolicyReference("../bank", "1"));
    }

    @Test
    void resolvesTemplatePolicyProjectProfileAndProjectDecisionInOrder() {
        DecisionPoint base = F1DecisionCatalog.create("a".repeat(64), NOW).stream()
                .filter(value -> value.decisionKey().equals("java.accessor-convention")).findFirst().orElseThrow();
        var provenance = new PolicyProvenance("bank", "1", "1".repeat(64), BigDecimal.ONE,
                "2".repeat(64), false);
        DecisionPoint policyFluent = DecisionTransitions.policy(base, "FLUENT", BigDecimal.ONE,
                provenance, true, NOW.plusSeconds(1));
        MigrationProfile template = new MigrationProfile("1", Map.of(), null, null, null, null,
                new MigrationProfile.Style(null, null, null, MigrationProfile.Naming.JAVA_BEANS), null);
        MigrationProfile projectProfile = new MigrationProfile("1", Map.of(), null, null, null, null,
                new MigrationProfile.Style(null, null, null, MigrationProfile.Naming.JAVA_BEANS), null);
        DecisionResolver resolver = new DecisionResolver();

        var profileWins = resolver.resolve(template, new TemplateReference("bank", "1"), List.of(policyFluent),
                new PolicyReference("bank", "1"), projectProfile, List.of());
        assertEquals(MigrationProfile.Naming.JAVA_BEANS, profileWins.profile().style().naming());

        DecisionPoint projectFluent = DecisionTransitions.patch(base, "FLUENT", base.revision(), NOW.plusSeconds(2));
        var decisionWins = resolver.resolve(template, new TemplateReference("bank", "1"), List.of(),
                new PolicyReference("bank", "1"), projectProfile, List.of(projectFluent));
        assertEquals(MigrationProfile.Naming.FLUENT, decisionWins.profile().style().naming());

        var differentBinding = resolver.resolve(template, new TemplateReference("bank", "2"), List.of(),
                new PolicyReference("bank", "1"), projectProfile, List.of(projectFluent));
        assertNotEquals(decisionWins.profileHash(), differentBinding.profileHash());
        assertEquals(MigrationProfiles.resolve(MigrationProfiles.emptyOverlay()),
                resolver.resolve(MigrationProfiles.emptyOverlay(), List.of()).profile());
    }

    private static DecisionPoint confirmed(DecisionPoint value) {
        return DecisionTransitions.patch(value, value.chosenOption(), value.revision(), NOW.plusSeconds(1));
    }

    private static DecisionPoint withOptions(DecisionPoint value, List<String> options) {
        return new DecisionPoint(value.schemaVersion(), value.id(), value.category(), value.decisionKey(),
                value.location(), value.question(), options, options.get(0), options.get(0), value.source(),
                value.confidence(), value.rationale(), value.evidence(), value.status(), value.semanticIrHash(),
                value.policyProvenance(), value.llmFailed(), value.llmFailureCategory(), value.revision(),
                value.createdAt(), value.updatedAt(), value.active());
    }
}
