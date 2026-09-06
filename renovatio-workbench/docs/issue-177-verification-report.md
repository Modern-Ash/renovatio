# Issue 177 verification report

- Date: 2026-09-06
- Runtime: Eclipse Theia `1.75.0`, Node `24.20.0`, Docker Linux executor
- Implemented shell: five Renovatio activity areas, project explorer, Theia commands/keybindings,
  browser-storage persistence and configured dashboard continuity link

## Automated checks

| Check | Result |
| --- | --- |
| Docker production build | Pass; browser and node bundles report zero errors |
| Extension contracts | Pass; 7 tests, 0 failures |
| Runtime startup / HTTP smoke | Pass; Theia listens on port 3000 and returns HTTP 200 |
| Bundle inclusion | Pass; `renovatio.shell.equivalence` found in the final frontend bundle |
| Source boundary | Pass; shell contract asserts no direct `Wizard` or `renovatio-ui/src` import |

## Coverage

The contract tests cover registration for Project, Analysis, Architecture, AI and Equivalence;
six project asset classes; command keybindings; accessible navigation labels; browser-storage keys;
and explicit permission/error states. The dashboard is an external configured link through
`RENOVATIO_DASHBOARD_URL`, keeping the shell independent from existing wizard internals.

## Remaining limits

This work proceeds under the explicitly recorded development-only exception to the #176 dependency.
No browser screenshot, real keyboard interaction, screen-reader pass, or macOS run is claimed here;
those remain pending in #176 and must be revisited before production/release acceptance.
