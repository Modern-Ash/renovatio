# Annotated-IR semantic output verification

Date: 2026-08-30

The Java 17 focused dependency reactor completed with 133 tests passed and zero failures.

Provider output now passes through the accepted issue 124 semantic contract after JSON Schema and
ordered prompt validation. The runtime projects each catalog output into its typed annotation
payload, constructs deterministic provenance, identity and sidecar values, and invokes
`AnnotatedCobolValidator` with the canonical input's node context.

The test suite verifies that a node-kind mismatch reported by the accepted validator discards the
model proposal and produces a deterministic `VALIDATOR_REJECTED` fallback.
