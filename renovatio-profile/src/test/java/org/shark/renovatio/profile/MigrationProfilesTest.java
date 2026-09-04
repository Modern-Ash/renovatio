package org.shark.renovatio.profile;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.shark.renovatio.profile.MigrationProfile.*;

class MigrationProfilesTest {
    @Test
    void resolvesPartialOverlayWithoutMutatingDefaults() {
        MigrationProfile overlay = new MigrationProfile("1", Map.of("team", "core"),
                null, new Architecture(ArchitectureStyle.HEXAGONAL, null), null,
                null, null, null);
        MigrationProfile effective = MigrationProfiles.resolve(overlay);
        assertEquals(ArchitectureStyle.HEXAGONAL, effective.architecture().style());
        assertEquals(ModuleGrouping.BY_PROGRAM, effective.architecture().moduleGrouping());
        assertEquals(Language.JAVA, effective.target().language());
        assertEquals(Map.of("team", "core"), effective.extensions());
    }

    @Test
    void jsonAndYamlRoundTripSemantically() {
        MigrationProfile expected = MigrationProfiles.defaults();
        assertEquals(expected, MigrationProfiles.readJson(MigrationProfiles.writeJson(expected)));
        assertEquals(expected, MigrationProfiles.readYaml(MigrationProfiles.writeYaml(expected)));
    }

    @Test
    void extensionsRoundTripExplicitNullValues() {
        MigrationProfile json = MigrationProfiles.readJson(
                "{\"schemaVersion\":\"1\",\"extensions\":{\"vendorSetting\":null}}");
        MigrationProfile yaml = MigrationProfiles.readYaml("""
                schemaVersion: "1"
                extensions:
                  vendorSetting: null
                """);

        assertTrue(json.extensions().containsKey("vendorSetting"));
        assertNull(json.extensions().get("vendorSetting"));
        assertEquals(json, yaml);
        assertEquals(json, MigrationProfiles.readJson(MigrationProfiles.writeJson(json)));
        assertEquals(yaml, MigrationProfiles.readYaml(MigrationProfiles.writeYaml(yaml)));
        assertThrows(UnsupportedOperationException.class,
                () -> json.extensions().put("other", true));
    }

    @Test
    void rejectsUnknownFieldsAndInvalidLlmCombinations() {
        assertThrows(MigrationProfiles.ProfileFormatException.class,
                () -> MigrationProfiles.readJson("{\"schemaVersion\":\"1\",\"extensions\":{},\"unknown\":1}"));
        MigrationProfile invalid = new MigrationProfile("1", Map.of(), null, null, null, null, null,
                new Llm(false, true, 0));
        List<MigrationProfiles.Violation> violations = MigrationProfiles.validateOverlay(invalid);
        assertEquals(List.of("/llm/maxSuggestionsPerRun", "/llm/suggestDecisions"),
                violations.stream().map(MigrationProfiles.Violation::path).toList());
    }

    @Test
    void requiresVersionExtensionsAndBoundedLanguageVersion() {
        MigrationProfile invalid = new MigrationProfile("2", null,
                new Target(Language.JAVA, " "), null, null, null, null, null);
        assertEquals(3, MigrationProfiles.validateOverlay(invalid).size());
    }

    @Test
    void canonicalHashIgnoresMapInsertionOrder() {
        Map<String, Object> first = new LinkedHashMap<>(); first.put("b", 2); first.put("a", 1);
        Map<String, Object> second = new LinkedHashMap<>(); second.put("a", 1); second.put("b", 2);
        assertEquals(MigrationProfiles.canonical(first), MigrationProfiles.canonical(second));
        assertEquals(MigrationProfiles.sha256(MigrationProfiles.canonical(first)),
                MigrationProfiles.sha256(MigrationProfiles.canonical(second)));
    }

    @Test
    void effectiveProfileAppliesOnlyMappedDecisionsAndSortsIdentityInputs() {
        Map<String, String> decisions = new LinkedHashMap<>();
        decisions.put("java.framework-coupling", "PLAIN_JAVA");
        decisions.put("java.accessor-convention", "FLUENT");
        decisions.put("java.generated-package", "org.shark.renovatio.generated.cobol");
        var result = MigrationProfiles.effective(MigrationProfiles.emptyOverlay(), decisions,
                Map.of("java.framework-coupling", "PLAIN_JAVA", "java.accessor-convention", "FLUENT"),
                List.of("b", "a"));
        assertEquals(Framework.NONE, result.profile().runtime().framework());
        assertEquals(Naming.FLUENT, result.profile().style().naming());
        assertEquals(List.of("a", "b"), result.appliedDecisionIds());
        assertEquals(64, result.profileHash().length());
    }

    @Test
    void unboundLayeredProfileRetainsLegacyEffectiveHash() {
        MigrationProfile overlay = new MigrationProfile("1", Map.of("dialect", "IBM"),
                new Target(Language.JAVA, "21"), null, null, null, null, null);
        Map<String, String> decisions = Map.of("java.accessor-convention", "FLUENT");
        List<String> ids = List.of("b", "a");

        var legacy = MigrationProfiles.effective(overlay, decisions, decisions, ids);
        var layered = MigrationProfiles.effectiveLayers(MigrationProfiles.emptyOverlay(), Map.of(),
                overlay, decisions, decisions, ids, Map.of());

        assertEquals(legacy.profile(), layered.profile());
        assertEquals(legacy.profileHash(), layered.profileHash());
    }
}
