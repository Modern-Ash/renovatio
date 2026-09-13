---
issue: 235
epic: 221
agora_work: architecture-convergence-2026/community-alpha-release
status: ready-for-release-review
---

# Issue 235: Community Alpha Release Specification

## Objective

Prepare Renovatio `0.3.0-alpha.1` as an honest technical preview after the
required architecture-convergence dependencies are closed and before the Spec
Owner approves publication.

The release must describe Renovatio as a governed modernization toolkit with a
reference COBOL-to-Java path and clearly labelled experimental surfaces. It must
not claim stable production readiness.

## Blocking Dependencies

Publication remains blocked until these conditions are true:

- AC-03 / issue #224 is merged and closed with an owner-approved repository
  license. Verified closed on 2026-09-11.
- AC-01, AC-02, AC-07, AC-09 and AC-10 are closed with their Agora work
  completed. Verified closed on 2026-09-11.
- If remote LLM support is advertised as more than experimental, AC-08 is
  completed; otherwise LLM capability is documented as offline/experimental.
- The Spec Owner approves the release-readiness report for the exact candidate
  SHA.

## Release Scope

The alpha release may include:

- Reference capability: offline COBOL-to-Java generation through the canonical
  application pipeline.
- Beta capability: shared API/CLI/MCP/Workbench surface contracts when backed
  by completed issue #230 evidence.
- Experimental capability: Node, JCL and Python targets unless their respective
  acceptance evidence proves full release readiness.
- Planned capability: any adapter, UI workflow or target that cannot pass the
  release quality gates at candidate time.

## Required Outcomes

- Capability matrix and release notes distinguish `reference`, `beta`,
  `experimental` and `planned`.
- README, architecture docs, contribution/security docs, troubleshooting and
  examples match the runtime behavior of the candidate SHA.
- Version identifiers are coherent across Maven, UI/workbench packages, docs,
  changelog, tag and generated artifacts.
- SBOM, checksums, build provenance and signatures are generated from the exact
  candidate SHA.
- Clean-clone quick start proves the reference COBOL-to-Java path in under ten
  minutes on the supported toolchain.
- Secret, PII and generated-artifact scans pass before tag publication.
- No tag, GitHub Release or artifact publication occurs without explicit
  Spec Owner approval.

## Non-Goals

- Do not publish a stable or production-ready release.
- Do not rewrite history as part of the release unless separately approved.
- Do not include local databases, generated targets, secrets, PII or internal
  Agora evidence not intended for distribution.
- Do not promote experimental targets to supported status without evidence.
