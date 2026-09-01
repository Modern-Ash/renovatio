package org.shark.renovatio.architecture;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.profile.MigrationProfile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectureRequestTest {
    @Test
    void canonicalizesProgramsConfigurationAndEvidence() {
        var first = ArchitectureFixtures.program("PAY001", "src/z.cob", '1');
        var second = ArchitectureFixtures.program("CUS001", "src/a.cob", '2');
        GroupingConfiguration grouping = new GroupingConfiguration("Core Module",
                Map.of("pay001", "Payment Domain"), Map.of("customer-rec", "Customers"),
                Map.of("PAY", "Payments"));

        ArchitectureRequest request = ArchitectureRequest.create(List.of(first, second),
                ArchitectureFixtures.effective(MigrationProfile.ArchitectureStyle.HEXAGONAL,
                        MigrationProfile.ModuleGrouping.BY_DOMAIN), grouping,
                Map.of("cus001", List.of("customer-rec")), List.of("f".repeat(64), "a".repeat(64)));

        assertEquals(List.of("CUS001", "PAY001"), request.programs().stream().map(value -> value.programId()).toList());
        assertEquals("core-module", request.grouping().singleModuleName());
        assertEquals(Map.of("PAY001", "payment-domain"), request.grouping().manualModules());
        assertEquals(List.of("a".repeat(64), "f".repeat(64)), request.acceptedEvidenceHashes());
        assertEquals(64, request.requestHash().length());

        ArchitectureRequest reordered = ArchitectureRequest.create(List.of(second, first), request.effectiveProfile(),
                new GroupingConfiguration("Core Module", Map.of("PAY001", "Payment Domain"),
                        Map.of("CUSTOMER-REC", "Customers"), Map.of("PAY", "Payments")),
                Map.of("CUS001", List.of("CUSTOMER-REC")), List.of("a".repeat(64), "f".repeat(64)));
        assertEquals(request.requestHash(), reordered.requestHash());
        assertEquals(request, reordered);
    }

    @Test
    void rejectsAmbiguousOrInconsistentRequests() {
        var program = ArchitectureFixtures.program("PAY001", "src/a.cob", '1');
        var duplicateId = ArchitectureFixtures.program("PAY001", "src/b.cob", '2');
        var duplicatePath = ArchitectureFixtures.program("PAY002", "src/a.cob", '3');
        var effective = ArchitectureFixtures.effective(MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT,
                MigrationProfile.ModuleGrouping.BY_PROGRAM);

        assertThrows(IllegalArgumentException.class, () -> ArchitectureRequest.create(List.of(), effective,
                GroupingConfiguration.empty(), Map.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> ArchitectureRequest.create(List.of(program, duplicateId),
                effective, GroupingConfiguration.empty(), Map.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> ArchitectureRequest.create(List.of(program, duplicatePath),
                effective, GroupingConfiguration.empty(), Map.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> ArchitectureRequest.create(List.of(program), effective,
                GroupingConfiguration.empty(), Map.of("UNKNOWN", List.of("X")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ArchitectureRequest("1", List.of(program), effective,
                GroupingConfiguration.empty(), Map.of(), List.of(), "0".repeat(64)));
        assertThrows(IllegalArgumentException.class, () -> new GroupingConfiguration("migration",
                Map.of(), Map.of(), Map.of("BAD PREFIX!", "x")));
    }
}
