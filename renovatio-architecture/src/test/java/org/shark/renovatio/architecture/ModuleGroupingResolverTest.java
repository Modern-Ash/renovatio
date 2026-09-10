package org.shark.renovatio.architecture;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.profile.MigrationProfile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModuleGroupingResolverTest {
    private final ModuleGroupingResolver resolver = new ModuleGroupingResolver();

    @Test
    void appliesManualCopybookLongestPrefixAndProgramFallbackPrecedence() {
        var payManual = ArchitectureFixtures.program("PAY001", "src/pay1.cob", '1');
        var payDomain = ArchitectureFixtures.program("PAY002", "src/pay2.cob", '2');
        var payPrefix = ArchitectureFixtures.program("PAY003", "src/pay3.cob", '3');
        var fallback = ArchitectureFixtures.program("INV001", "src/inv.cob", '4');
        GroupingConfiguration grouping = new GroupingConfiguration("migration",
                Map.of("PAY001", "manual"), Map.of("ACCOUNT-REC", "accounts"),
                Map.of("P", "broad", "PAY", "payments"));
        ArchitectureRequest request = ArchitectureRequest.create(
                List.of(payPrefix, fallback, payDomain, payManual),
                ArchitectureFixtures.effective(MigrationProfile.ArchitectureStyle.HEXAGONAL,
                        MigrationProfile.ModuleGrouping.BY_DOMAIN), grouping,
                Map.of("PAY001", List.of("ACCOUNT-REC"), "PAY002", List.of("ACCOUNT-REC")), List.of());

        var result = resolver.resolve(request);

        assertEquals(Map.of("INV001", "inv001", "PAY001", "manual", "PAY002", "accounts",
                "PAY003", "payments"), result.moduleByProgram());
        assertEquals(List.of("prefix:P"), result.unusedRules());
        assertThrows(UnsupportedOperationException.class, () -> result.moduleByProgram().put("X", "x"));
    }

    @Test
    void supportsByProgramOverridesAndSingleModule() {
        var first = ArchitectureFixtures.program("PAY001", "src/a.cob", '1');
        var second = ArchitectureFixtures.program("CUS001", "src/b.cob", '2');
        var byProgram = ArchitectureRequest.create(List.of(first, second),
                ArchitectureFixtures.effective(MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT,
                        MigrationProfile.ModuleGrouping.BY_PROGRAM),
                new GroupingConfiguration("migration", Map.of("CUS001", "customers"), Map.of(), Map.of()),
                Map.of(), List.of());
        assertEquals(Map.of("CUS001", "customers", "PAY001", "pay001"),
                resolver.resolve(byProgram).moduleByProgram());

        var single = ArchitectureRequest.create(List.of(first, second),
                ArchitectureFixtures.effective(MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT,
                        MigrationProfile.ModuleGrouping.SINGLE_MODULE),
                new GroupingConfiguration("Modern Core", Map.of(), Map.of(), Map.of()), Map.of(), List.of());
        assertEquals(Map.of("CUS001", "modern-core", "PAY001", "modern-core"),
                resolver.resolve(single).moduleByProgram());
    }

    @Test
    void rejectsEqualPrecedenceAndSingleModuleConflicts() {
        var program = ArchitectureFixtures.program("PAY001", "src/a.cob", '1');
        var effective = ArchitectureFixtures.effective(MigrationProfile.ArchitectureStyle.HEXAGONAL,
                MigrationProfile.ModuleGrouping.BY_DOMAIN);
        var copybookConflict = ArchitectureRequest.create(List.of(program), effective,
                new GroupingConfiguration("migration", Map.of(), Map.of("A", "one", "B", "two"), Map.of()),
                Map.of("PAY001", List.of("A", "B")), List.of());
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(copybookConflict));

        var singleConflict = ArchitectureRequest.create(List.of(program),
                ArchitectureFixtures.effective(MigrationProfile.ArchitectureStyle.HEXAGONAL,
                        MigrationProfile.ModuleGrouping.SINGLE_MODULE),
                new GroupingConfiguration("core", Map.of("PAY001", "other"), Map.of(), Map.of()),
                Map.of(), List.of());
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(singleConflict));
    }
}
