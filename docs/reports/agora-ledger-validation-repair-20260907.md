# Agora ledger validation repair — 2026-09-07

Issue: GitHub #188, `chore(agora): restore global ledger validation`.

## Scope

This repair is limited to governance records and evidence metadata. No runtime implementation, API contract, workbench UI, migration engine, or product behavior was changed.

## Repairs

- Preserved immutable evidence provenance by restoring historical artifact digests where available and adding versioned archive copies for report artifacts bound to earlier verification runs, including F1 and F2 report versions.
- Archived stale completed-work clarification advisory files under `docs/reports/agora-advisory-archive/` instead of rebinding their `last-run-input-sha256` values to current inputs.
- Restored stale completed-work consistency reports to their historical bytes and reclassified them as `historical-consistency-report` artifacts, so they remain traceable without being presented as current advisory output.
- Migrated seven historical session context paths from a machine-local absolute checkout path to repo-relative `CONTEXT.md` paths. Because the current Agora validator only accepts `context-sha256` with an absolute canonical path, the validator-bound `context-sha256` remains `null`; each record now also carries `portable-context-sha256` with the actual `CONTEXT.md` digest so consumers can verify the preserved launch context without reintroducing a workstation-specific path.
- Repaired the F4 persistence evidence register by replacing a comma-bearing free-form `text:` artifact reference with a repository artifact, `docs/reports/f4-persistence-strategy-test-report.md`.
- Repointed historical `java-21-migration` project activity entries to `docs/reports/java-21-migration-events-archive.md`, an explicit archive of the recovered ledger entries for the missing swarm event files.

## Verification

Command:

```bash
agora validate
```

Result:

```json
{
  "ok": true,
  "issues": []
}
```

The passing validation inspected 222 Agora documents, 36 swarms, 45 work records, 52 work revisions, 199 evidence entries, 0 canonical clarification registers, 82 event files, and the project activity ledger.
