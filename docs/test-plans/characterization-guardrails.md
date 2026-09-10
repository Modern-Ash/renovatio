# Test Plan: Characterization Harness and Guardrails

> GitHub issue: [#122](https://github.com/Modern-Ash/renovatio/issues/122)  
> Agora work: `ai-modernization/characterization-guardrails`

## Scope

Verify the twelve-fixture COBOL characterization corpus, strict versioned schemas, deterministic
manual action reports, ordered fail-fast gates, review eligibility, and the credential-free offline
CI lane described by the accepted specification.

## Test layers

1. Unit tests validate schema resolution, schema rejection, content-derived action-item identifiers,
   stable report ordering, redaction, gate ordering, and first-failure behavior.
2. Fixture tests load all twelve declared fixture directories and compare canonical IR, generated
   Java, observable behavior, diagnostics, and manual action items.
3. Integration tests force a failure at schema, compilation, characterization, and review
   eligibility gates and prove that no later gate executes.
4. Reproducibility tests execute supported fixtures twice and compare SHA-256 output hashes.
5. Offline CI runs Maven 3.9.12 on Java 17 in the pinned container with `--network=none`, negative
   connectivity probes, and no provider credentials.

## Commands

```bash
mvn -B -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am -DskipTests package
mvn -B -pl renovatio-provider-cobol,cobol-openrewrite-recipes -am test
```

The CI form adds Maven offline mode (`-o`) inside the network-disabled container. The final report
must record the exact commit, environment, test totals, fixture matrix, output hashes, forced gate
failures, and offline probe results.

## Pass conditions

- Every declared fixture and expected file satisfies its supported or residual contract.
- Unknown schema versions and undeclared fields fail closed.
- Every forced gate failure stops later gates and emits the expected action item.
- Repeated runs are byte-identical.
- The offline lane completes without credentials or network access.
- The full affected reactor compiles and all selected tests pass.
