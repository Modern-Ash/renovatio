# CardDemo pipeline coverage (issue #217)

`carddemo-coverage.md` / `carddemo-coverage.json` are **generated** — do not hand-edit.

## What it measures

For every COBOL program in the vendored AWS CardDemo corpus
(`renovatio-provider-cobol/src/test/resources/corpus/carddemo/`), it runs the Renovatio pipeline
and records: parse OK, Java emission OK, and whether the generated Java compiles with `javac`,
plus manual-action-item counts, `// TODO` / `// Unhandled` markers, subsystem classification, and
a lexical scan of procedural verbs not yet translated.

It is **measurement only** — it never changes the translator and never fails the build on low
coverage. The generator (`PopulateCobolProcessRecipe`, `JavaGenerationService` templates) is
untouched.

## How to regenerate

```bash
mvn -pl renovatio-provider-cobol test \
    -Dgroups=coverage -Drenovatio.surefire.excludedGroups= -Dexec.skip=true
```

The test carries the JUnit tag `coverage` and is excluded from the default build
(`renovatio.surefire.excludedGroups=coverage` in the module POM). Commit the regenerated
`carddemo-coverage.{md,json}` when the numbers move.

## CI

Run it on a nightly job (not on every PR — it is ~10 s and does 44 in-memory `javac` runs).
Suggested step: the command above, then fail the job only if `git diff --stat` shows the report
changed without a matching commit (drift guard), never on the coverage numbers themselves.

## Corpus provenance

See `renovatio-provider-cobol/src/test/resources/corpus/carddemo/PROVENANCE.md`.
