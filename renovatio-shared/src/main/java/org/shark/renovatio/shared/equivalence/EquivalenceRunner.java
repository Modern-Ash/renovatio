package org.shark.renovatio.shared.equivalence;

/** Boundary implemented by COBOL and generated-target fixture executors. */
@FunctionalInterface
public interface EquivalenceRunner {
    Observation run(Fixture fixture) throws Exception;

    record Fixture(String id, String inputHash) {
        public Fixture { if (id == null || id.isBlank() || inputHash == null || inputHash.isBlank()) throw new IllegalArgumentException("fixture id and input hash are required"); }
    }
    record Observation(String outputHash, java.util.Map<String, String> invariants) {
        public Observation { invariants = java.util.Map.copyOf(invariants == null ? java.util.Map.of() : invariants); }
    }
}
