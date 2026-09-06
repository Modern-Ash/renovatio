# Issue #176 — Theia platform and distribution spike

## Outcome

Produce a runnable, reviewable Eclipse Theia prototype that proves whether Theia is a suitable
web-first shell for Renovatio before any production screen is migrated. The decision must be
supported by an ADR, reproducible execution instructions, a compatibility and risk matrix, and
verification evidence; the spike itself must not replace or modify the current React dashboard or
production workflow.

## Scope

- Create a minimal Theia application that opens a local workspace and exposes the standard file
  tree.
- Add one Renovatio extension with a visible custom widget, a command that opens the widget, and a
  menu contribution.
- Prove the browser distribution on Linux and macOS. Linux CI must perform a clean dependency
  install, build, startup, and HTTP smoke test; macOS browser verification may be manual when a
  macOS CI runner is unavailable and must be identified as such.
- Evaluate desktop packaging as a secondary distribution. A documented feasibility result is
  sufficient; shipping or supporting a production desktop build is outside this increment.
- Evaluate Open VSX and an explicit allowlist of extensions required by the prototype. Unsupported
  APIs, proprietary Marketplace dependencies, and version constraints must be recorded rather
  than hidden by fallbacks.
- Define environment-driven backend connectivity and an authentication integration seam. The
  prototype must preserve the current backend contract, support a configurable backend base URL,
  and must not embed credentials or secrets in frontend bundles.
- Document licensing, telemetry defaults, Content Security Policy, workspace isolation, command
  allowlisting, and extension trust boundaries.
- Begin implementation from the current `main` tip in an isolated worktree or clean checkout, as
  required by Epic #175. Existing uncommitted work from another branch must not be folded into the
  spike implicitly.

## Version and platform decision

The spike may compare candidate versions, but its accepted result must pin exact Theia and Node
versions in package manifests, the ADR, and the runbook. The selected Node line must be supported by
the chosen Theia release. The ADR must compare Eclipse Theia, Code-OSS, OpenVSCode Server, and a
custom Monaco shell against at least: web and desktop distribution, extensibility, maintenance
cost, licensing, telemetry control, extension ecosystem, security boundaries, and Renovatio widget
integration.

The ADR may reject Theia if the evidence exposes a material blocker. In that case the prototype,
risk matrix, and recommendation still satisfy the spike only when the rejection is explicit and
technically reviewed; issue #177 must remain blocked pending a replacement platform decision.

## Prototype contract

The prototype is independently runnable and does not depend on internal React wizard components.
It may call or proxy existing `renovatio-api` endpoints, but it must not change backend domain,
parser, projection, emission, LLM, or equivalence behavior. The existing dashboard remains the
authoritative production UI throughout this work.

Configuration must distinguish local development, CI, and deployable browser environments. At a
minimum it defines the backend base URL, local workspace root or workspace selection behavior,
extension registry/allowlist configuration, telemetry setting, and authentication mode or adapter.
Unsafe defaults, cross-origin requirements, and CSP exceptions must be visible in the runbook and
risk matrix.

## Required artifacts

- `spec`: this governed specification.
- `implementation-plan`: file layout, dependency/version selection process, test strategy, CI
  changes, and rollback/removal steps.
- `prototype`: the buildable Theia application and Renovatio extension.
- `architecture-decision-record`: comparison, decision, exact versions, consequences, and rejected
  alternatives.
- `runbook`: Docker/local startup and environment configuration instructions.
- `compatibility-risk-matrix`: operating systems, browsers, desktop feasibility, extensions,
  licensing, telemetry, CSP, authentication, and security constraints.
- `verification-report`: commands and results for clean build, startup, HTTP smoke, browser checks,
  and any manual evidence.
- `review-report`: independent technical review of the decision and recorded limitations.

## Acceptance evidence

Acceptance requires all eight Agora criteria to reach their required stages, every artifact above
to be registered, at least one successful evidence record, and Spec Owner approval. The verification
report must identify exact commands, environment, versions, commit or content identity, and whether
each result was automated or manual. A screenshot may support the widget/browser check but cannot
replace build and smoke-test evidence.

## Exclusions

- Migrating production dashboard screens or disabling the current dashboard.
- Rewriting the Spring Boot API or domain engine.
- Committing to a production desktop release.
- Assuming compatibility with all VS Code extensions.
- Enabling autonomous or unreviewed code-generation actions.

## Dependency on issue #177

Issue #177 remains blocked until this work is completed with an accepted platform ADR, pinned
Theia and Node versions, a successful prototype build/start/smoke result, and documented extension,
distribution, security, and configuration risks. If the ADR rejects Theia, #177 requires a new or
revised specification before it can resume.
