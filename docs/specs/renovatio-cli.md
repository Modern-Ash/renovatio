# Specification: `renovatio-cli` — command-line adapter over the in-process core

> GitHub issue: [#130](https://github.com/Modern-Ash/renovatio/issues/130) (phase 1 of 3)
> Agora work: `adoption-tooling/renovatio-cli`

## 1. Outcome and boundary

Ship a new Maven module `renovatio-cli` producing a single executable JAR whose `renovatio`
command exposes the COBOL migration capabilities as ordinary subcommands. The CLI boots the
existing Spring application context **headless and in-process** and calls
`LanguageProviderRegistry.routeToolCall(...)` directly. No MCP client, no running server, and no
raw JSON-RPC are required to analyze, plan, apply, diff, or review a COBOL migration.

This module is a sibling adapter to `renovatio-mcp-server`. It is authoritative for issue #130
phase 1 only. It does **not**:

- change any parsing, IR, OpenRewrite, generation, transpilation, or guardrail logic;
- add persistence to `MigrationPlanService` or any core service (deferred to phase 2 / issue #130
  API work);
- re-implement the MCP server — `renovatio serve` delegates to the existing entry points;
- introduce a REST API or web UI (phases 2 and 3).

The MCP server and its behaviour over HTTP `:8082` and `--stdio` are unchanged and remain the
supported programmatic interface.

## 2. Authoritative dependencies

| Dependency | Contract relied upon |
| --- | --- |
| Routing | `org.shark.renovatio.core.service.LanguageProviderRegistry#routeToolCall(String, Map<String,Object>)` returning a result `Map<String,Object>` with a `success` boolean. |
| Reserved argument keys | `workspacePath`, `scope`, `planId`, `runId`, `dryRun`, `language`, `nql` (from `LanguageProviderRegistry.RESERVED_ARGUMENT_KEYS`). |
| COBOL tool names | `cobol.analyze`, `cobol.metrics`, `cobol.plan`, `cobol.apply`, `cobol.diff` (`CobolLanguageProvider`). |
| Context wiring | `McpStdioServerApplication` scans `org.shark.renovatio.mcp.server` + `org.shark.renovatio.core`; the CLI reuses the same base packages so every `LanguageProvider` bean is registered. |
| Guardrail review data | `manual-action-item.v1` JSON as written by `ManualActionItemWriter`, default report path `build/reports/renovatio/manual-action-items.json`. |
| Redaction | `org.shark.renovatio.provider.cobol.guardrail.SensitiveValueRedactor` for any value rendered from a manual action item. |
| MCP entry points | `McpServerApplication.main(String[])` (HTTP) and `McpStdioServerApplication.main(String[])` (stdio). |

The context bootstrap is defined once in a `RenovatioCliContext` helper so tests and every
subcommand share a single configured `ApplicationContext`.

## 3. Command surface

```
renovatio analyze <path> [--dialect IBM|GNU|MF] [--scope '**/*.cbl'] [--json]
renovatio metrics <path> [--scope '**/*.cbl'] [--json]
renovatio plan    <path> [--scope ...] [--strategy incremental|full] [--framework spring-boot] [--json]
renovatio apply   <planId> [--dry-run | --no-dry-run] [--out <dir>] [--json]
renovatio diff    <runId> [--format unified|semantic|both] [--json]
renovatio review  [--report <path>] [--severity error|warning|info] [--json]
renovatio report  [--html <file> | --pdf <file>] [--report <path>]
renovatio serve   [--http | --stdio] [-- <passthrough args>]
renovatio --version | --help
```

Global conventions:

- `<path>` is a workspace directory. It is resolved to an absolute path and passed as
  `workspacePath`.
- `--scope` maps to the reserved `scope` argument; when omitted the core default applies.
- `--dialect`, `--strategy`, `--framework` map to non-reserved argument keys of the same name and
  are forwarded verbatim.
- Exit code `0` when the underlying result `success == true`, `1` when `success == false` or the
  result is null/empty, `2` for a usage error (picocli), `3` for an unexpected exception.
- `--json` prints the raw result map as pretty-printed JSON to stdout and nothing else. Without
  `--json`, a human-readable rendering goes to stdout and diagnostics go to stderr.
- All commands are non-interactive and read no stdin except `serve --stdio`.

### 3.1 `analyze`

Calls `cobol.analyze`. Human output: program count, paragraph/section counts, copybook count,
detected dialect, and any warnings. `--json` emits the full analyze result.

### 3.2 `metrics`

Calls `cobol.metrics`. Human output: a table of LOC, cyclomatic complexity, number of paragraphs,
number of copybooks, and the list of programs above the complexity threshold reported by the core.

### 3.3 `plan`

Calls `cobol.plan`. Prints the returned `planId` prominently, then the plan step summary. Persists
a **plan descriptor** (section 4) so a later `apply` in a separate process can resolve the id.

### 3.4 `apply`

Takes a `planId` produced by `plan`. Resolves it through the descriptor store (section 4), calls
`cobol.apply` with `planId`, `dryRun` (default `true`), and forwards `--out` as `outputDir` when
given. Prints the returned `runId`, modified-file count, and dry-run status. Persists a **run
descriptor**.

`--dry-run` is the default; `--no-dry-run` performs a real apply. A real apply prints the output
directory and the count of files written.

### 3.5 `diff`

Takes a `runId` from `apply`. Resolves it through the descriptor store, calls `cobol.diff`, and
renders the unified diff (`--format unified`, default), the semantic summary (`--format semantic`),
or both.

### 3.6 `review`

Reads a `manual-action-item.v1` report (default `build/reports/renovatio/manual-action-items.json`,
override with `--report`). Renders a checklist **ordered by severity** (`error` before `warning`
before `info`), then by `failedGate`, then by insertion order. Each item renders as:

```
[ ] <severity> · <failedGate> · <programName>
    reason:            <reason>
    required action:   <requiredHumanAction>
    acceptance:        <acceptanceCondition>
    reference:         <diagnosticReference>
```

Every rendered field value is passed through `SensitiveValueRedactor` first. `--severity` filters
to items at or above the given level. `--json` emits the filtered, ordered item array. Exit code is
`0` when the report parses (even with zero items), `1` when the report file is missing or invalid.

### 3.7 `report`

Convenience wrapper that renders the manual-action-item report to a self-contained HTML file
(`--html`) or PDF (`--pdf`) by delegating to the existing report rendering services already used by
the `/reports/html|pdf` endpoints. Exactly one of `--html` / `--pdf` is required.

### 3.8 `serve`

`--stdio` (default) calls `McpStdioServerApplication.main(passthrough)`; `--http` calls
`McpServerApplication.main(passthrough)`. The CLI process becomes the server process. No new server
code is written.

## 4. Cross-invocation id resolution

`MigrationPlanService` keeps plans and runs in memory, so a fresh CLI JVM cannot see a `planId`
created by a previous `plan` invocation. Phase 1 solves this **entirely inside `renovatio-cli`**
without touching core services:

- A `WorkspaceStateStore` writes JSON descriptors under `<workspace>/.renovatio/state/`:
  - `plans/<planId>.json` — `{ planId, workspacePath, nql, scope, strategy, framework, createdAt }`
  - `runs/<runId>.json`  — `{ runId, planId, workspacePath, dryRun, outputDir, createdAt }`
- `plan` mints its own stable `planId` (`UUID`), records the descriptor, and also invokes
  `cobol.plan` once so the human sees the real step summary. The CLI's `planId` is the id shown to
  the user and the only one it accepts back.
- `apply <planId>` loads the descriptor, **replays** `cobol.plan` in the current process to obtain a
  live internal plan id, immediately calls `cobol.apply` against it, then records a run descriptor
  under the CLI's own `runId`.
- `diff <runId>` loads the run descriptor, replays `plan` + `apply(dryRun)` to reconstruct the
  in-memory run, then calls `cobol.diff`.

Replay is deterministic because plans and runs are pure functions of `(nql, scope, workspace)` and
the generators. The store is append-only within a workspace; ids never collide across workspaces
because each workspace has its own `.renovatio/state/`. `.renovatio/` is added to the repository
`.gitignore` guidance in the module README (not committed by the tool).

If a descriptor is missing, the command exits `1` with `unknown plan id <id> — run 'renovatio plan'
first` (or the run equivalent).

## 5. Module layout

```
renovatio-cli/
  pom.xml                     # depends on renovatio-core, renovatio-provider-cobol,
                              # renovatio-provider-java, renovatio-mcp-server, picocli,
                              # spring-boot-starter (no web), jackson; spring-boot-maven-plugin
  src/main/java/org/shark/renovatio/cli/
    RenovatioCli.java         # @Command root, main(); picocli + Spring bootstrap
    RenovatioCliContext.java  # builds/owns the headless ApplicationContext + registry lookup
    WorkspaceStateStore.java  # plan/run descriptor persistence
    OutputWriter.java         # human vs --json rendering, exit-code mapping
    command/AnalyzeCommand.java, MetricsCommand.java, PlanCommand.java, ApplyCommand.java,
            DiffCommand.java, ReviewCommand.java, ReportCommand.java, ServeCommand.java
    review/ManualActionItemReport.java  # jackson binding for manual-action-item.v1 + ordering
  src/main/resources/            # logback config that silences Spring banner + INFO noise on stdout
  src/test/java/org/shark/renovatio/cli/
    <one test class per command> + WorkspaceStateStoreTest + RenovatioCliContextTest
```

`renovatio-cli` is added to the root `pom.xml` `<modules>` list after `renovatio-mcp-server`.

## 6. Context bootstrap

`RenovatioCliContext` runs:

```java
new SpringApplicationBuilder(RenovatioCliConfiguration.class)
    .web(WebApplicationType.NONE)
    .bannerMode(Banner.Mode.OFF)
    .properties("logging.level.root=WARN")
    .run();
```

`RenovatioCliConfiguration` is `@Configuration @ComponentScan({"org.shark.renovatio.core",
"org.shark.renovatio.provider.cobol", "org.shark.renovatio.provider.java"})`. It is created once
per process, cached, and closed on JVM shutdown. Subcommands obtain
`context.getBean(LanguageProviderRegistry.class)`.

`serve` does **not** use this context; it hands control to the MCP application which builds its own.

## 7. Output contract

`OutputWriter` centralizes:

- `--json`: `ObjectMapper` with `INDENT_OUTPUT`, sorted keys, written to stdout, trailing newline.
- human: per-command renderers; never prints stack traces to stdout; a failed result prints
  `error: <message>` to stderr using the result's `message` / `error` / `summary` field.
- exit code mapping from section 3.

No secret, credential, file content, diff body, or source snippet is logged at INFO or above.
Manual-action-item fields are redacted (section 3.6).

## 8. Acceptance scenarios

### 8.1 cli-subcommands

- `renovatio analyze <p1>`, `metrics <p1>`, `plan <p1>`, `apply <planId> --dry-run`,
  `diff <runId>`, `review --report <fixture>`, `report --html <tmp>` each exit `0` against the
  `specs/1-cobol-python-migration/examples/p1` fixture (or a checked-in sample report for `review`
  / `report`), invoking the core in-process with no MCP server started.
- `serve --stdio --help` and `serve --http --help` reach the MCP application without error.

### 8.2 human-readable-output

- Without `--json` each command prints a plain-text rendering and no JSON.
- With `--json` each command prints a single pretty-printed JSON document parseable by
  `ObjectMapper`, and prints nothing else to stdout.

### 8.3 review-checklist

- Given a `manual-action-item.v1` report with mixed severities, `review` lists `error` items before
  `warning` before `info`, shows `reason`, `requiredHumanAction`, `acceptanceCondition`, and
  `diagnosticReference` for each.
- A field containing a value that `SensitiveValueRedactor` masks is rendered masked.
- `--severity warning` omits `info` items; an empty report exits `0` with `no manual action items`.

### 8.4 chained-ids

- `plan <p1>` prints a `planId`; a **new process** `apply <planId> --dry-run` prints a `runId`; a
  **new process** `diff <runId>` renders the diff for that run.
- `apply <unknown>` and `diff <unknown>` exit `1` with an `unknown … id` message.
- Descriptors exist under `<p1>/.renovatio/state/plans` and `.../runs` after the commands run.

### 8.5 no-regression

- `mvn clean install` is green with the new module in the reactor.
- `renovatio-mcp-server` tests are unchanged and pass; starting `McpServerApplication` (HTTP) and
  `McpStdioServerApplication` (stdio) still works and the tool catalog is identical.
- `renovatio serve --stdio` produces the same startup behaviour as running
  `McpStdioServerApplication` directly (asserted by a smoke test that the command dispatches to the
  entry point).

### 8.6 tested

- One test class per subcommand drives the picocli `CommandLine` in-process and asserts stdout,
  exit code, and (for `--json`) JSON shape, against the p1 fixture and checked-in sample reports.
- `WorkspaceStateStoreTest` covers round-trip, missing-id, and workspace isolation.
- `RenovatioCliContextTest` asserts the registry bean resolves and `cobol.analyze` routes.

## 9. Construct-to-test matrix

| Path | Test |
| --- | --- |
| context boots, registry resolves | `RenovatioCliContextTest` |
| `analyze` / `metrics` human + `--json` | `AnalyzeCommandTest`, `MetricsCommandTest` |
| `plan` mints id + writes descriptor | `PlanCommandTest`, `WorkspaceStateStoreTest` |
| `apply` replays plan, writes run descriptor, dry-run default | `ApplyCommandTest` |
| `diff` replays chain, renders formats | `DiffCommandTest` |
| `review` ordering, redaction, `--severity`, empty report | `ReviewCommandTest` |
| `report` html/pdf output file created, mutually exclusive flags | `ReportCommandTest` |
| `serve` dispatches to MCP entry points | `ServeCommandTest` |
| unknown plan/run id → exit 1 | `ApplyCommandTest`, `DiffCommandTest` |
| exit-code mapping | `OutputWriterTest` |

## 10. Delivery artifacts

- this specification, before transition to `clarified`;
- an implementation plan, before implementation;
- the `renovatio-cli` module, wired into the root reactor;
- a module `README.md` documenting the command surface and `.renovatio/` state directory;
- a passing test report, before completion.

## 11. Out of scope

- Persistence in core services, async jobs, REST endpoints, SSE, database (phase 2).
- Any SPA / dashboard / wizard (phase 3).
- Consolidating the three COBOL facades (`CobolLanguageProvider` / `CobolMcpToolsProvider` /
  `CobolProvider`) — noted as a phase-2 precondition; phase 1 targets `cobol.*` tool names as they
  exist today.
- Native image / jlink packaging and installers.
- Changing tool names, argument schemas, or result shapes.
