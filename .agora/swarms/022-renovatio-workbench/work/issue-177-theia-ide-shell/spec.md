# Specification — Issue #177: Renovatio IDE shell

## Scope

Extend the isolated `renovatio-workbench` Theia application from issue #176 with a browser-first
Renovatio IDE shell. The shell provides domain navigation and work-area views without importing,
embedding, or replacing internals of the existing wizard or administrative dashboard.

## Product decisions

- The five requested areas are top-level Renovatio commands and navigable views: Project, Analysis,
  Architecture, AI, and Equivalence.
- Project navigation starts from a deterministic local project fixture/service seam and represents
  COBOL sources, copybooks, JCL, models, runs, and evidence. Live backend project data is out of
  scope; its adapter boundary is explicit.
- Dashboard continuity is a labelled, accessible external link derived from
  `RENOVATIO_DASHBOARD_URL`; it does not embed or import dashboard/wizard code.
- The selected project and last active Renovatio area persist in Theia browser storage. Loading,
  empty, permission-denied, and error states are individually named and visible.
- The standard Theia dock layout supplies tabs, resizable side areas, and a bottom panel. Renovatio
  contributes views and commands; it does not fork the workbench shell.
- Keyboard access uses command palette entries and documented keybindings. All contributed controls
  have names, semantic grouping, focus indication, and keyboard activation.

## Acceptance mapping

| Criterion | Required outcome | Inspectable evidence |
| --- | --- | --- |
| `project-navigation` | Project explorer exposes the six requested asset classes and opens their views | Extension contract test plus navigation smoke |
| `activity-shell` | Five area commands/views coexist with standard Theia tabs, side layout, and bottom panel | Command/view registration and shell smoke |
| `command-access` | Commands appear in palette/menu and their keybindings are documented | Command reference and contract test |
| `dashboard-continuity` | A labelled dashboard link uses configuration without wizard imports | Environment contract test and source boundary check |
| `state-persistence` | Last project/area survives reload with explicit transient and error states | Persistence unit test and smoke |
| `accessibility` | Navigation is keyboard-operable and has accessible labels/ARIA semantics | Accessibility report and automated assertions |
| `wizard-decoupling` | No wizard/dashboard implementation import exists | Dependency/source boundary check |
| `verification-evidence` | Test, smoke, accessibility and command artifacts are captured | Verification and review reports |

## Non-goals and constraints

- Do not change Spring Boot domain flows, production dashboard behavior, or existing wizard source.
- Do not claim browser visual/macOS validation that remains open in issue #176.
- Do not enable Open VSX/plugin execution while the issue #176 security finding remains unresolved.
- The user-authorized exception permits development of #177 only; it is not release or production
  approval.
