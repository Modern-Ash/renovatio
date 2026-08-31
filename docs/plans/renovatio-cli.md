# Implementation Plan: `renovatio-cli` (issue #130 phase 1)

> Spec: `docs/specs/renovatio-cli.md`
> Agora work: `adoption-tooling/renovatio-cli`
> Method: spec-driven + TDD. Every task writes the failing test first.

## Guiding constraints

- Zero changes under `renovatio-core`, `renovatio-provider-*`, `cobol-openrewrite-recipes`,
  `renovatio-cobol-*`. If a change there proves unavoidable, stop and re-open the spec.
- `renovatio-mcp-server` is a compile dependency only so `serve` can call the two `main` methods;
  its sources are untouched.
- Build stays green after every task (`mvn -q -pl renovatio-cli -am test`).

## Task 0 — module skeleton (no behaviour)

1. Create `renovatio-cli/pom.xml`: parent `org.shark.renovatio:renovatio:<version>`, artifactId
   `renovatio-cli`. Dependencies: `renovatio-core`, `renovatio-provider-cobol`,
   `renovatio-provider-java`, `renovatio-mcp-server`, `info.picocli:picocli:4.7.6`,
   `spring-boot-starter` (exclude nothing web-related is pulled), `jackson-databind`,
   `spring-boot-starter-test` (test). Build: `spring-boot-maven-plugin` with mainClass
   `org.shark.renovatio.cli.RenovatioCli`, and `maven-compiler-plugin` picocli annotation
   processor.
2. Add `<module>renovatio-cli</module>` to root `pom.xml` after `renovatio-mcp-server`.
3. Add `info.picocli:picocli` version to root `dependencyManagement`.
4. `RenovatioCli` with an empty `@Command(name="renovatio", subcommands={...})` and a `main` that
   returns `new CommandLine(new RenovatioCli()).execute(args)`.
5. `src/main/resources/logback-spring.xml` (or `logback.xml`): root WARN, pattern to stderr.

**Test:** `RenovatioCliSmokeTest` — `execute("--help")` returns 0 and prints `Usage: renovatio`.

**Gate:** `mvn -q -pl renovatio-cli -am install` green; root reactor still builds.

## Task 1 — headless context + registry lookup

TDD: `RenovatioCliContextTest`
- `registry()` returns a non-null `LanguageProviderRegistry`.
- `registry().routeToolCall("cobol.analyze", Map.of("workspacePath", <p1>))` returns a map with
  `success == true`.
- Second call to `RenovatioCliContext.shared()` returns the same instance.

Implement `RenovatioCliContext`:
- `RenovatioCliConfiguration` `@Configuration @ComponentScan({core, provider.cobol, provider.java})`.
- Lazy singleton building the context via `SpringApplicationBuilder(...).web(NONE).bannerMode(OFF)`
  with `logging.level.root=WARN`; register a shutdown hook to `close()`.
- `LanguageProviderRegistry registry()` accessor.

## Task 2 — OutputWriter + exit-code contract

TDD: `OutputWriterTest`
- `--json` path: given a result map, writes sorted, indented JSON, no extra output.
- human path: `success=false` map prints `error: <message>` to stderr, returns exit 1.
- `success=true` returns exit 0; null/empty map returns exit 1.

Implement `OutputWriter` (captures `PrintStream out/err`, `boolean json`), `render(Map result,
HumanRenderer renderer)` → int exit code. Shared `ObjectMapper` singleton.

## Task 3 — `analyze` and `metrics`

TDD: `AnalyzeCommandTest`, `MetricsCommandTest` (drive `CommandLine`, capture streams)
- `analyze <p1>` exit 0; stdout contains program count line; not JSON.
- `analyze <p1> --json` stdout parses as JSON with `success:true`.
- `analyze --scope '**/*.cob'` forwards `scope`.
- `metrics <p1>` prints a metrics table; `--json` parses.
- missing path arg → exit 2 (picocli usage).

Implement `AnalyzeCommand`, `MetricsCommand` as `Callable<Integer>` `@Command`s: resolve path,
build arg map, `context.registry().routeToolCall("cobol.analyze"|"cobol.metrics", args)`, delegate
to `OutputWriter` with a small human renderer. Register both in `RenovatioCli.subcommands`.

## Task 4 — WorkspaceStateStore

TDD: `WorkspaceStateStoreTest`
- `savePlan` then `loadPlan` round-trips all fields.
- `loadPlan(unknownId)` → `Optional.empty()`.
- two workspaces with the same id string do not collide (separate `.renovatio/state` dirs).
- descriptor files land under `<workspace>/.renovatio/state/plans|runs`.

Implement `WorkspaceStateStore` + `PlanDescriptor` / `RunDescriptor` records, jackson-serialized.

## Task 5 — `plan`

TDD: `PlanCommandTest`
- `plan <p1>` exit 0; stdout shows `planId: <uuid>`; a descriptor file exists for that id.
- `--json` output contains the CLI `planId` and the core plan summary.
- `--strategy full --framework spring-boot` persisted in the descriptor.

Implement `PlanCommand`: mint `planId = UUID`, call `cobol.plan` (args: `workspacePath`, `scope`,
`nql` if given, `strategy`, `framework`), persist `PlanDescriptor`, render both ids.

