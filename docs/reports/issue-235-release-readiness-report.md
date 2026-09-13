---
issue: 235
epic: 221
agora_artifact: release-readiness-report
status: ready-for-spec-owner-review
---

# Issue 235 Release Readiness Report

## Summary

Renovatio `0.3.0-alpha.1` has a candidate release-readiness package. It is
ready for Spec Owner review of the source candidate, but it is not approved for
tag creation or publication.

## Dependency Status

- AC-01 / issue #222: closed on 2026-09-09.
- AC-02 / issue #223: closed on 2026-09-09.
- AC-03 / issue #224: closed on 2026-09-11 after Apache-2.0 license merge.
- AC-07 / issue #228: closed on 2026-09-10.
- AC-09 / issue #230: closed on 2026-09-11.
- AC-10 / issue #231: closed on 2026-09-11.

## Candidate Gates

- Release scope: candidate release notes and capability matrix distinguish
  reference, beta, experimental and planned capability levels.
- Quickstart: documented as the reference clean-clone path for COBOL-to-Java;
  final publication requires rerun on the approved tag commit.
- Docs: public documentation map is recorded in
  `docs/release/0.3.0-alpha.1-community-docs.md`.
- Versioning: Maven parent/modules and UI/workbench package metadata are aligned
  to `0.3.0-alpha.1`; changelog includes the alpha section.
- Supply chain: candidate SBOM summary, provenance and checksums are present.
  `npm --prefix renovatio-ui audit --audit-level=moderate` passes after
  updating UI dependencies. `npm --prefix renovatio-workbench audit
  --audit-level=moderate` remains blocked by Theia 1.75.0 transitive
  vulnerabilities (`decompress`, `diff`, `dompurify`, `qs`,
  `serialize-javascript`, `uuid`); Theia 1.75.0 is the latest published Theia
  package checked on 2026-09-11, so publication remains blocked until patched
  upstream packages or an approved mitigation are available.
- Quality gates: `scripts/verify-alpha-release.sh 0.3.0-alpha.1` passes.
  `./mvnw -Dexec.skip=true test` passes across the 24-module Maven reactor.
  `npm --prefix renovatio-ui test -- --run` passes 12 files / 29 tests.
  `npm --prefix renovatio-workbench test` passes the core UI contract test.
  Full final gates must be rerun on the exact approved tag commit before GitHub
  Release publication.
- Clean repository: verifier checks known generated database/build artifacts are
  not tracked.
- Approval: pending Spec Owner approval for the exact candidate SHA.

## Decision

Do not tag or publish yet. The next authorized action is Spec Owner review of
this report, including a decision on the Workbench Theia audit blocker.
