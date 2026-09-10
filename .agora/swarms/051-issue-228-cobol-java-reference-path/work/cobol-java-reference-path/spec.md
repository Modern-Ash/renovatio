# Issue 228 corrective specification

## Provenance

This is a retrospective governance reconstruction. PR #249 was merged as commit
`78c4580fac98ec0ad83cad3188aa5857c49fb042` and GitHub issue #228 closed automatically before the
required durable Agora work record existed. This cycle preserves that chronology and does not claim
that the original implementation was governed before merge.

## Problem statement

The merged increment provides three reference fixtures, a COBOL pipeline facade, build and
equivalence helpers, validation utilities, surface tests, and a runbook. Post-merge inspection found
that the public acceptance claim is broader than the implementation:

- parse, Semantic IR, decisions, manifest, and OpenRewrite stages report success without executing
  their named capability;
- Domain Model and Architecture Model stages are absent;
- determinism and idempotency validators compare stage messages rather than generated file sets and
  have no focused tests;
- stale-source validation and semantic-gap collection are not connected to pipeline execution;
- the surface proof exercises analysis helpers rather than the complete reference migration; and
- the runbook still declares Java 17 and stale test totals although the repository requires Java 21.

## Required outcome

Deliver an honest, executable COBOL-to-Java reference path for the three committed fixtures. Every
reported stage must either execute its named production capability or be removed from the success
contract. Generated files must compile, equivalence must compare the complete expected and actual
file sets, determinism must compare independently generated trees byte-for-byte, repeated execution
must be idempotent, changed source must be rejected, unsupported semantics must become stable action
items, and the service/MCP surface proof must traverse the same generation behavior. The runbook
must match the Java 21 toolchain and commands validated from a clean checkout.

## Boundaries

- Keep COBOL-to-Java as the only reference target in this work.
- Do not add remote LLM calls or make network access part of the offline demonstration.
- Do not weaken the canonical application boundary or introduce a source-provider dependency on a
  target provider.
- Do not represent placeholder stage labels as evidence of execution.
- Preserve PR #249 and its review remediation as historical provenance.

## Acceptance criteria

The eight criteria in `WORK.md` are binding. Verification requires focused fixture, pipeline,
determinism, idempotency, stale-input, semantic-gap, equivalence, and surface tests; a Java 21 Maven
reactor run; the characterization guardrail; and successful GitHub checks on the corrective head.

