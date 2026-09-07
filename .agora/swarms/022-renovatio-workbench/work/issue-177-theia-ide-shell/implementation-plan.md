# Implementation plan — Issue #177

1. Add an isolated `RenovatioShellWidget` and project domain fixture/store in the existing Theia
   extension. Model six asset classes and explicit loading, empty, permission and error states.
2. Register five stable area commands, menu entries and default keybindings; open each area in the
   standard Theia main area and retain the standard navigator/bottom panel layout.
3. Add a project explorer contribution and an accessible shell navigation surface. Persist the
   selected project and active area through Theia browser storage.
4. Add a dashboard-continuity command/view that reads `RENOVATIO_DASHBOARD_URL` and opens a labelled
   external link without importing any `renovatio-ui` implementation.
5. Extend styling and contract tests for keyboard/ARIA semantics, state persistence, source-boundary
   protection, command registration and asset coverage. Add command and accessibility reports.
6. Rebuild in the pinned Docker toolchain, run contract and HTTP smoke tests, and record only the
   browser/macOS limitation inherited from #176 rather than claiming visual verification.
