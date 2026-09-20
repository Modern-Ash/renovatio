# Renovatio Evaluator Evidence Summary

Status: Ready

This demo bundle shows the minimum path Renovatio wants an evaluator to see:

- COBOL source and copybook are present under `src/mainframe` and `copybooks`.
- Domain and architecture artifacts are available under `.renovatio/diagrams`.
- Migration traceability is available in `.renovatio/migration-map.renovatio.json`.
- Generated Java is staged under `generated/java`.

Review sequence:

1. Open the workspace manifest and confirm backend plus LLM model identity.
2. Run backend and LLM checks. Offline backends should be shown as Offline, not as broken local editing.
3. Open the native domain diagram.
4. Open the migration map and inspect the legacy-to-target link.
5. Export the evidence bundle for handoff.
