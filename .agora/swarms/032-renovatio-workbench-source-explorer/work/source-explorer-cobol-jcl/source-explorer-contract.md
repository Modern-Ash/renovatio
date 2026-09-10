# Source Explorer adapter contract

`GET /api/projects/{projectId}/workbench/source-explorer` returns a read-only
structural projection of the project's legacy assets:

```json
{
  "files": [
    {
      "id": "src/PAYROLL.CBL",
      "name": "PAYROLL.CBL",
      "kind": "cobol-program",
      "path": "src/PAYROLL.CBL",
      "hash": "sha256:...",
      "encoding": "US-ASCII",
      "analysisStatus": "parsed",
      "symbols": [
        {
          "id": "PAYROLL.CBL#para:0100-MAIN",
          "kind": "paragraph",
          "name": "0100-MAIN",
          "line": 42,
          "column": 8,
          "parentId": "PAYROLL.CBL#section:MAIN-SECTION",
          "irCoordinate": "ir://PAYROLL/paragraph/0100-MAIN"
        }
      ],
      "diagnostics": [
        { "severity": "warning", "message": "COPY MISSINGCPY not found in workspace", "line": 12 }
      ]
    }
  ],
  "datasets": [{ "id": "NIGHTLY.BATCH.PAYROLL", "name": "NIGHTLY.BATCH.PAYROLL", "referencedBy": ["NIGHTLY-BATCH.JCL"] }]
}
```

Symbol `kind` is one of `division`, `section`, `paragraph`, `perform`, `call`,
`exec-sql`, `exec-cics`, `copy`, `jcl-step`, `jcl-dd`. `analysisStatus` is
`parsed`, `partial` or `unsupported`. `diagnostics[].severity` is `info`,
`warning` or `error`.

The endpoint is governed by the existing Workbench view authorization boundary
(`X-Role`, or `renovatio.workbench.dev-no-auth-enabled` in local development).
It walks only files inside the project's normalized workspace root, never writes,
and never triggers analysis. An `unsupported` file still appears with an empty
symbol list so the workspace stays navigable. An empty `files` array means the
project has no legacy assets, not that analysis succeeded.
