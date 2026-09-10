# Reference fixtures

The governed reference set is the three repository fixtures under `renovatio-provider-cobol/src/test/resources/fixtures`:

- `batch-simple`
- `cics-mvc`
- `db2-access`

Each fixture is discovered from COBOL/copybook inputs and `expected/manifest.json`. The production pipeline now generates and compares the complete Java artifact set declared by each manifest, while source and generated-tree snapshots provide deterministic inputs and outputs. Fixture decisions are loaded from `decisions.json` when present.
