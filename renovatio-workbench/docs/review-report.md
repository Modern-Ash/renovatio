# Issue 176 technical review

- Date: 2026-09-05
- Review pass: `platform-spike-technical-review`
- Reviewed commit: `70ee6fb183849fa8c6e9ff97d513d38d0cf70329`
- Method: Agora `spec-driven`
- Verdict: conditionally acceptable as a Linux platform spike; not eligible for completion

## Confirmed checks

- The implementation is isolated under `renovatio-workbench/` plus its scoped CI workflow and
  does not replace the existing React dashboard or Spring Boot backend.
- All direct Theia dependencies are pinned to `1.75.0`; Node and npm are pinned for local, Docker
  and CI execution.
- The production build succeeds with zero Theia bundle errors, 3/3 contract tests pass, the server
  returns HTTP 200, and the final bundle contains `renovatio.workbench.open`.
- The prototype contributes a `ReactWidget`, command, main-menu action, file navigator packages,
  environment adapter seam, accessible focus state, responsive layout and reduced-motion handling.
- The ADR covers Theia, Code-OSS, OpenVSCode Server and Monaco, and its recommendation is explicitly
  conditional on the open platform and security evidence.
- Open VSX/plugin execution is disabled. The production dependency graph has 0 high and 0 critical
  findings after pruning; the 22 moderate findings and the development-only critical advisory are
  explicitly recorded.

## Findings

### High — required interactive and macOS evidence is absent

The current executor proves Linux bundle construction and HTTP readiness but cannot prove that the
widget is visibly rendered, that its menu/command opens it, or that the browser target runs on
macOS. The in-app browser reported no browser instance, and no macOS runner is available. This
blocks `renovatio-widget`, `platform-distribution`, and `verification-evidence` from reaching the
verified/accepted stages.

Required resolution: attach a browser capture showing the widget, record a palette or menu open
interaction, and run/capture the same pinned build on macOS.

### High — vulnerable archive library remains in the build toolchain

`@theia/cli@1.75.0` retains development-only `decompress@4.2.1`. The Docker multi-stage build and
production prune prevent it from entering the runtime dependency graph, and the spike processes no
untrusted plugin archives. That is a bounded spike containment, not production supply-chain
approval.

Required resolution before release: consume a compatible upstream fix, replace the affected build
path, or obtain an explicit security approval backed by sandbox and archive-input controls.

### Medium — inherited moderate runtime advisories

The pinned production graph reports 22 moderate findings through Theia dependencies including
DOMPurify, `qs`, and `uuid`. npm proposes incompatible Theia downgrades rather than a safe in-line
fix. These findings do not block local spike verification but do block an unconditional production
recommendation.

## Gate decision

Do not accept or complete issue #176 yet. Keep it in `verifying`, record the two high findings as
blockers, and keep issue #177 blocked by #176. Once interactive Linux/macOS evidence is available,
rerun the review; the build-toolchain finding may remain an explicit release-level follow-up if the
spec owner accepts that scope for the platform decision.
