# renovatio-cli

Command-line adapter over the in-process Renovatio core. Ships as a single executable JAR
whose `renovatio` command exposes COBOL migration capabilities as ordinary subcommands.

## Commands

```
renovatio analyze <path> [--dialect IBM|GNU|MF] [--scope '**/*.cbl'] [--json]
renovatio metrics <path> [--scope '**/*.cbl'] [--json]
renovatio plan    <path> [--scope ...] [--strategy incremental|full] [--framework spring-boot] [--json]
renovatio apply   <planId> [--dry-run | --no-dry-run] [--out <dir>] [--json]
renovatio diff    <runId> [--format unified|semantic|both] [--json]
renovatio review  [--report <path>] [--severity error|warning|info] [--json]
renovatio report  [--html <file> | --pdf <file>]
renovatio serve   [--http | --stdio] [-- <passthrough args>]
```

## Cross-invocation chaining

Plans and runs are persisted under `<workspace>/.renovatio/state/` as JSON descriptors.
This allows chaining across separate CLI invocations:

```bash
renovatio plan specs/1-cobol-python-migration/examples/p1
# prints: planId: <uuid>

renovatio apply <planId> --dry-run
# prints: runId: <uuid>

renovatio diff <runId>
# renders the diff
```

## `.renovatio/` state directory

Each workspace stores CLI-minted identifiers under `.renovatio/state/`:

```
.renovatio/state/
  plans/<planId>.json   # plan descriptor
  runs/<runId>.json     # run descriptor
```

Add `.renovatio/` to your `.gitignore` to avoid committing local state.

## Build

```bash
mvn -q -pl renovatio-cli -am install
java -jar target/renovatio.jar --help
```

## Testing

```bash
mvn -q -pl renovatio-cli test
```
