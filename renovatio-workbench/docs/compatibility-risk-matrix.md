# Compatibility and risk matrix

| Area | Release position | Evidence required | Risk / control |
| --- | --- | --- | --- |
| Linux browser | Primary automated platform | Node 24 clean install, production build, startup and HTTP smoke | Native dependency drift; lockfile and pinned CI runtime |
| macOS browser | Required validation platform | Manual or macOS CI launch and widget capture | Not claimable from Linux; record executor, browser and OS version |
| Desktop/Electron | Secondary feasibility only | Dependency/build assessment; optional local package attempt | Signing, notarization, auto-update and sandbox out of scope |
| Node | `24.20.0` | Runtime output and Theia upstream engine contract | Existing Node 20 environments cannot build this package; native modules require Python/C++ at build time |
| Theia | `1.75.0` exact | npm metadata, upstream tag and clean build | Monthly upgrade cadence; use Renovatio extension boundary |
| Open VSX | Evaluated but disabled in the runtime | Registry configuration and dependency audit | Critical `decompress@4.2.1` archive traversal in Theia 1.75 plugin path; enable only after compatible fix/approved containment |
| Production dependency audit | No high or critical findings after production prune; moderate findings are documented release debt only | `npm audit --omit=dev --json` and CI high-severity threshold | Findings are inherited from Theia (`dompurify`, `qs`, `uuid` paths); refresh the pinned Theia line and reassess before release |
| Build dependency audit | `@theia/cli` retains development-only `decompress@4.2.1` | Full install audit plus lockfile path inspection | Build in an isolated container, do not process untrusted archives, prune development dependencies, and require an upstream fix/replacement before release |
| VS Code extensions | Compatible subset only; none loaded by this spike | Per-extension install and behavior tests after security unblock | Marketplace-only/proprietary artifacts are not assumed available |
| Authentication | Existing backend adapter seam | Environment contract and gateway review | No auth exists in the widget; never expose secrets client-side |
| Backend URL | Runtime environment variable | Widget/config verification | Validate origin and proxy policy; production uses approved HTTPS gateway |
| Telemetry | Off by default | Preference and environment review | Opt-in only after data inventory and retention approval |
| CSP | Gateway responsibility | Report actual generated-app requirements | Development bundles may need `unsafe-eval`; tighten before release |
| Workspace | Local/read-only Docker example | File navigator and mount check | Arbitrary host access is unsafe; isolate per tenant in production |
| Commands | Renovatio workbench and activity commands only | `config/release-hardening.json` plus `npm run hardening:audit` | Adding commands requires allowlist review |
| Licensing | Renovatio Apache-2.0; Theia EPL-2.0/GPL-2.0+Classpath dual terms | Dependency/license review | Preserve notices and review redistributed transitive components |

## Release boundary

This matrix supports continuous web usage of the Theia workbench with the existing dashboard kept
operational. Desktop distribution, Open VSX enablement and production CSP measurement remain
separate release gates.
