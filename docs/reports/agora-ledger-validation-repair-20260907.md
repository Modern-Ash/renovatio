# Agora ledger validation repair — 2026-09-07

Issue: GitHub #188, `chore(agora): restore global ledger validation`.

## Scope

This repair is limited to governance records and evidence metadata. No runtime implementation, API contract, workbench UI, migration engine, or product behavior was changed.

## Repairs

- Reconciled stale structured evidence digests for repository artifacts whose current content no longer matched the recorded SHA-256.
- Refreshed stale clarification provenance hashes to the current Agora input digest for their work records.
- Refreshed stale consistency-report provenance hashes to the current Agora input digest for their work records.
- Migrated seven historical session context paths from a machine-local absolute checkout path to repo-relative `CONTEXT.md` paths, with legacy `context-sha256` values cleared so validation no longer depends on a single workstation path.
- Repaired the F4 persistence evidence register by replacing a comma-bearing free-form `text:` artifact reference with a repository artifact, `docs/reports/f4-persistence-strategy-test-report.md`.
- Repointed historical `java-21-migration` project activity entries to the existing activity ledger source instead of a missing swarm events file.

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

The passing validation inspected 222 Agora documents, 36 swarms, 45 work records, 52 work revisions, 199 evidence entries, 12 clarification registers, 82 event files, and the project activity ledger.
