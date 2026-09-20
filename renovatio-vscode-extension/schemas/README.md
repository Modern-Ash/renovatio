# Renovatio VS Code Schema Contracts

The VS Code extension contributes JSON Schemas for local-first Renovatio workbench artifacts. These schemas are part of epic #281 and are exercised by the fixture tests added for #292.

## Schemas

- `workspace-manifest.schema.json` validates `.renovatio/workspace.renovatio.json`.
- `migration-map.renovatio.schema.json` validates `.renovatio/migration-map.renovatio.json`.
- `domain-model.schema.json` validates `*.renovatio-domain.json`.
- `architecture.schema.json` validates `*.renovatio-arch.json`.
- `migration-map.schema.json` is the legacy migration-map schema retained for compatibility.

## Update Rules

- Keep all artifact paths workspace-relative.
- Add required fields only when the extension can create or repair them.
- Update `renovatio-vscode-extension/examples/` and `test/fixtures/workspace-basic/` with every schema change.
- Run `npm test` in `renovatio-vscode-extension` after schema edits; the schema fixture test compiles the contributed schemas with AJV.
- Keep schema validation local-first: malformed artifacts should surface diagnostics and repair commands, not silent backend writes.

## Minimal Workspace Manifest

```json
{
  "version": "1",
  "projectId": "carddemo",
  "source": {
    "language": "cobol",
    "roots": ["src/mainframe"]
  },
  "targets": [
    {
      "language": "java",
      "root": "generated/java"
    }
  ],
  "backend": {
    "url": "http://127.0.0.1:8081",
    "environment": "local"
  },
  "llm": {
    "provider": "openai",
    "model": "gpt-4.1",
    "promptProfile": "renovatio-cobol-reverse-engineering-v1"
  }
}
```

## Minimal Migration Map

```json
{
  "version": "1",
  "projectId": "carddemo",
  "generatedAt": "2026-09-19T20:00:00Z",
  "entries": [
    {
      "id": "program:CARDDEMO",
      "kind": "program",
      "source": {
        "language": "cobol",
        "path": "src/mainframe/CARDDEMO.cbl"
      },
      "target": {
        "language": "java",
        "path": "generated/java/src/main/java/com/example/CardDemoService.java"
      },
      "status": "needs-review",
      "evidence": [".renovatio/evidence/evaluator-summary.md"]
    }
  ]
}
```
