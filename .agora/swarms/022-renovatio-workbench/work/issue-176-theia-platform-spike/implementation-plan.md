# Issue #176 — Implementation plan

## Delivery baseline

- Implement on branch `agora/renovatio-workbench` in the isolated worktree
  `/tmp/renovatio-workbench-main`, created from `origin/main` at `c3a94bf5`.
- Use Eclipse Theia `1.75.0`, the latest stable release observed in npm for this cycle.
- Use Node `24.20.0` and npm `11.19.0`; Theia `v1.75.0` declares `node >=24`. Pin the runtime in
  `.nvmrc`, `package.json`, and the Docker image. Commit an npm lockfile and use `npm ci` in
  automation.
- Keep the existing `renovatio-ui` and `renovatio-api` unchanged.

## Repository layout

Create `renovatio-workbench/` as an independent npm workspace:

- `package.json`, `package-lock.json`, `.nvmrc`, `.npmrc`, `tsconfig.json`: reproducible workspace
  and runtime metadata.
- `applications/browser/`: the browser-hosted Theia product with filesystem, workspace, Monaco,
  navigator, command/menu, Open VSX, and the Renovatio extension.
- `extensions/renovatio-core-ui/`: a frontend extension containing the widget, command, menu
  contribution, view contribution, styling, and focused unit tests.
- `Dockerfile`: deterministic production build and browser runtime using the pinned Node image.
- `README.md`: local/Docker execution, environment contract, smoke command, security defaults, and
  desktop evaluation instructions.
- `config/ovsx-router-config.json`: explicit Open VSX routing and extension boundary.

## Implementation sequence

1. Scaffold the npm workspaces and pin all direct Theia packages to `1.75.0`.
2. Build the minimal browser application and confirm that a local workspace and file navigator load.
3. Implement an industrial/utilitarian Renovatio status widget with one accessible heading, clear
   focus styles, AA contrast, reduced-motion handling, project/backend status, and traceability
   markers.
4. Register `renovatio.workbench.open` in the command palette and a `Renovatio` application menu;
   activate the widget through a Theia `AbstractViewContribution`.
5. Define environment configuration for backend base URL, authentication mode, workspace root,
   telemetry, and Open VSX routing. Do not embed secrets or add a new backend auth protocol.
6. Add unit tests for the widget/contribution contract and an HTTP smoke script that starts the
   built application, waits for readiness, fetches `/`, and terminates cleanly.
7. Add a CI workflow using Node `24.20.0` with `npm ci`, build, tests, and HTTP smoke verification.
8. Add a multi-stage Dockerfile and verify the container serves the Theia application.
9. Produce the ADR, compatibility/risk matrix, runbook content, verification report, and technical
   review. Record Linux checks as automated and macOS/browser/desktop checks according to the
   evidence actually obtained.

## Acceptance coverage

- `prototype-build`: steps 1, 2, 6, 7, and 8.
- `renovatio-widget`: steps 3 and 4.
- `platform-distribution`: steps 2, 7, 8, and 9.
- `extension-compatibility`: steps 1, 2, 4, 5, and 9.
- `environment-security`: steps 5, 8, and 9.
- `adr-decision`: step 9.
- `non-production-impact`: isolated layout and unchanged existing UI/API in all steps.
- `verification-evidence`: steps 6 through 9.

## Verification commands

Run from `renovatio-workbench/`, using Node `24.20.0` directly or the pinned Docker image:

```text
npm ci
npm run build
npm test
npm run smoke
docker build -t renovatio-workbench:issue-176 .
docker run --rm -p 3000:3000 renovatio-workbench:issue-176
curl --fail --silent --show-error http://127.0.0.1:3000/
```

Record exact commands and outcomes in the verification report. No macOS, browser screenshot, or
desktop result will be claimed unless it is actually executed and captured.

## Rollback and containment

The prototype is additive under `renovatio-workbench/`; rollback is removal of that directory and
its dedicated CI job. It does not write into `renovatio-ui`, does not replace Spring Boot static
resources, and does not modify production routing.