## Task 6 — `apply` with replay

TDD: `ApplyCommandTest`
- After `plan`, a **fresh** `CommandLine` invocation `apply <planId>` exits 0; stdout shows
  `runId:`; dry-run true by default; run descriptor persisted.
- `apply <planId> --no-dry-run --out <tmp>` reports written files under `<tmp>`.
- `apply <unknownId>` → exit 1, stderr `unknown plan id`.

Implement `ApplyCommand`: load `PlanDescriptor` (exit 1 if absent), replay `cobol.plan` to get the
live internal id from the result, `cobol.apply` with `planId=<internal>`, `dryRun`, `outputDir`;
mint CLI `runId`, persist `RunDescriptor`, render.

## Task 7 — `diff` with replay

TDD: `DiffCommandTest`
- After `plan` + `apply`, fresh `diff <runId>` exits 0; unified diff text rendered.
- `--format semantic` renders the semantic summary; `--format both` renders both.
- `diff <unknownId>` → exit 1.

Implement `DiffCommand`: load `RunDescriptor`, replay `plan` + `apply(dryRun=true)` to repopulate
in-memory run, capture internal runId, `cobol.diff`, render by `--format`.

## Task 8 — `review`

TDD: `ReviewCommandTest` with checked-in fixtures under
`renovatio-cli/src/test/resources/reports/`
- `sample-manual-action-items.json` (mixed severities, ≥1 field needing redaction).
- ordering: error → warning → info; fields shown; masked value masked.
- `--severity warning` drops info items.
- `empty-report.json` → exit 0, `no manual action items`.
- missing file → exit 1.

Implement `ManualActionItemReport` (jackson binding to `manual-action-item.v1`), a comparator
(`severity` rank, then `failedGate`, then order), `ReviewCommand` rendering the checklist with
`SensitiveValueRedactor.redact(...)` on each field. `--json` emits the ordered filtered array.

## Task 9 — `report`

TDD: `ReportCommandTest`
- `report --html <tmp.html> --report <sample>` creates a non-empty HTML file, exit 0.
- `report --pdf <tmp.pdf> --report <sample>` creates a non-empty PDF file, exit 0.
- neither / both flags → exit 2.

Implement `ReportCommand` delegating to the existing HTML/PDF renderer beans (same ones behind
`/reports/html|pdf`); locate the bean in `RenovatioCliContext`. If those renderers are not
reachable without web context, fall back to rendering the `review` checklist to HTML via a small
template and mark PDF via the existing PDFBox helper. Confirm the reachable bean during Task 1
exploration and record the choice in the test.

## Task 10 — `serve`

TDD: `ServeCommandTest`
- `serve --stdio` and `serve --http` dispatch to a seam (`McpLauncher` interface) — test injects a
  fake and asserts the right target + passthrough args.
- default (no flag) = stdio.

Implement `ServeCommand` with a `McpLauncher` seam whose production impl calls
`McpStdioServerApplication.main` / `McpServerApplication.main`.

## Task 11 — module README + `.gitignore` guidance

`renovatio-cli/README.md`: command table, examples against `specs/1-cobol-python-migration/
examples/p1`, explanation of `<workspace>/.renovatio/state/`. Add `.renovatio/` to root
`.gitignore`.

## Task 12 — full verification + test report

1. `mvn clean install` from repo root — capture totals.
2. Offline sanity: `mvn -o -pl renovatio-cli -am test`.
3. Manual smoke (documented in report): build the CLI jar, run `analyze/plan/apply/diff/review`
   against p1, confirm ids chain across separate processes.
4. Start `McpServerApplication` and `McpStdioServerApplication`, confirm tool catalog unchanged
   (diff against a pre-change capture).
5. Write `docs/reports/renovatio-cli-test-report-20260831.md`; register as `test-report` artifact;
   add `unit-tests` evidence.

## Sequencing / commits

One commit per task, message `feat(cli): <task>` (Task 0/2/4/10 may be `chore`/`test` as fits),
all under branch `agora/issue-130-renovatio-cli`. Criterion stage marks:

| After task(s) | Criterion → `implemented` + `verified` |
| --- | --- |
| 3 | (partial) |
| 3, 5–7 | `cli-subcommands`, `chained-ids` |
| 2, 3 | `human-readable-output` |
| 8 | `review-checklist` |
| 9, 10 | `cli-subcommands` (complete) |
| 12 | `no-regression`, `tested` |

Then `agora work transition ... --to verifying`, attach evidence, `--to completed` after approval.

## Risks

- **`report` renderer reachability** — the HTML/PDF services may be `@Component`s in
  `renovatio-mcp-server` needing web context. Mitigation in Task 9; worst case `report` renders
  from the CLI's own template and the spec's `report` scope narrows to "checklist to HTML/PDF".
- **Spring context startup cost per CLI call** (~2–4 s). Acceptable for phase 1; phase 2 API keeps
  a warm context. Not optimised here.
- **picocli + Spring**: not using `picocli-spring-boot-starter`; picocli owns the CLI, Spring is
  just a bean container fetched lazily. Keeps startup and tests simple.
