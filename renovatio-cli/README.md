# renovatio-cli

Command-line adapter over the in-process Renovatio core. Ships as a single executable JAR
whose `renovatio` command exposes COBOL migration capabilities as ordinary subcommands.

## Commands

```
renovatio analyze <path> [--dialect IBM|GNU|MF] [--scope '**/*.cbl'] [--json]
renovatio metrics <path> [--scope '**/*.cbl'] [--json]
renovatio generate <path> --profile <profile.json|yaml> [--out <dir>] [--json]
renovatio plan    <path> [--scope ...] [--strategy incremental|full] [--framework spring-boot] [--json]
renovatio apply   <planId> [--dry-run | --no-dry-run] [--out <dir>] [--json]
renovatio diff    <runId> [--format unified|semantic|both] [--json]
renovatio review  [--report <path>] [--severity error|warning|info] [--json]
renovatio report  [--html <file> | --pdf <file>]
renovatio profile init [--project <path>] [--force] [--json]
renovatio profile save|apply|diff|list ...
renovatio decisions list --project <path>
renovatio decisions set <decision-key-or-id> <option> --project <path>
renovatio policy export|apply|list ...
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

Each workspace stores CLI-minted identifiers and reusable decision inputs under `.renovatio/`:

```
.renovatio/migration-profile.json   # sparse local overlay used by generation
.renovatio/decisions.json           # decisions reconciled by `analyze`
.renovatio/profile-template.json    # explicitly bound template version
.renovatio/policy-catalog.json      # explicitly bound policy version
.renovatio/state/
  plans/<planId>.json   # plan descriptor
  runs/<runId>.json     # run descriptor
```

A policy catalog can be created without editing project state by hand:

```bash
renovatio analyze <project>
renovatio decisions list --project <project>
renovatio decisions set java.accessor-convention JAVA_BEANS --project <project>
renovatio policy export bank --version 1 --project <project>
```

Initialize a sparse profile, edit it, and generate Java or Node artifacts explicitly:

```bash
renovatio profile init --project <project>
renovatio generate <project> --profile <project>/.renovatio/migration-profile.json --out generated
```

Applying a reusable profile template stores only its immutable binding. The project profile stays
a sparse local overlay, so rebinding to a newer or different template updates inherited values.

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
