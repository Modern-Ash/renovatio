package org.shark.renovatio.provider.cobol.guardrail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManualActionItemIdsTest {

    @Test
    void derivesStableContentAddressedId() {
        String first = ManualActionItemIds.from(
                "input.cob", "SAMPLE", "10:1-10:20", "GO_TO", "Irreducible control flow");
        String second = ManualActionItemIds.from(
                "input.cob", "SAMPLE", "10:1-10:20", "GO_TO", "Irreducible control flow");

        assertThat(first).isEqualTo(second).matches("mai-[a-f0-9]{24}");
        assertThat(ManualActionItemIds.from(
                "input.cob", "SAMPLE", "11:1-11:20", "GO_TO", "Irreducible control flow"))
                .isNotEqualTo(first);
    }

    @Test
    void lengthPrefixesPreventAmbiguousConcatenation() {
        assertThat(ManualActionItemIds.from("ab", "c", null, "GO_TO", "reason"))
                .isNotEqualTo(ManualActionItemIds.from("a", "bc", null, "GO_TO", "reason"));
    }
}
