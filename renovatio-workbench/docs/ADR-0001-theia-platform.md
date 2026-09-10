# ADR-0001: Use Eclipse Theia for the Renovatio Workbench

- Status: proposed by spike; acceptance requires technical review
- Date: 2026-09-05
- Issue: Modern-Ash/renovatio#176
- Versions evaluated: Eclipse Theia `1.75.0`, Node `24.20.0`

## Context

Renovatio needs a web-first IDE shell for source navigation, domain and architecture views, governed
AI interactions, change sets, and equivalence evidence while keeping its existing Spring Boot
domain services and React dashboard available during migration.

## Decision

Adopt Eclipse Theia `1.75.0` as the platform for the spike and recommended base for the Workbench,
subject to the verification and risks recorded with this ADR. Pin Node `24.20.0` and all direct
Theia dependencies, use the browser product first, and treat Electron as a later distribution.
Keep Open VSX/plugin installation disabled until the critical `decompress@4.2.1` advisory in the
Theia 1.75 plugin dependency path has a compatible fix or an explicitly approved containment.

## Options

| Option | Web/desktop | Extension model | Product integration | Maintenance and control | Result |
| --- | --- | --- | --- | --- | --- |
| Eclipse Theia | First-class browser and Electron targets | Theia extensions plus VS Code-compatible plugins through Open VSX | Native widgets, commands, menus, trees and services | Upstream framework; product owns dependency updates, policy and distribution | Selected |
| Code-OSS fork | Desktop-first; web distribution requires substantial product work | Broad VS Code API surface | Deep control but tied to a large fork | Highest merge, branding, telemetry and release burden | Rejected |
| OpenVSCode Server | Strong hosted editor baseline | VS Code extensions with distribution constraints | Faster editor hosting, weaker bespoke product-shell seam | Lower initial cost but less control for Renovatio domain UX | Rejected |
| Monaco custom shell | Web editor primitive only | No complete IDE extension/runtime model | Maximum UI freedom | Must build workspaces, commands, panels, plugins and desktop packaging | Rejected |

## Reasons

Theia provides the smallest maintained platform that satisfies both a browser-first product and
custom IDE contributions without forking the VS Code distribution. The prototype demonstrates a
Renovatio `ReactWidget`, command, menu and local file navigator using supported Theia APIs. Exact
version pinning and a small initial package set constrain upgrade and supply-chain risk.

## Consequences

- Node 24 is a hard runtime boundary for Theia 1.75 and differs from the existing UI's older toolchain.
- Open VSX availability does not guarantee extension compatibility or licensing approval. The
  evaluated Theia 1.75 integration is disabled because its plugin path currently carries a critical
  archive-extraction advisory.
- A production browser deployment needs tenant workspace isolation, gateway authentication, CSP,
  extension allowlisting, resource limits and audit controls beyond this local spike.
- Electron remains feasible but needs signing, notarization, update and sandbox work before support.
- If build, smoke, browser, or security review exposes a blocking issue, this ADR must change to
  `rejected` and issue #177 must remain blocked.
